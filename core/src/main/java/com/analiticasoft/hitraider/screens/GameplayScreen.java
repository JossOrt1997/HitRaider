package com.analiticasoft.hitraider.screens;

import com.analiticasoft.hitraider.assets.*;
import com.analiticasoft.hitraider.combat.Faction;
import com.analiticasoft.hitraider.combat.Projectile;
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
import com.analiticasoft.hitraider.world.RoomInstance;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * GameplayScreen (refactor + sprite pipeline integrated)
 * - Safe assets (no crash if missing)
 * - Player/enemy sprites + fallback to debug shapes
 * - 2-layer parallax background (if textures exist)
 * - HUD: Souls-like bars + floating weapon/info, toggles F1/F2
 * - Debug toggles: H hitboxes, U hurtboxes
 * - Restart is deferred (R) to avoid Box2D native crashes
 * - Hot reload assets (F5)
 */
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

    // HUD toggles
    private boolean hudEssentialOn = true;
    private boolean hudInfoOn = true;

    // Debug toggles
    private boolean debugHitboxes = true;
    private boolean debugHurtboxes = true;

    // Input
    private final InputState input = new InputState();
    private final DesktopInputProvider inputProvider = new DesktopInputProvider();

    // Controllers / game state
    private final RunController run = new RunController();
    private final CameraController cameraController = new CameraController();
    private final ShakeController shake = new ShakeController();
    private final TransitionController transition = new TransitionController();

    // Debug visuals fallback
    private final CharacterRenderer charRenderer = new DebugCharacterRenderer();
    private final DebugPhysicsRenderer debugPhysics = new DebugPhysicsRenderer();
    private CharacterAnimator playerAnim;

    // Door
    private com.badlogic.gdx.physics.box2d.Body doorBody;
    private boolean doorClosed = false;

    // Feel
    private float hitstopTimer = 0f;
    private int meleeHitCounter = 0;

    // Restart deferred
    private boolean restartRequested = false;

    // Sprite system + background
    private final SpriteManager sprites = new SpriteManager();
    private BackgroundParallax background;

    // Visual state times
    private float playerStateTime = 0f;
    private PlayerSprites.State playerVisualState = PlayerSprites.State.IDLE;

    // Enemy animation timers (parallel to enemy arrays)
    private final Array<Float> meleeAnimTimes = new Array<>();
    private final Array<Float> rangedAnimTimes = new Array<>();

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

        // Debug anim fallback (player only)
        playerAnim = new CharacterAnimator();
        DebugAnimLibrary.definePlayer(playerAnim);

        // Load sprites safely (will not crash if missing)
        sprites.load();

        // Default background (forest) if assets exist
        background = new BackgroundParallax(
            sprites.forestBase(),
            sprites.forestMid(),
            1.0f,
            0.85f,
            GameConfig.VIRTUAL_H
        );

        // Start run
        run.buildTemplates();
        run.startNewRun(true);

        transition.startFadeIn();

        // Camera start
        worldCamera.position.set(GameConfig.VIRTUAL_W / 2f, GameConfig.VIRTUAL_H / 2f, 0f);
        worldCamera.update();

        // Door for current room
        spawnDoorForCurrentRoom();

        // Init enemy timers
        syncEnemyAnimTimers();

        // Init player visual state
        playerVisualState = PlayerStateMapper.map(run.player);
        playerStateTime = 0f;
    }

    @Override
    public void render(float delta) {
        inputProvider.poll(input);

        // HUD toggles
        if (Gdx.input.isKeyJustPressed(Input.Keys.F1) || Gdx.input.isKeyJustPressed(Input.Keys.TAB)) hudEssentialOn = !hudEssentialOn;
        if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) hudInfoOn = !hudInfoOn;

        // Debug toggles
        if (Gdx.input.isKeyJustPressed(Input.Keys.H)) debugHitboxes = !debugHitboxes;
        if (Gdx.input.isKeyJustPressed(Input.Keys.U)) debugHurtboxes = !debugHurtboxes;

        // Hot reload assets (optional)
        if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) {
            sprites.reload();
            background = new BackgroundParallax(
                sprites.forestBase(),
                sprites.forestMid(),
                1.0f,
                0.85f,
                GameConfig.VIRTUAL_H
            );
        }

        // Restart request (deferred)
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            restartRequested = true;
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
        if (hudEssentialOn || hudInfoOn) renderUI();
        renderCriticalOverlay();

        input.endFrame();
    }

    private void update(float delta) {
        // SAFE deferred restart
        if (restartRequested) {
            restartRequested = false;

            shake.reset();
            hitstopTimer = 0f;
            meleeHitCounter = 0;

            resetDoor();

            run.startNewRun(true);
            transition.startFadeIn();
            spawnDoorForCurrentRoom();

            syncEnemyAnimTimers();

            playerVisualState = PlayerStateMapper.map(run.player);
            playerStateTime = 0f;

            return;
        }

        // Transition update
        boolean finishedFadeOut = transition.update(delta);
        if (finishedFadeOut) {
            resetDoor();

            if (run.run.hasNext()) {
                run.run.next();
                run.loadCurrentRoom(false);
            } else {
                run.startNewRun(false);
            }

            transition.startFadeIn();
            spawnDoorForCurrentRoom();
            syncEnemyAnimTimers();

            playerVisualState = PlayerStateMapper.map(run.player);
            playerStateTime = 0f;

            return;
        }

        // Shake timers
        shake.update(delta);

        // Shoot cooldown
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
                run.combat.spawnMeleeHitbox(
                    run.player.body,
                    run.player,
                    run.player.getFaction(),
                    run.player.getFacingDir(),
                    run.player.getAimY(input),
                    PlayerTuning.BASE_MELEE_DAMAGE
                );
            }

            // shoot
            if (input.isJustPressed(Action.SHOOT) && run.shootCooldown <= 0f) {
                int dmg = PlayerTuning.BASE_PROJECTILE_DAMAGE + run.relics.getBonusProjectileDamage();

                float sx = run.player.getXpx() + run.player.getFacingDir() * PlayerTuning.PROJECTILE_SPAWN_OFFSET_X;
                float sy = run.player.getYpx() + PlayerTuning.PROJECTILE_SPAWN_OFFSET_Y;

                Projectile p = new Projectile(
                    run.physics.world,
                    run.player.getFaction(),
                    dmg,
                    sx, sy,
                    run.player.getFacingDir() * PlayerTuning.PROJECTILE_SPEED,
                    0f,
                    PlayerTuning.PROJECTILE_LIFETIME
                );

                p.piercesLeft = run.relics.getPiercingShots();
                run.projectiles.spawn(p);

                run.shootCooldown = PlayerTuning.SHOOT_BASE_COOLDOWN * run.relics.getFireRateMultiplier();
            }
        }

        // Enemies (skip in choice room)
        if (!run.inChoiceRoom) {
            // melee
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
                    if (i < meleeAnimTimes.size) meleeAnimTimes.removeIndex(i);
                    run.onEnemyKilledDrop(e, i);
                }
            }

            // ranged
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
                    if (i < rangedAnimTimes.size) rangedAnimTimes.removeIndex(i);
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

        // update player visual state time
        PlayerSprites.State newState = PlayerStateMapper.map(run.player);
        if (newState != playerVisualState) {
            playerVisualState = newState;
            playerStateTime = 0f;
        } else {
            playerStateTime += delta;
        }

        // enemy anim times
        for (int i = 0; i < meleeAnimTimes.size; i++) meleeAnimTimes.set(i, meleeAnimTimes.get(i) + delta);
        for (int i = 0; i < rangedAnimTimes.size; i++) rangedAnimTimes.set(i, rangedAnimTimes.get(i) + delta);

        // anim fallback
        playerAnim.set(VisualMapper.playerKey(run.player));
        playerAnim.update(delta);
    }

    private void renderWorld() {
        // ---------- BACKGROUND (parallax) ----------
        if (background != null) {
            batch.setProjectionMatrix(worldCamera.combined);
            batch.begin();
            background.render(batch, worldCamera.position.x, worldCamera.position.y, GameConfig.VIRTUAL_W, GameConfig.VIRTUAL_H);
            batch.end();
        }

        // ---------- FILLED SHAPES (platforms, door, pickups, projectiles) ----------
        shapes.setProjectionMatrix(worldCamera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        if (run.platformRects != null) {
            for (var p : run.platformRects) {
                if ("oneway".equals(p.type)) shapes.setColor(0.35f, 0.35f, 0.38f, 1f);
                else shapes.setColor(0.20f, 0.20f, 0.22f, 1f);
                shapes.rect(p.cx - p.w / 2f, p.cy - p.h / 2f, p.w, p.h);
            }
        }

        if (doorClosed && doorBody != null) {
            float dx = PhysicsConstants.toPixels(doorBody.getPosition().x);
            float dy = PhysicsConstants.toPixels(doorBody.getPosition().y);
            shapes.setColor(0.08f, 0.08f, 0.09f, 1f);
            shapes.rect(dx - 14f, dy - 110f, 28f, 220f);
        }

        shapes.setColor(0.10f, 0.45f, 0.10f, 1f);
        for (RelicPickup p : run.pickups) shapes.circle(p.getXpx(), p.getYpx(), 6f, 16);

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

        shapes.end();

        // ---------- SPRITES (enemies + player) ----------
        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();

        // Melee enemies
        EnemySprites meleeSprites = sprites.eldarMelee();
        for (int i = 0; i < run.meleeEnemies.size; i++) {
            MeleeEnemy e = run.meleeEnemies.get(i);
            EnemySprites.State st = EnemyStateMapper.map(String.valueOf(e.getState()));
            float t = (i < meleeAnimTimes.size) ? meleeAnimTimes.get(i) : 0f;

            TextureRegion frame = EnemySprites.isOneShot(st) ? meleeSprites.getOnce(st, t) : meleeSprites.get(st, t);

            if (frame != null) {
                float ex = e.getXpx();
                float footY = e.getYpx() - meleeSprites.feetOffsetPx;

                float w = frame.getRegionWidth() * meleeSprites.scale;
                float h = frame.getRegionHeight() * meleeSprites.scale;

                float drawX = ex - w / 2f;
                float drawY = footY;

                boolean flipX = e.getFacingDir() < 0;
                if (frame.isFlipX() != flipX) frame.flip(true, false);

                batch.draw(frame, drawX, drawY, w, h);
            }
        }

        // Ranged enemies
        EnemySprites rangedSprites = sprites.eldarRanged();
        for (int i = 0; i < run.rangedEnemies.size; i++) {
            RangedEnemy e = run.rangedEnemies.get(i);
            EnemySprites.State st = EnemyStateMapper.map(String.valueOf(e.getState()));
            float t = (i < rangedAnimTimes.size) ? rangedAnimTimes.get(i) : 0f;

            TextureRegion frame = EnemySprites.isOneShot(st) ? rangedSprites.getOnce(st, t) : rangedSprites.get(st, t);

            if (frame != null) {
                float ex = e.getXpx();
                float footY = e.getYpx() - rangedSprites.feetOffsetPx;

                float w = frame.getRegionWidth() * rangedSprites.scale;
                float h = frame.getRegionHeight() * rangedSprites.scale;

                float drawX = ex - w / 2f;
                float drawY = footY;

                boolean flipX = e.getFacingDir() < 0;
                if (frame.isFlipX() != flipX) frame.flip(true, false);

                batch.draw(frame, drawX, drawY, w, h);
            }
        }

        // Player
        PlayerSprites ps = sprites.player();
        TextureRegion pFrame = PlayerSprites.isOneShot(playerVisualState)
            ? ps.getOnce(playerVisualState, playerStateTime)
            : ps.get(playerVisualState, playerStateTime);

        if (pFrame != null) {
            float px = run.player.getXpx();
            float footY = run.player.getYpx() - ps.feetOffsetPx;

            float w = pFrame.getRegionWidth() * ps.scale;
            float h = pFrame.getRegionHeight() * ps.scale;

            float drawX = px - w / 2f;
            float drawY = footY;

            boolean flipX = run.player.getFacingDir() < 0;
            if (pFrame.isFlipX() != flipX) pFrame.flip(true, false);

            batch.draw(pFrame, drawX, drawY, w, h);
        }

        batch.end();

        // ---------- FALLBACK enemy shapes where sprites are missing ----------
        shapes.setProjectionMatrix(worldCamera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        for (int i = 0; i < run.meleeEnemies.size; i++) {
            MeleeEnemy e = run.meleeEnemies.get(i);
            EnemySprites.State st = EnemyStateMapper.map(String.valueOf(e.getState()));
            float t = (i < meleeAnimTimes.size) ? meleeAnimTimes.get(i) : 0f;
            TextureRegion frame = EnemySprites.isOneShot(st) ? meleeSprites.getOnce(st, t) : meleeSprites.get(st, t);

            if (frame == null) {
                float ex = e.getXpx();
                float ey = e.getYpx() - 14f;
                shapes.setColor(0.12f, 0.12f, 0.16f, 1f);
                charRenderer.draw(shapes, ex, ey, e.getFacingDir(), run.meleeAnims.get(i).getFrame());
            }
        }

        for (int i = 0; i < run.rangedEnemies.size; i++) {
            RangedEnemy e = run.rangedEnemies.get(i);
            EnemySprites.State st = EnemyStateMapper.map(String.valueOf(e.getState()));
            float t = (i < rangedAnimTimes.size) ? rangedAnimTimes.get(i) : 0f;
            TextureRegion frame = EnemySprites.isOneShot(st) ? rangedSprites.getOnce(st, t) : rangedSprites.get(st, t);

            if (frame == null) {
                float ex = e.getXpx();
                float ey = e.getYpx() - 14f;
                shapes.setColor(0.10f, 0.10f, 0.22f, 1f);
                shapes.rect(ex - 10f, ey, 20f, 28f);
            }
        }

        shapes.end();

        // ---------- DEBUG LINES ----------
        if (debugHitboxes || debugHurtboxes) {
            shapes.setProjectionMatrix(worldCamera.combined);
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
        shapes.setProjectionMatrix(uiCamera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        // Souls-like bars + legion circle
        if (hudEssentialOn) {
            float baseX = 14f;
            float baseY = GameConfig.VIRTUAL_H - 20f;

            float circleR = 16f;
            float circleCx = baseX + circleR;
            float circleCy = baseY - 10f;

            shapes.setColor(0.05f, 0.05f, 0.06f, 1f);
            shapes.circle(circleCx, circleCy, circleR + 2f, 24);
            shapes.setColor(0.12f, 0.12f, 0.14f, 1f);
            shapes.circle(circleCx, circleCy, circleR, 24);

            float barsX = circleCx + circleR + 10f;
            float barW = 170f;
            float barH = 7f;
            float gap = 4f;

            int hp = run.player.getHealth().getHp();
            int hpMax = run.player.getHealth().getMaxHp();
            float hpPct = (hpMax <= 0) ? 0f : (hp / (float) hpMax);
            hpPct = Math.max(0f, Math.min(1f, hpPct));

            float staminaPct = 1f;
            float energyPct = 1f;

            shapes.setColor(0.05f, 0.05f, 0.06f, 1f);
            shapes.rect(barsX, baseY - 0f, barW, barH);
            shapes.rect(barsX, baseY - (barH + gap), barW, barH);
            shapes.rect(barsX, baseY - 2f * (barH + gap), barW, barH);

            shapes.setColor(0.55f, 0.12f, 0.12f, 1f);
            shapes.rect(barsX, baseY - 0f, barW * hpPct, barH);

            shapes.setColor(0.15f, 0.55f, 0.18f, 1f);
            shapes.rect(barsX, baseY - (barH + gap), barW * staminaPct, barH);

            shapes.setColor(0.14f, 0.25f, 0.65f, 1f);
            shapes.rect(barsX, baseY - 2f * (barH + gap), barW * energyPct, barH);
        }

        // Fade overlay
        if (transition.fade > 0f) {
            shapes.setColor(0f, 0f, 0f, transition.fade);
            shapes.rect(0, 0, GameConfig.VIRTUAL_W, GameConfig.VIRTUAL_H);
        }

        shapes.end();

        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();

        // Legion icon (safe)
        if (hudEssentialOn) {
            TextureRegion legion = sprites.legionSalamanders();
            if (legion != null) {
                float baseX = 14f;
                float baseY = GameConfig.VIRTUAL_H - 20f;
                float circleR = 16f;
                float circleCx = baseX + circleR;
                float circleCy = baseY - 10f;
                float size = 32f;
                batch.draw(legion, circleCx - size / 2f, circleCy - size / 2f, size, size);
            }
        }

        // Essential text
        if (hudEssentialOn) {
            int hp = run.player.getHealth().getHp();
            int maxHp = run.player.getHealth().getMaxHp();
            int enemiesAlive = run.meleeEnemies.size + run.rangedEnemies.size;

            font.draw(batch, "HP " + hp + "/" + maxHp, 14f, GameConfig.VIRTUAL_H - 60f);
            font.draw(batch, "Enemies: " + enemiesAlive, 14f, GameConfig.VIRTUAL_H - 78f);

            // Weapon placeholder
            float panelW = 220f;
            float x = GameConfig.VIRTUAL_W - panelW - 12f;
            float y = 12f;
            font.draw(batch, "WEAPON", x + 10f, y + 50f);
            font.draw(batch, "Current: [TODO]", x + 10f, y + 34f);
            font.draw(batch, "Ammo: [--/--]", x + 10f, y + 18f);
        }

        // Debug/info
        if (hudInfoOn) {
            float x = GameConfig.VIRTUAL_W - 248f;
            float y = GameConfig.VIRTUAL_H - 18f;

            int fps = Gdx.graphics.getFramesPerSecond();
            RoomInstance room = run.run.current();

            font.draw(batch, "DEBUG / INFO", x, y); y -= 18f;
            font.draw(batch, "FPS: " + fps, x, y); y -= 18f;
            font.draw(batch, "Seed: " + run.run.seed, x, y); y -= 18f;
            font.draw(batch, "Room: " + (run.run.index + 1) + "/" + run.run.totalRooms + " [" + room.type + "]", x, y); y -= 18f;
            font.draw(batch, "Tpl: " + room.template.id + " | Budget: " + room.budget, x, y); y -= 18f;
            font.draw(batch, "Plan M:" + room.meleeCount + " R:" + room.rangedCount, x, y); y -= 18f;

            font.draw(batch, String.format("ShootCD: %.2f", run.shootCooldown), x, y); y -= 18f;
            font.draw(batch, "ChoiceRoom: " + run.inChoiceRoom, x, y); y -= 18f;

            font.draw(batch,
                "Relics: " + run.relics.getOwned().size +
                    " | Dmg+" + run.relics.getBonusProjectileDamage() +
                    " | FRx" + String.format("%.2f", (1f / run.relics.getFireRateMultiplier())) +
                    " | P" + run.relics.getPiercingShots(),
                x, y);

            font.draw(batch, "F1 Essential | F2 Info | H hitbox | U hurtbox | F5 reload", x, 20f);
        }

        batch.end();
    }

    private void renderCriticalOverlay() {
        if (run.player == null) return;

        if (!run.player.isAlive()) {
            batch.setProjectionMatrix(uiCamera.combined);
            batch.begin();
            hudPainter.drawFadingText(batch, font, "YOU DIED - Press R to restart run", 200, 200, 1f);
            batch.end();
        }
    }

    private void spawnDoorForCurrentRoom() {
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

    private void syncEnemyAnimTimers() {
        meleeAnimTimes.clear();
        for (int i = 0; i < run.meleeEnemies.size; i++) meleeAnimTimes.add(0f);

        rangedAnimTimes.clear();
        for (int i = 0; i < run.rangedEnemies.size; i++) rangedAnimTimes.add(0f);
    }

    @Override
    public void resize(int width, int height) {
        worldViewport.update(width, height, true);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        sprites.dispose();
        shapes.dispose();
        batch.dispose();
        font.dispose();
        if (run.physics != null) run.physics.dispose();
    }
}
