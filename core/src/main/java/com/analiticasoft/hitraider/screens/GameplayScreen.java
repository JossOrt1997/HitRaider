package com.analiticasoft.hitraider.screens;

import com.analiticasoft.hitraider.combat.*;
import com.analiticasoft.hitraider.entities.MeleeEnemy;
import com.analiticasoft.hitraider.entities.Player;
import com.analiticasoft.hitraider.entities.RangedEnemy;
import com.analiticasoft.hitraider.input.Action;
import com.analiticasoft.hitraider.input.DesktopInputProvider;
import com.analiticasoft.hitraider.input.InputState;
import com.analiticasoft.hitraider.physics.GameContactListener;
import com.analiticasoft.hitraider.physics.PhysicsConstants;
import com.analiticasoft.hitraider.physics.PhysicsWorld;
import com.analiticasoft.hitraider.render.*;
import com.analiticasoft.hitraider.relics.*;
import com.analiticasoft.hitraider.ui.HudPainter;
import com.analiticasoft.hitraider.world.EncounterManager;
import com.analiticasoft.hitraider.world.LevelFactory;
import com.analiticasoft.hitraider.world.RoomManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.Random;

public class GameplayScreen implements Screen {

    private static final float VIRTUAL_W = 640f;
    private static final float VIRTUAL_H = 360f;

    private OrthographicCamera worldCamera;
    private Viewport worldViewport;
    private OrthographicCamera uiCamera;

    private ShapeRenderer shapes;
    private SpriteBatch batch;
    private BitmapFont font;

    private final HudPainter hud = new HudPainter();

    // Debug toggles
    private boolean debugOverlay = true;
    private boolean debugHitboxes = true;
    private boolean debugHurtboxes = true;

    // Input
    private final InputState input = new InputState();
    private final DesktopInputProvider inputProvider = new DesktopInputProvider();

    // Systems
    private PhysicsWorld physics;
    private CombatSystem combat;
    private ProjectileSystem projectiles;
    private GameContactListener contactListener;

    // Level
    private Array<LevelFactory.PlatformRect> platformRects;

    // Entities
    private Player player;
    private final Array<MeleeEnemy> meleeEnemies = new Array<>();
    private final Array<RangedEnemy> rangedEnemies = new Array<>();

    // Encounter + Rooms
    private final EncounterManager encounter = new EncounterManager();
    private final RoomManager rooms = new RoomManager();

    // Door
    private Body doorBody;
    private boolean doorClosed = false;
    private static final float DOOR_W = 28f;
    private static final float DOOR_H = 220f;

    // Visual (frames)
    private final CharacterRenderer charRenderer = new DebugCharacterRenderer();
    private final DebugPhysicsRenderer debugPhysics = new DebugPhysicsRenderer();
    private CharacterAnimator playerAnim;
    private final Array<CharacterAnimator> meleeAnims = new Array<>();
    private final Array<CharacterAnimator> rangedAnims = new Array<>();

    // Visual snapshots (sprites-ready)
    private final VisualStateSnapshot snapPlayer = new VisualStateSnapshot();
    private final VisualStateSnapshot snapEnemy = new VisualStateSnapshot();

    // Relics/pickups
    private final RelicManager relics = new RelicManager();
    private final Array<RelicPickup> pickups = new Array<>();
    private final Random rng = new Random();

    // Polish: hitstop + camera shake + fades
    private float hitstopTimer = 0f;

    private float shakeTimer = 0f;
    private float shakeIntensity = 0f;
    private float shakeSeed = 0f;

    private float fade = 0f; // 0=visible, 1=black overlay
    private boolean transitioning = false;
    private float transitionTimer = 0f; // 0..duration
    private boolean transitionToNextRoom = false;

    private float clearTextTimer = 0f;
    private float diedTextTimer = 0f;

    @Override
    public void show() {
        worldCamera = new OrthographicCamera();
        worldViewport = new FitViewport(VIRTUAL_W, VIRTUAL_H, worldCamera);
        worldViewport.apply(true);

        uiCamera = new OrthographicCamera();
        uiCamera.setToOrtho(false, VIRTUAL_W, VIRTUAL_H);
        uiCamera.update();

        shapes = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.setColor(0.05f, 0.05f, 0.06f, 1f);

        playerAnim = new CharacterAnimator();
        DebugAnimLibrary.definePlayer(playerAnim);

        buildRooms();
        initRoom(true);

        worldCamera.position.set(VIRTUAL_W / 2f, VIRTUAL_H / 2f, 0f);
        worldCamera.update();
    }

