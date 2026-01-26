package com.analiticasoft.hitraider.gameplay;

import com.analiticasoft.hitraider.assets.PlayerStateMapper;
import com.analiticasoft.hitraider.assets.PlayerSprites;
import com.analiticasoft.hitraider.combat.Projectile;
import com.analiticasoft.hitraider.config.CombatTuning;
import com.analiticasoft.hitraider.config.PlayerTuning;
import com.analiticasoft.hitraider.config.ShakeTuning;
import com.analiticasoft.hitraider.entities.MeleeEnemy;
import com.analiticasoft.hitraider.entities.RangedEnemy;
import com.analiticasoft.hitraider.input.Action;
import com.analiticasoft.hitraider.input.InputState;
import com.analiticasoft.hitraider.physics.PhysicsConstants;
import com.analiticasoft.hitraider.world.RoomInstance;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;

/**
 * GameplayRuntime: owns the fixed pipeline.
 * No rendering here. Only state updates and physics orchestration.
 */
public class GameplayRuntime {

    public void init(GameplayContext ctx) {
        // Inject destroy queue into run systems
        ctx.run.setDestroyQueue(ctx.destroyQueue);
        if (ctx.run.projectiles != null) ctx.run.projectiles.setDestroyQueue(ctx.destroyQueue);

        // Init visuals
        ctx.playerVisualState = PlayerStateMapper.map(ctx.run.player);
        ctx.playerStateTime = 0f;

        syncEnemyTimers(ctx);

        // FrameStats tuning
        ctx.frameStats.setSpikeThresholdMs(33f);
        ctx.frameStats.setWindowSeconds(5f);
    }

    public void requestRestart(GameplayContext ctx) {
        ctx.restartRequested = true;
    }

    public void requestReload(GameplayContext ctx) {
        ctx.reloadRequested = true;
    }

    public void toggleStrict(GameplayContext ctx) {
        ctx.strictModeOn = !ctx.strictModeOn;
    }

    public void toggleStrictFreeze(GameplayContext ctx) {
        ctx.strictFreezeOnFail = !ctx.strictFreezeOnFail;
    }

    public void unfreeze(GameplayContext ctx) {
        ctx.frozenByStrict = false;
        ctx.lastStrictError = null;
    }

    public void tick(GameplayContext ctx, InputState input, float delta) {
        // reload
        if (ctx.reloadRequested) {
            ctx.reloadRequested = false;
            ctx.sprites.reload();
            // background rebuild is done in screen (render side) or caller
            return;
        }

        // restart
        if (ctx.restartRequested) {
            ctx.restartRequested = false;
            fullRestart(ctx);
            return;
        }

        // transition
        boolean finishedFadeOut = ctx.transition.update(delta);
        if (finishedFadeOut) {
            onRoomTransition(ctx);
            return;
        }

        // hitstop/freeze
        float dt = delta;
        if (ctx.hitstopTimer > 0f) {
            ctx.hitstopTimer = Math.max(0f, ctx.hitstopTimer - delta);
            dt = 0f;
        }
        if (ctx.frozenByStrict) dt = 0f;

        // pre-update timers
        ctx.shake.update(dt);
        if (ctx.run.shootCooldown > 0f) ctx.run.shootCooldown = Math.max(0f, ctx.run.shootCooldown - dt);

        // begin frame
        ctx.run.combat.beginFrame();

        // update entities
        updatePlayer(ctx, input, dt);
        updateEnemies(ctx, dt);

        // pre-physics
        ctx.run.combat.update(dt);

        // physics
        ctx.run.physics.step(dt);

        // post-physics
        ctx.run.projectiles.flushImpacts();
        ctx.destroyQueue.flush(ctx.run.physics.world, ctx.run.combat);
        ctx.run.projectiles.update(dt);
        ctx.run.processPickupsChoiceAware();

        // post-physics events
        postPhysicsEvents(ctx, dt);

        // camera
        ctx.camera.follow(ctx.worldCamera, ctx.run.player);
        ctx.shake.apply(ctx.worldCamera);
        ctx.worldCamera.update();

        // visual timers
        updateVisualTimers(ctx, dt);

        // invariants
        if (ctx.strictModeOn) validateInvariants(ctx);
    }

