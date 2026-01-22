package com.analiticasoft.hitraider.screens;

import com.analiticasoft.hitraider.combat.Projectile;
import com.analiticasoft.hitraider.combat.Faction;
import com.analiticasoft.hitraider.relics.RelicType;
import com.analiticasoft.hitraider.config.*;
import com.analiticasoft.hitraider.controllers.*;
import com.analiticasoft.hitraider.entities.MeleeEnemy;
import com.analiticasoft.hitraider.entities.RangedEnemy;
import com.analiticasoft.hitraider.input.Action;
import com.analiticasoft.hitraider.input.DesktopInputProvider;
import com.analiticasoft.hitraider.input.InputState;
import com.analiticasoft.hitraider.physics.PhysicsConstants;
import com.analiticasoft.hitraider.relics.RelicPickup;
import com.analiticasoft.hitraider.render.*;
import com.analiticasoft.hitraider.ui.HudPainter;
import com.analiticasoft.hitraider.ui.HudRenderer;
import com.analiticasoft.hitraider.world.RoomInstance;
import com.analiticasoft.hitraider.world.RoomType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class GameplayScreen implements Screen {

    // Cameras
    private OrthographicCamera worldCamera;
    private Viewport worldViewport;
    private OrthographicCamera uiCamera;

    // Render
    private ShapeRenderer shapes;
    private SpriteBatch batch;
    private BitmapFont font;

    private final HudPainter hudPainter = new HudPainter();
    private final HudRenderer hudRenderer = new HudRenderer();

    // Debug flags
    private boolean debugOverlay = true;
    private boolean debugHitboxes = true;
    private boolean debugHurtboxes = true;

    // Input
    private final InputState input = new InputState();
    private final DesktopInputProvider inputProvider = new DesktopInputProvider();

    // Controllers
    private final RunController run = new RunController();
    private final CameraController cameraController = new CameraController();
    private final ShakeController shake = new ShakeController();
    private final TransitionController transition = new TransitionController();

    // Visuals
    private final CharacterRenderer charRenderer = new DebugCharacterRenderer();
    private final DebugPhysicsRenderer debugPhysics = new DebugPhysicsRenderer();
    private CharacterAnimator playerAnim;

    // Door state (kept in screen to keep Box2D body lifecycle simple)
    private com.badlogic.gdx.physics.box2d.Body doorBody;
    private boolean doorClosed = false;

    // Feel
    private float hitstopTimer = 0f;

    // Lifesteal hit counter
    private int meleeHitCounter = 0;

    private boolean restartRequested = false;


    @Override
    public void show() {
        worldCamera = new OrthographicCamera();
        worldViewport = new FitViewport(GameConfig.VIRTUAL_W, GameConfig.VIRTUAL_H, worldCamera);
        worldViewport.apply(true);

        uiCamera = new OrthographicCamera();
        uiCamera.setToOrtho(false, GameConfig.VIRTUAL_W, GameConfig.VIRTUAL_H);
        uiCamera.update();

        shapes = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.setColor(0.05f, 0.05f, 0.06f, 1f);

        playerAnim = new CharacterAnimator();
        DebugAnimLibrary.definePlayer(playerAnim);

        run.buildTemplates();
        run.startNewRun(true);

        transition.startFadeIn();

        worldCamera.position.set(GameConfig.VIRTUAL_W / 2f, GameConfig.VIRTUAL_H / 2f, 0f);
        worldCamera.update();

        // door for current room
        spawnDoorForCurrentRoom();
    }

    @Override
    public void render(float delta) {
        inputProvider.poll(input);

        // toggles
        if (Gdx.input.isKeyJustPressed(Input.Keys.F1) || Gdx.input.isKeyJustPressed(Input.Keys.TAB)) debugOverlay = !debugOverlay;
        if (Gdx.input.isKeyJustPressed(Input.Keys.H)) debugHitboxes = !debugHitboxes;
        if (Gdx.input.isKeyJustPressed(Input.Keys.U)) debugHurtboxes = !debugHurtboxes;

        // restart run (FULL reset: rebuild physics/world/player to avoid ghost colliders & dead state)
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            restartRequested = true;  // ✅ solo pedir restart
        }
        float dt = delta;
        if (hitstopTimer > 0f) {
            hitstopTimer = Math.max(0f, hitstopTimer - delta);
            dt = 0f;
        }

        update(dt);

        Gdx.gl.glClearColor(0.92f, 0.93f, 0.95f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        worldViewport.apply();

        renderWorld();
        if (debugOverlay) renderUI();

        input.endFrame();
    }

    private void update(float delta) {

        if (restartRequested) {
            restartRequested = false;

            // Limpia efectos/counters de gameplay
            shake.reset();
            hitstopTimer = 0f;
            meleeHitCounter = 0;

            // IMPORTANT: destruye puerta vieja de forma segura
            resetDoor();

            // Reinicia el run reconstruyendo física
            run.startNewRun(true);

            // Reinicia transición / puerta
            transition.startFadeIn();
            spawnDoorForCurrentRoom();

            return; // ✅ cortamos el frame aquí (no step, no flush, nada)
        }

        // Transition update
        boolean finishedFadeOut = transition.update(delta);
        if (finishedFadeOut) {
            // advance room (or new run) and fade in
            resetDoor();

            if (run.run.hasNext()) {
                run.run.next();
                run.loadCurrentRoom(false);
            } else {
                run.startNewRun(false);
            }

            transition.startFadeIn();
            spawnDoorForCurrentRoom();
            return;
        }

        // shake timers
        shake.update(delta);

        // shoot cooldown
        if (run.shootCooldown > 0f) run.shootCooldown = Math.max(0f, run.shootCooldown - delta);

        // Begin frame combat
        run.combat.beginFrame();

        // Player update (block when dead)
        if (!run.player.isAlive()) {
            var v = run.player.body.getLinearVelocity();
            run.player.body.setLinearVelocity(0f, v.y);
        } else {
            run.player.update(delta, input);

            // melee
            if (run.player.shouldSpawnAttackHitboxThisFrame()) {
                run.combat.spawnMeleeHitbox(run.player.body, run.player, run.player.getFaction(), run.player.getFacingDir(), run.player.getAimY(input), PlayerTuning.BASE_MELEE_DAMAGE);
            }

            // shoot (K)
            if (input.isJustPressed(Action.SHOOT) && run.shootCooldown <= 0f) {
                int dmg = PlayerTuning.BASE_PROJECTILE_DAMAGE + run.relics.getBonusProjectileDamage();

                float sx = run.player.getXpx() + run.player.getFacingDir() * PlayerTuning.PROJECTILE_SPAWN_OFFSET_X;
                float sy = run.player.getYpx() + PlayerTuning.PROJECTILE_SPAWN_OFFSET_Y;

                Projectile p = new Projectile(run.physics.world, run.player.getFaction(), dmg, sx, sy,
                    run.player.getFacingDir() * PlayerTuning.PROJECTILE_SPEED, 0f, PlayerTuning.PROJECTILE_LIFETIME);

                p.piercesLeft = run.relics.getPiercingShots();
                run.projectiles.spawn(p);

                run.shootCooldown = PlayerTuning.SHOOT_BASE_COOLDOWN * run.relics.getFireRateMultiplier();
            }
        }

        // Enemies (skip in choice room)
        if (!run.inChoiceRoom) {
            for (int i = run.meleeEnemies.size - 1; i >= 0; i--) {
                MeleeEnemy e = run.meleeEnemies.get(i);
                e.update(delta, run.player);

                run.meleeAnims.get(i).set(VisualMapper.enemyKey(e));
                run.meleeAnims.get(i).update(delta);

                if (e.didStartAttackThisFrame()) {
                    run.combat.spawnMeleeHitbox(e.body, e, e.getFaction(), e.getFacingDir(), 0, 1);
                }

                if (!e.isAlive()) {
                    run.combat.purgeForBody(e.body);
                    run.physics.world.destroyBody(e.body);
                    run.meleeEnemies.removeIndex(i);
                    run.meleeAnims.removeIndex(i);
                    run.onEnemyKilledDrop(e, i);
                }
            }

            for (int i = run.rangedEnemies.size - 1; i >= 0; i--) {
                RangedEnemy re = run.rangedEnemies.get(i);
                re.update(delta, run.player);

                run.rangedAnims.get(i).set(AnimKey.ENEMY_CHASE);
                run.rangedAnims.get(i).update(delta);

                if (re.didShootThisFrame()) {
                    float sx = re.getXpx() + re.getFacingDir() * 14f;
                    float sy = re.getYpx() + 10f;
                    Projectile p = new Projectile(run.physics.world, re.getFaction(), 1, sx, sy,
                        re.getFacingDir() * 7.5f, 0f, 1.4f);
                    run.projectiles.spawn(p);
                }

                if (!re.isAlive()) {
                    run.physics.world.destroyBody(re.body);
                    run.rangedEnemies.removeIndex(i);
                    run.rangedAnims.removeIndex(i);
                }
            }
        }

        // hitboxes lifetime
        run.combat.update(delta);

        // physics
        run.physics.step(delta);

        // projectile impacts after step
        run.projectiles.flushImpacts();

        // events -> shake/hitstop
        if (run.combat.consumePlayerHurt()) shake.start(ShakeTuning.PLAYER_HURT_DUR, ShakeTuning.PLAYER_HURT_INT);
        if (run.combat.consumeEnemyHurt()) {
            shake.start(ShakeTuning.ENEMY_HURT_DUR, ShakeTuning.ENEMY_HURT_INT);

            int N = run.relics.getLifestealEveryHits();
            if (N > 0 && run.player.isAlive()) {
                meleeHitCounter++;
                if (meleeHitCounter % N == 0) run.player.getHealth().heal(1);
            }
        }
        if (run.combat.consumeMeleeWorldHit()) shake.start(ShakeTuning.MELEE_WORLD_DUR, ShakeTuning.MELEE_WORLD_INT);

        int pe = run.projectiles.consumeImpactsEnemy();
        if (pe > 0) {
            shake.start(ShakeTuning.PROJ_HIT_ENEMY_DUR, ShakeTuning.PROJ_HIT_ENEMY_INT);
            hitstopTimer = Math.max(hitstopTimer, CombatTuning.HITSTOP_PROJECTILE);
        }

        int pw = run.projectiles.consumeImpactsWorld();
        if (pw > 0) shake.start(ShakeTuning.PROJ_HIT_WORLD_DUR, ShakeTuning.PROJ_HIT_WORLD_INT);

        run.projectiles.update(delta);

        // pickups
        run.processPickupsChoiceAware();

        // encounter
        int alive = run.meleeEnemies.size + run.rangedEnemies.size;
        run.encounter.update(delta, alive);

        // door: open when canExit
        if (run.canExit()) openDoor();

        // exit trigger: start fade-out
        RoomInstance room = run.run.current();
        if (!transition.isTransitioning() && run.canExit()) {
            if (run.player.getXpx() > room.template.exitXpx + 20f) {
                transition.startFadeOut();
            }
        }

        // camera follow
        cameraController.follow(worldCamera, run.player);
        shake.apply(worldCamera);
        worldCamera.update();

        // anim player
        playerAnim.set(VisualMapper.playerKey(run.player));
        playerAnim.update(delta);
    }

    private void spawnDoorForCurrentRoom() {
        // door starts closed
        closeDoorAt(run.run.current().template.exitXpx, 120f);
    }

    private void closeDoorAt(float xPx, float yPx) {
        if (doorBody != null) return;

        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.StaticBody;
        bd.position.set(PhysicsConstants.toMeters(xPx), PhysicsConstants.toMeters(yPx));
        doorBody = run.physics.world.createBody(bd);

        PolygonShape s = new PolygonShape();
        s.setAsBox(PhysicsConstants.toMeters(28f / 2f), PhysicsConstants.toMeters(220f / 2f));

        FixtureDef fd = new FixtureDef();
        fd.shape = s;
        Fixture fx = doorBody.createFixture(fd);
        fx.setUserData("ground");
        s.dispose();

        doorClosed = true;
    }

    private void openDoor() {
        if (doorBody == null) return;
        run.combat.purgeForBody(doorBody);
        run.physics.world.destroyBody(doorBody);
        doorBody = null;
        doorClosed = false;
    }

    private void resetDoor() {
        if (doorBody != null) {
            try {
                // destroy only if it still belongs to the current world
                if (run.physics != null && doorBody.getWorld() == run.physics.world) {
                    run.combat.purgeForBody(doorBody);
                    run.physics.world.destroyBody(doorBody);
                }
            } catch (Exception ignored) {
            } finally {
                doorBody = null;
                doorClosed = false;
            }
        } else {
            doorClosed = false;
        }
    }

    private void renderWorld() {
        shapes.setProjectionMatrix(worldCamera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        // platforms
        if (run.platformRects != null) {
            for (var p : run.platformRects) {
                if ("oneway".equals(p.type)) shapes.setColor(0.35f, 0.35f, 0.38f, 1f);
                else shapes.setColor(0.20f, 0.20f, 0.22f, 1f);
                shapes.rect(p.cx - p.w/2f, p.cy - p.h/2f, p.w, p.h);
            }
        }

        // door
        if (doorClosed && doorBody != null) {
            float dx = PhysicsConstants.toPixels(doorBody.getPosition().x);
            float dy = PhysicsConstants.toPixels(doorBody.getPosition().y);
            shapes.setColor(0.08f, 0.08f, 0.09f, 1f);
            shapes.rect(dx - 14f, dy - 110f, 28f, 220f);
        }

        // pickups
        shapes.setColor(0.10f, 0.45f, 0.10f, 1f);
        for (RelicPickup p : run.pickups) shapes.circle(p.getXpx(), p.getYpx(), 6f, 16);

        // projectiles
        for (var pr : run.projectiles.projectiles) {
            if (pr.state == Projectile.State.ALIVE) {
                if (pr.faction == Faction.PLAYER) shapes.setColor(0.05f, 0.05f, 0.05f, 1f);
                else shapes.setColor(0.10f, 0.10f, 0.25f, 1f);
                shapes.rect(pr.lastXpx - 3f, pr.lastYpx - 3f, 6f, 6f);
            } else {
                float a = pr.impactFxLeft / 0.10f;
                float rr = (pr.faction == Faction.PLAYER ? 8f : 6f) + (1f - a) * 10f;
                if (pr.faction == Faction.PLAYER) shapes.setColor(0.05f, 0.05f, 0.05f, 1f);
                else shapes.setColor(0.10f, 0.10f, 0.25f, 1f);
                shapes.circle(pr.lastXpx, pr.lastYpx, rr, 18);
            }
        }

        // enemies
        for (int i = 0; i < run.meleeEnemies.size; i++) {
            MeleeEnemy e = run.meleeEnemies.get(i);
            float ex = e.getXpx();
            float ey = e.getYpx() - 14f;
            shapes.setColor(0.12f, 0.12f, 0.16f, 1f);
            charRenderer.draw(shapes, ex, ey, e.getFacingDir(), run.meleeAnims.get(i).getFrame());
        }
        for (int i = 0; i < run.rangedEnemies.size; i++) {
            RangedEnemy e = run.rangedEnemies.get(i);
            float ex = e.getXpx();
            float ey = e.getYpx() - 14f;
            shapes.setColor(0.10f, 0.10f, 0.22f, 1f);
            shapes.rect(ex - 10f, ey, 20f, 28f);
        }

        // player
        float px = run.player.getXpx();
        float py = run.player.getYpx() - 16f;
        shapes.setColor(run.player.isFlashing() ? 0.02f : 0.10f, run.player.isFlashing() ? 0.02f : 0.10f, run.player.isFlashing() ? 0.02f : 0.12f, 1f);
        charRenderer.draw(shapes, px, py, run.player.getFacingDir(), playerAnim.getFrame());

        shapes.end();

        if (debugHitboxes || debugHurtboxes) {
            shapes.begin(ShapeRenderer.ShapeType.Line);

            if (debugHurtboxes) {
                shapes.setColor(0.2f, 0.4f, 0.9f, 1f);
                for (Fixture fx : run.player.body.getFixtureList()) {
                    if ("player_ground_sensor".equals(fx.getUserData())) continue;
                    debugPhysics.drawFixtureOutline(shapes, fx);
                }
                for (MeleeEnemy e : run.meleeEnemies) {
                    for (Fixture fx : e.body.getFixtureList()) debugPhysics.drawFixtureOutline(shapes, fx);
                }
                for (RangedEnemy e : run.rangedEnemies) {
                    for (Fixture fx : e.body.getFixtureList()) debugPhysics.drawFixtureOutline(shapes, fx);
                }
            }

            if (debugHitboxes) {
                shapes.setColor(0.9f, 0.2f, 0.2f, 1f);
                for (Fixture fx : run.combat.getActiveHitboxFixtures()) debugPhysics.drawFixtureOutline(shapes, fx);
            }

            shapes.end();
        }
    }

    private void renderUI() {
        // hp bar + fade overlay
        shapes.setProjectionMatrix(uiCamera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        float barX = 10f, barY = GameConfig.VIRTUAL_H - 26f, barW = 200f, barH = 10f;
        int hp = run.player.getHealth().getHp();
        int maxHp = run.player.getHealth().getMaxHp();
        float pct = (maxHp <= 0) ? 0f : (hp / (float) maxHp);
        pct = Math.max(0f, Math.min(1f, pct));

        shapes.setColor(0.10f, 0.10f, 0.12f, 1f);
        shapes.rect(barX, barY, barW, barH);

        shapes.setColor(0.20f, 0.75f, 0.25f, 1f);
        shapes.rect(barX, barY, barW * pct, barH);

        if (transition.fade > 0f) {
            shapes.setColor(0f, 0f, 0f, transition.fade);
            shapes.rect(0, 0, GameConfig.VIRTUAL_W, GameConfig.VIRTUAL_H);
        }

        shapes.end();

        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();

        RoomInstance room = run.run.current();

        hudRenderer.drawHeader(batch, font, run.run, room);
        hudRenderer.drawRelics(batch, font, run.relics);

        font.draw(batch, "HP: " + hp + "/" + maxHp, 10, GameConfig.VIRTUAL_H - 92);
        font.draw(batch, "Enemies: " + (run.meleeEnemies.size + run.rangedEnemies.size), 260, GameConfig.VIRTUAL_H - 92);

        font.draw(batch, "K shoot | J melee | R new run | F1/TAB overlay | H/U debug", 10, 20);

        if (room.type == RoomType.CHOICE && run.inChoiceRoom) {
            hudPainter.drawFadingText(batch, font, "CHOOSE ONE RELIC", 250, 220, 1f);
        }

        if (!run.player.isAlive()) {
            hudPainter.drawFadingText(batch, font, "YOU DIED - Press R", 240, 200, 1f);
        }

        batch.end();
    }

    @Override public void resize(int width, int height) { worldViewport.update(width, height, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        shapes.dispose();
        batch.dispose();
        font.dispose();
        if (run.physics != null) run.physics.dispose();
    }
}