    private void buildRooms() {
        rooms.clear();

        rooms.add(new RoomManager.RoomDef(120f, 140f, 1120f, 2, 1)
            .addSpawn(520f, 220f).addSpawn(650f, 220f).addSpawn(820f, 220f).addSpawn(950f, 220f));

        rooms.add(new RoomManager.RoomDef(140f, 140f, 1120f, 3, 1)
            .addSpawn(520f, 220f).addSpawn(650f, 220f).addSpawn(820f, 220f).addSpawn(980f, 220f).addSpawn(1040f, 220f));

        rooms.add(new RoomManager.RoomDef(160f, 140f, 1120f, 2, 2)
            .addSpawn(520f, 220f).addSpawn(650f, 220f).addSpawn(820f, 220f).addSpawn(980f, 220f).addSpawn(1040f, 220f));
    }

    private void initRoom(boolean rebuildPhysics) {
        if (rebuildPhysics) {
            if (physics != null) physics.dispose();
            physics = new PhysicsWorld(new Vector2(0f, -25f));
            combat = new CombatSystem(physics.world);
            projectiles = new ProjectileSystem(physics.world);

            contactListener = new GameContactListener(combat, projectiles);
            physics.world.setContactListener(contactListener);

            platformRects = LevelFactory.createTestLevel(physics.world);
            player = new Player(physics.world, 120f, 140f);
        }

        meleeEnemies.clear();
        rangedEnemies.clear();
        meleeAnims.clear();
        rangedAnims.clear();
        pickups.clear();

        doorBody = null;
        doorClosed = false;

        RoomManager.RoomDef r = rooms.current();
        if (r == null) return;

        // place player
        player.body.setTransform(PhysicsConstants.toMeters(r.entryXpx), PhysicsConstants.toMeters(r.entryYpx), 0f);
        player.body.setLinearVelocity(0f, 0f);

        // spawn
        int si = 0;
        for (int i = 0; i < r.meleeCount; i++) {
            Vector2 sp = r.spawns.get(si++ % r.spawns.size);
            meleeEnemies.add(new MeleeEnemy(physics.world, sp.x, sp.y));
            CharacterAnimator a = new CharacterAnimator();
            DebugAnimLibrary.defineMeleeEnemy(a);
            meleeAnims.add(a);
        }
        for (int i = 0; i < r.rangedCount; i++) {
            Vector2 sp = r.spawns.get(si++ % r.spawns.size);
            rangedEnemies.add(new RangedEnemy(physics.world, sp.x, sp.y));
            CharacterAnimator a = new CharacterAnimator();
            DebugAnimLibrary.defineMeleeEnemy(a); // placeholder
            rangedAnims.add(a);
        }

        encounter.reset();
        hitstopTimer = 0f;

        clearTextTimer = 0f;
        diedTextTimer = 0f;

        closeDoorAt(r.exitXpx, 120f);

        // fade in
        fade = 1f;
        transitioning = true;
        transitionTimer = 0f;
        transitionToNextRoom = false;
    }

    private void closeDoorAt(float xPx, float yPx) {
        if (doorBody != null) return;

        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.StaticBody;
        bd.position.set(PhysicsConstants.toMeters(xPx), PhysicsConstants.toMeters(yPx));
        doorBody = physics.world.createBody(bd);

        PolygonShape s = new PolygonShape();
        s.setAsBox(PhysicsConstants.toMeters(DOOR_W / 2f), PhysicsConstants.toMeters(DOOR_H / 2f));

        FixtureDef fd = new FixtureDef();
        fd.shape = s;
        Fixture fx = doorBody.createFixture(fd);
        fx.setUserData("ground");
        s.dispose();

        doorClosed = true;
    }

    private void openDoor() {
        if (doorBody == null) return;
        combat.purgeForBody(doorBody);
        physics.world.destroyBody(doorBody);
        doorBody = null;
        doorClosed = false;
    }