    private void fullRestart(GameplayContext ctx) {
        ctx.shake.reset();
        ctx.hitstopTimer = 0f;
        ctx.meleeHitCounter = 0;

        resetDoor(ctx);

        ctx.run.setDestroyQueue(ctx.destroyQueue);
        ctx.run.startNewRun(true);
        ctx.run.projectiles.setDestroyQueue(ctx.destroyQueue);

        ctx.transition.startFadeIn();
        spawnDoorForCurrentRoom(ctx);
        syncEnemyTimers(ctx);

        ctx.playerVisualState = PlayerStateMapper.map(ctx.run.player);
        ctx.playerStateTime = 0f;
    }

    private void onRoomTransition(GameplayContext ctx) {
        resetDoor(ctx);

        if (ctx.run.run.hasNext()) {
            ctx.run.run.next();
            ctx.run.loadCurrentRoom(false);
        } else {
            ctx.run.startNewRun(false);
        }

        ctx.run.setDestroyQueue(ctx.destroyQueue);
        ctx.run.projectiles.setDestroyQueue(ctx.destroyQueue);

        ctx.transition.startFadeIn();
        spawnDoorForCurrentRoom(ctx);
        syncEnemyTimers(ctx);

        ctx.playerVisualState = PlayerStateMapper.map(ctx.run.player);
        ctx.playerStateTime = 0f;
    }

    private void updatePlayer(GameplayContext ctx, InputState input, float dt) {
        if (!ctx.run.player.isAlive()) {
            var v = ctx.run.player.body.getLinearVelocity();
            ctx.run.player.body.setLinearVelocity(0f, v.y);
            return;
        }

        ctx.run.player.update(dt, input);

        if (ctx.run.player.shouldSpawnAttackHitboxThisFrame()) {
            ctx.run.combat.spawnMeleeHitbox(
                ctx.run.player.body,
                ctx.run.player,
                ctx.run.player.getFaction(),
                ctx.run.player.getFacingDir(),
                ctx.run.player.getAimY(input),
                PlayerTuning.BASE_MELEE_DAMAGE
            );
        }

        if (input.isJustPressed(Action.SHOOT) && ctx.run.shootCooldown <= 0f) {
            int dmg = PlayerTuning.BASE_PROJECTILE_DAMAGE + ctx.run.relics.getBonusProjectileDamage();

            float sx = ctx.run.player.getXpx() + ctx.run.player.getFacingDir() * PlayerTuning.PROJECTILE_SPAWN_OFFSET_X;
            float sy = ctx.run.player.getYpx() + PlayerTuning.PROJECTILE_SPAWN_OFFSET_Y;

            Projectile p = new Projectile(
                ctx.run.physics.world,
                ctx.run.player.getFaction(),
                dmg,
                sx, sy,
                ctx.run.player.getFacingDir() * PlayerTuning.PROJECTILE_SPEED,
                0f,
                PlayerTuning.PROJECTILE_LIFETIME
            );

            p.piercesLeft = ctx.run.relics.getPiercingShots();
            ctx.run.projectiles.spawn(p);

            ctx.run.shootCooldown = PlayerTuning.SHOOT_BASE_COOLDOWN * ctx.run.relics.getFireRateMultiplier();
        }
    }

    private void updateEnemies(GameplayContext ctx, float dt) {
        if (ctx.run.inChoiceRoom) return;

        for (int i = ctx.run.meleeEnemies.size - 1; i >= 0; i--) {
            MeleeEnemy e = ctx.run.meleeEnemies.get(i);
            e.update(dt, ctx.run.player);

            if (e.didStartAttackThisFrame()) {
                ctx.run.combat.spawnMeleeHitbox(e.body, e, e.getFaction(), e.getFacingDir(), 0, 1);
            }

            if (!e.isAlive()) {
                ctx.run.combat.purgeForBody(e.body);
                ctx.destroyQueue.queueBody(e.body);

                ctx.run.meleeEnemies.removeIndex(i);
                ctx.run.meleeAnims.removeIndex(i);
                if (i < ctx.meleeAnimTimes.size) ctx.meleeAnimTimes.removeIndex(i);

                ctx.run.onEnemyKilledDrop(e, i);
            }
        }

        for (int i = ctx.run.rangedEnemies.size - 1; i >= 0; i--) {
            RangedEnemy re = ctx.run.rangedEnemies.get(i);
            re.update(dt, ctx.run.player);

            if (re.didShootThisFrame()) {
                float sx = re.getXpx() + re.getFacingDir() * 14f;
                float sy = re.getYpx() + 10f;

                Projectile p = new Projectile(
                    ctx.run.physics.world, re.getFaction(), 1,
                    sx, sy,
                    re.getFacingDir() * 7.5f, 0f,
                    1.4f
                );
                ctx.run.projectiles.spawn(p);
            }

            if (!re.isAlive()) {
                ctx.destroyQueue.queueBody(re.body);

                ctx.run.rangedEnemies.removeIndex(i);
                ctx.run.rangedAnims.removeIndex(i);
                if (i < ctx.rangedAnimTimes.size) ctx.rangedAnimTimes.removeIndex(i);
            }
        }
    }

    private void postPhysicsEvents(GameplayContext ctx, float dt) {
        if (ctx.run.combat.consumePlayerHurt()) ctx.shake.start(ShakeTuning.PLAYER_HURT_DUR, ShakeTuning.PLAYER_HURT_INT);

        if (ctx.run.combat.consumeEnemyHurt()) {
            ctx.shake.start(ShakeTuning.ENEMY_HURT_DUR, ShakeTuning.ENEMY_HURT_INT);

            int N = ctx.run.relics.getLifestealEveryHits();
            if (N > 0 && ctx.run.player.isAlive()) {
                ctx.meleeHitCounter++;
                if (ctx.meleeHitCounter % N == 0) ctx.run.player.getHealth().heal(1);
            }
        }

        if (ctx.run.combat.consumeMeleeWorldHit()) ctx.shake.start(ShakeTuning.MELEE_WORLD_DUR, ShakeTuning.MELEE_WORLD_INT);

        int pe = ctx.run.projectiles.consumeImpactsEnemy();
        if (pe > 0) {
            ctx.shake.start(ShakeTuning.PROJ_HIT_ENEMY_DUR, ShakeTuning.PROJ_HIT_ENEMY_INT);
            ctx.hitstopTimer = Math.max(ctx.hitstopTimer, CombatTuning.HITSTOP_PROJECTILE);
        }

        int pw = ctx.run.projectiles.consumeImpactsWorld();
        if (pw > 0) ctx.shake.start(ShakeTuning.PROJ_HIT_WORLD_DUR, ShakeTuning.PROJ_HIT_WORLD_INT);

        int alive = ctx.run.meleeEnemies.size + ctx.run.rangedEnemies.size;
        ctx.run.encounter.update(dt, alive);

        if (ctx.run.canExit()) openDoor(ctx);

        RoomInstance room = ctx.run.run.current();
        if (!ctx.transition.isTransitioning() && ctx.run.canExit()) {
            if (ctx.run.player.getXpx() > room.template.exitXpx + 20f) {
                ctx.transition.startFadeOut();
            }
        }
    }

    private void updateVisualTimers(GameplayContext ctx, float dt) {
        PlayerSprites.State newState = PlayerStateMapper.map(ctx.run.player);
        if (newState != ctx.playerVisualState) {
            ctx.playerVisualState = newState;
            ctx.playerStateTime = 0f;
        } else {
            ctx.playerStateTime += dt;
        }

        for (int i = 0; i < ctx.meleeAnimTimes.size; i++) ctx.meleeAnimTimes.set(i, ctx.meleeAnimTimes.get(i) + dt);
        for (int i = 0; i < ctx.rangedAnimTimes.size; i++) ctx.rangedAnimTimes.set(i, ctx.rangedAnimTimes.get(i) + dt);
    }