    @Override
    public void render(float delta) {
        inputProvider.poll(input);

        // toggles
        if (Gdx.input.isKeyJustPressed(Input.Keys.F1) || Gdx.input.isKeyJustPressed(Input.Keys.TAB)) debugOverlay = !debugOverlay;
        if (Gdx.input.isKeyJustPressed(Input.Keys.H)) debugHitboxes = !debugHitboxes;
        if (Gdx.input.isKeyJustPressed(Input.Keys.U)) debugHurtboxes = !debugHurtboxes;


        // restart run
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            rooms.reset();
            initRoom(false);
        }

        // hitstop
        float dt = delta;
        if (hitstopTimer > 0f) {
            hitstopTimer = Math.max(0f, hitstopTimer - delta);
            dt = 0f;
        }

        update(dt);

        // clear
        Gdx.gl.glClearColor(0.92f, 0.93f, 0.95f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        worldViewport.apply();

        renderWorld();
        if (debugOverlay) renderUI();

        input.endFrame();
    }

    private void update(float delta) {
        // Transition fade controller (0.25s)
        if (transitioning) {
            transitionTimer += delta;
            float t = Math.min(1f, transitionTimer / 0.25f);

            if (!transitionToNextRoom) {
                // fade in
                fade = 1f - t;
                if (t >= 1f) transitioning = false;
            } else {
                // fade out to black, then switch room, then fade in
                fade = t;
                if (t >= 1f) {
                    // switch room now (safe)
                    if (rooms.hasNext()) rooms.next();
                    else rooms.reset();
                    initRoom(false); // initRoom starts with fade-in
                    return;
                }
            }
        }

        // camera shake timers
        if (shakeTimer > 0f) {
            shakeTimer = Math.max(0f, shakeTimer - delta);
            shakeSeed += 31.7f * delta;
        }

        // update texts timers
        if (clearTextTimer > 0f) clearTextTimer = Math.max(0f, clearTextTimer - delta);
        if (diedTextTimer > 0f) diedTextTimer = Math.max(0f, diedTextTimer - delta);

        combat.beginFrame();

        // If dead: block input and show message
        if (!player.isAlive()) {
            diedTextTimer = Math.max(diedTextTimer, 999f); // persistent until restart
            // physics still steps to settle, but no player input needed
            Vector2 v = player.body.getLinearVelocity();
            player.body.setLinearVelocity(0f, v.y);

        } else {
            player.update(delta, input);

            // melee hitbox
            if (player.shouldSpawnAttackHitboxThisFrame()) {
                combat.spawnMeleeHitbox(player.body, player, player.getFaction(), player.getFacingDir(), player.getAimY(input), 1);
            }

            // shoot (K)
            if (input.isJustPressed(Action.SHOOT)) {
                int dmg = 1 + relics.getBonusProjectileDamage();
                float sx = player.getXpx() + player.getFacingDir() * 14f;
                float sy = player.getYpx() + 10f;
                projectiles.spawn(new Projectile(physics.world, player.getFaction(), dmg, sx, sy, player.getFacingDir() * 8.5f, 0f, 1.2f));
            }
        }

        // melee enemies
        for (int i = meleeEnemies.size - 1; i >= 0; i--) {
            MeleeEnemy e = meleeEnemies.get(i);
            e.update(delta, player);

            meleeAnims.get(i).set(VisualMapper.enemyKey(e));
            meleeAnims.get(i).update(delta);

            if (e.didStartAttackThisFrame()) {
                combat.spawnMeleeHitbox(e.body, e, e.getFaction(), e.getFacingDir(), 0, 1);
            }

            if (!e.isAlive()) {
                combat.purgeForBody(e.body);
                physics.world.destroyBody(e.body);
                meleeEnemies.removeIndex(i);
                meleeAnims.removeIndex(i);

                if (rng.nextFloat() < 0.25f) {
                    pickups.add(new RelicPickup(physics.world, RelicType.BONUS_PROJECTILE_DAMAGE, e.getXpx(), e.getYpx()));
                }
            }
        }

        // ranged enemies
        for (int i = rangedEnemies.size - 1; i >= 0; i--) {
            RangedEnemy re = rangedEnemies.get(i);
            re.update(delta, player);

            rangedAnims.get(i).set(AnimKey.ENEMY_CHASE); // placeholder
            rangedAnims.get(i).update(delta);

            if (re.didShootThisFrame()) {
                float sx = re.getXpx() + re.getFacingDir() * 14f;
                float sy = re.getYpx() + 10f;
                projectiles.spawn(new Projectile(physics.world, re.getFaction(), 1, sx, sy, re.getFacingDir() * 7.5f, 0f, 1.4f));
            }

            if (!re.isAlive()) {
                physics.world.destroyBody(re.body);
                rangedEnemies.removeIndex(i);
                rangedAnims.removeIndex(i);
            }
        }

        combat.update(delta);

        // step physics
        physics.step(delta);

        // ✅ flush projectile impacts after step
        projectiles.flushImpacts();



        // apply light shake and micro-hitstop on projectile impacts
        /*int impacts = projectiles.getImpactsFlushedThisFrame();
        if (impacts > 0) {
            // little shake
            startShake(0.08f, 2.0f);
            // optional tiny hitstop
            hitstopTimer = Math.max(hitstopTimer, 0.02f);
        }*/

        if (combat.consumePlayerHurt()) {
            startShake(0.10f, 4.5f);
        }

    // (B) Melee: si tú pegaste a un enemigo (medio)
        if (combat.consumeEnemyHurt()) {
            startShake(0.08f, 2.5f);
        }

    // (C) Melee: pegaste el mundo (pared/suelo) => como pegar enemigo (medio)
        if (combat.consumeMeleeWorldHit()) {
            startShake(0.08f, 2.5f);
        }

    // (D) Proyectiles: pega enemigo (medio-bajo)
        int pe = projectiles.consumeImpactsEnemy();
        if (pe > 0) {
            startShake(0.06f, 1.8f);
            // micro hitstop opcional:
            hitstopTimer = Math.max(hitstopTimer, 0.02f);
        }

    // (E) Proyectiles: pega mundo (leve)
        int pw = projectiles.consumeImpactsWorld();
        if (pw > 0) {
            startShake(0.05f, 0.9f);
        }



        // update projectiles (timers + fx)
        projectiles.update(delta);

        // pickups collection
        for (int i = pickups.size - 1; i >= 0; i--) {
            RelicPickup p = pickups.get(i);
            if (p.collected) {
                relics.add(p.type);
                physics.world.destroyBody(p.body);
                pickups.removeIndex(i);
            }
        }

        // melee hitstop + stronger shake
        if (combat.hitThisFrame()) {
            hitstopTimer = Math.max(hitstopTimer, 0.05f);
            startShake(0.10f, 4.0f);
        }

        // encounter + door
        int alive = meleeEnemies.size + rangedEnemies.size;
        encounter.update(delta, alive);

        if (encounter.getState() == EncounterManager.State.CLEAR) {
            openDoor();
            clearTextTimer = Math.max(clearTextTimer, 1.2f);
        }

        // room transition trigger
        RoomManager.RoomDef r = rooms.current();
        if (r != null && encounter.getState() == EncounterManager.State.CLEAR && !transitioning) {
            if (player.getXpx() > r.exitXpx + 20f) {
                transitioning = true;
                transitionToNextRoom = true;
                transitionTimer = 0f;
            }
        }

        // camera follow + shake
        float targetX = player.getXpx();
        float targetY = player.getYpx() + 40f;

        worldCamera.position.x += (targetX - worldCamera.position.x) * 0.12f;
        worldCamera.position.y += (targetY - worldCamera.position.y) * 0.10f;

        applyShakeToCamera();
        worldCamera.update();

        // anim player
        playerAnim.set(VisualMapper.playerKey(player));
        playerAnim.update(delta);
    }