    public void syncEnemyTimers(GameplayContext ctx) {
        ctx.meleeAnimTimes.clear();
        for (int i = 0; i < ctx.run.meleeEnemies.size; i++) ctx.meleeAnimTimes.add(0f);

        ctx.rangedAnimTimes.clear();
        for (int i = 0; i < ctx.run.rangedEnemies.size; i++) ctx.rangedAnimTimes.add(0f);
    }

    private void validateInvariants(GameplayContext ctx) {
        if (ctx.run.meleeEnemies.size != ctx.meleeAnimTimes.size) {
            onStrictFail(ctx, "Desync melee timers: enemies=" + ctx.run.meleeEnemies.size + " timers=" + ctx.meleeAnimTimes.size);
            syncEnemyTimers(ctx);
        }
        if (ctx.run.rangedEnemies.size != ctx.rangedAnimTimes.size) {
            onStrictFail(ctx, "Desync ranged timers: enemies=" + ctx.run.rangedEnemies.size + " timers=" + ctx.rangedAnimTimes.size);
            syncEnemyTimers(ctx);
        }
        if (ctx.doorBody != null && ctx.run.physics != null && ctx.doorBody.getWorld() != ctx.run.physics.world) {
            onStrictFail(ctx, "Door world mismatch");
            resetDoor(ctx);
        }
        if (ctx.run.inChoiceRoom && (ctx.run.meleeEnemies.size + ctx.run.rangedEnemies.size) > 0) {
            onStrictFail(ctx, "Choice room has enemies alive");
        }
    }

    private void onStrictFail(GameplayContext ctx, String msg) {
        ctx.lastStrictError = msg;
        Gdx.app.error("STRICT", msg);
        if (ctx.strictModeOn && ctx.strictFreezeOnFail) ctx.frozenByStrict = true;
    }

    // ----- Door helpers moved here (simple & consistent) -----

    public void spawnDoorForCurrentRoom(GameplayContext ctx) {
        closeDoorAt(ctx, ctx.run.run.current().template.exitXpx, 120f);
    }

    public void closeDoorAt(GameplayContext ctx, float xPx, float yPx) {
        if (ctx.doorBody != null) return;

        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.StaticBody;
        bd.position.set(PhysicsConstants.toMeters(xPx), PhysicsConstants.toMeters(yPx));
        ctx.doorBody = ctx.run.physics.world.createBody(bd);

        PolygonShape s = new PolygonShape();
        s.setAsBox(PhysicsConstants.toMeters(28f / 2f), PhysicsConstants.toMeters(220f / 2f));

        FixtureDef fd = new FixtureDef();
        fd.shape = s;
        Fixture fx = ctx.doorBody.createFixture(fd);
        fx.setUserData("ground");
        s.dispose();

        ctx.doorClosed = true;
    }

    public void openDoor(GameplayContext ctx) {
        if (ctx.doorBody == null) return;
        ctx.run.combat.purgeForBody(ctx.doorBody);
        ctx.destroyQueue.queueBody(ctx.doorBody);
        ctx.doorBody = null;
        ctx.doorClosed = false;
    }

    public void resetDoor(GameplayContext ctx) {
        if (ctx.doorBody != null) {
            try {
                if (ctx.run.physics != null && ctx.doorBody.getWorld() == ctx.run.physics.world) {
                    ctx.run.combat.purgeForBody(ctx.doorBody);
                    ctx.destroyQueue.queueBody(ctx.doorBody);
                }
            } catch (Exception ignored) {
            } finally {
                ctx.doorBody = null;
                ctx.doorClosed = false;
            }
        } else {
            ctx.doorClosed = false;
        }
    }
}