    private void startShake(float duration, float intensity) {
        shakeTimer = Math.max(shakeTimer, duration);
        shakeIntensity = Math.max(shakeIntensity, intensity);
    }

    private void applyShakeToCamera() {
        if (shakeTimer <= 0f) return;
        float t = shakeTimer;
        // pseudo random deterministic
        float sx = (float)Math.sin(shakeSeed * 17.0) * shakeIntensity;
        float sy = (float)Math.cos(shakeSeed * 23.0) * shakeIntensity;
        worldCamera.position.x += sx;
        worldCamera.position.y += sy;
    }

    private void renderWorld() {
        shapes.setProjectionMatrix(worldCamera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        // platforms
        if (platformRects != null) {
            for (LevelFactory.PlatformRect p : platformRects) {
                if ("oneway".equals(p.type)) shapes.setColor(0.35f, 0.35f, 0.38f, 1f);
                else shapes.setColor(0.20f, 0.20f, 0.22f, 1f);
                shapes.rect(p.cx - p.w / 2f, p.cy - p.h / 2f, p.w, p.h);
            }
        }

        // door
        if (doorClosed && doorBody != null) {
            float dx = PhysicsConstants.toPixels(doorBody.getPosition().x);
            float dy = PhysicsConstants.toPixels(doorBody.getPosition().y);
            shapes.setColor(0.08f, 0.08f, 0.09f, 1f);
            shapes.rect(dx - DOOR_W / 2f, dy - DOOR_H / 2f, DOOR_W, DOOR_H);
        }

        // pickups
        shapes.setColor(0.10f, 0.45f, 0.10f, 1f);
        for (RelicPickup p : pickups) shapes.circle(p.getXpx(), p.getYpx(), 6f, 16);

        // projectiles: colors by faction + impact fx
        for (Projectile pr : projectiles.projectiles) {
            if (pr.state == Projectile.State.ALIVE) {
                if (pr.faction == Faction.PLAYER) shapes.setColor(0.05f, 0.05f, 0.05f, 1f);
                else shapes.setColor(0.10f, 0.10f, 0.25f, 1f);
                shapes.rect(pr.lastXpx - 3f, pr.lastYpx - 3f, 6f, 6f);
            } else {
                float a = pr.impactFxLeft / 0.10f;
                float r = (pr.faction == Faction.PLAYER ? 8f : 6f) + (1f - a) * 10f;
                if (pr.faction == Faction.PLAYER) shapes.setColor(0.05f, 0.05f, 0.05f, 1f);
                else shapes.setColor(0.10f, 0.10f, 0.25f, 1f);
                shapes.circle(pr.lastXpx, pr.lastYpx, r, 18);
            }
        }

        // melee enemies
        for (int i = 0; i < meleeEnemies.size; i++) {
            MeleeEnemy e = meleeEnemies.get(i);
            float ex = e.getXpx();
            float ey = e.getYpx() - 14f;

            // telegraph highlight
            if (e.isFlashing()) shapes.setColor(0.05f, 0.05f, 0.05f, 1f);
            else if (e.getState() == MeleeEnemy.State.TELEGRAPH) {
                float al = e.getTelegraphAlpha();
                float base = 0.14f;
                float glow = 0.40f * al;
                float c = base + glow;
                shapes.setColor(c, c, c, 1f);
            } else shapes.setColor(0.12f, 0.12f, 0.16f, 1f);

            charRenderer.draw(shapes, ex, ey, e.getFacingDir(), meleeAnims.get(i).getFrame());
        }

        // ranged enemies (telegraph highlight)
        for (int i = 0; i < rangedEnemies.size; i++) {
            RangedEnemy e = rangedEnemies.get(i);
            float ex = e.getXpx();
            float ey = e.getYpx() - 14f;

            if (e.isFlashing()) shapes.setColor(0.05f, 0.05f, 0.05f, 1f);
            else if (e.getState() == RangedEnemy.State.TELEGRAPH) {
                float al = e.getTelegraphAlpha();
                float base = 0.16f;
                float glow = 0.35f * al;
                float c = base + glow;
                shapes.setColor(0.10f, c, 0.30f, 1f);
            } else shapes.setColor(0.10f, 0.10f, 0.22f, 1f);

            // placeholder rect
            shapes.rect(ex - 10f, ey, 20f, 28f);
        }

        // player (frame)
        float px = player.getXpx();
        float py = player.getYpx() - 16f;
        if (player.isFlashing()) shapes.setColor(0.02f, 0.02f, 0.02f, 1f);
        else shapes.setColor(0.10f, 0.10f, 0.12f, 1f);

        charRenderer.draw(shapes, px, py, player.getFacingDir(), playerAnim.getFrame());

        shapes.end();

        // debug hit/hurt
        if (debugHitboxes || debugHurtboxes) {
            shapes.begin(ShapeRenderer.ShapeType.Line);

            if (debugHurtboxes) {
                shapes.setColor(0.2f, 0.4f, 0.9f, 1f);
                for (Fixture fx : player.body.getFixtureList()) {
                    if ("player_ground_sensor".equals(fx.getUserData())) continue;
                    debugPhysics.drawFixtureOutline(shapes, fx);
                }
                for (MeleeEnemy e : meleeEnemies) {
                    for (Fixture fx : e.body.getFixtureList()) debugPhysics.drawFixtureOutline(shapes, fx);
                }
                for (RangedEnemy e : rangedEnemies) {
                    for (Fixture fx : e.body.getFixtureList()) debugPhysics.drawFixtureOutline(shapes, fx);
                }
            }

            if (debugHitboxes) {
                shapes.setColor(0.9f, 0.2f, 0.2f, 1f);
                for (Fixture fx : combat.getActiveHitboxFixtures()) debugPhysics.drawFixtureOutline(shapes, fx);
            }

            shapes.end();
        }
    }

    private void renderUI() {
        // ---- UI SHAPES (HP BAR + FADE) ----
        shapes.setProjectionMatrix(uiCamera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        // HP BAR (limpia)
        float barX = 10f;
        float barY = VIRTUAL_H - 26f;
        float barW = 200f;
        float barH = 10f;

        int hp = player.getHealth().getHp();
        int maxHp = player.getHealth().getMaxHp();
        float pct = (maxHp <= 0) ? 0f : (hp / (float) maxHp);
        pct = Math.max(0f, Math.min(1f, pct));

        // fondo
        shapes.setColor(0.10f, 0.10f, 0.12f, 1f);
        shapes.rect(barX, barY, barW, barH);

        // relleno
        shapes.setColor(0.20f, 0.75f, 0.25f, 1f);
        shapes.rect(barX, barY, barW * pct, barH);

        // borde
        shapes.setColor(0.02f, 0.02f, 0.02f, 1f);
        shapes.rect(barX, barY, barW, 1f);
        shapes.rect(barX, barY + barH - 1f, barW, 1f);
        shapes.rect(barX, barY, 1f, barH);
        shapes.rect(barX + barW - 1f, barY, 1f, barH);

        // Fade overlay (transition)
        if (fade > 0f) {
            shapes.setColor(0f, 0f, 0f, fade);
            shapes.rect(0, 0, VIRTUAL_W, VIRTUAL_H);
        }

        shapes.end();

        // ---- UI TEXT ----
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();

        // Línea superior (separada)
        font.draw(batch, "Room: " + rooms.index(), 10, VIRTUAL_H - 10);
        font.draw(batch, "Enemies: " + (meleeEnemies.size + rangedEnemies.size), 230, VIRTUAL_H - 10);

        // Debajo de la barra
        font.draw(batch, "HP: " + hp + "/" + maxHp, 10, VIRTUAL_H - 40);

        // Relics: los bajamos para que no se encimen
        font.draw(batch, "Relics: " + relics.getOwned().size, 10, VIRTUAL_H - 58);
        font.draw(batch, "+ProjDmg: +" + relics.getBonusProjectileDamage(), 120, VIRTUAL_H - 58);

        // Iconos placeholder
        float rx = 230f;
        float ry = VIRTUAL_H - 58f;
        for (int i = 0; i < relics.getOwned().size; i++) {
            font.draw(batch, "[■]", rx + i * 18f, ry);
        }

        // Controles abajo
        font.draw(batch, "K shoot | J melee | R restart | F1/TAB overlay | H/U debug", 10, 20);

        // Mensajes
        if (clearTextTimer > 0f) {
            float a = Math.min(1f, clearTextTimer / 0.30f);
            hud.drawFadingText(batch, font, "CLEAR! -> go right", 250, 200, a);
        }

        if (!player.isAlive()) {
            hud.drawFadingText(batch, font, "YOU DIED - Press R", 240, 200, 1f);
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
        if (physics != null) physics.dispose();
    }
}
