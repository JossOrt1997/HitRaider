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
import com.analiticasoft.hitraider.world.*;
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

    private boolean debugOverlay = true;
    private boolean debugHitboxes = true;
    private boolean debugHurtboxes = true;

    private final InputState input = new InputState();
    private final DesktopInputProvider inputProvider = new DesktopInputProvider();

    private PhysicsWorld physics;
    private CombatSystem combat;
    private ProjectileSystem projectiles;
    private GameContactListener contactListener;

    private Array<LevelFactory.PlatformRect> platformRects;

    private Player player;
    private final Array<MeleeEnemy> meleeEnemies = new Array<>();
    private final Array<RangedEnemy> rangedEnemies = new Array<>();

    private final EncounterManager encounter = new EncounterManager();

    private Body doorBody;
    private boolean doorClosed = false;
    private static final float DOOR_W = 28f;
    private static final float DOOR_H = 220f;

    private final CharacterRenderer charRenderer = new DebugCharacterRenderer();
    private final DebugPhysicsRenderer debugPhysics = new DebugPhysicsRenderer();
    private CharacterAnimator playerAnim;
    private final Array<CharacterAnimator> meleeAnims = new Array<>();
    private final Array<CharacterAnimator> rangedAnims = new Array<>();

    private final RelicManager relics = new RelicManager();
    private final Array<RelicPickup> pickups = new Array<>();
    private final Random rng = new Random();

    // Week7 run
    private final RunManager run = new RunManager();
    private final RoomTemplateRegistry templates = new RoomTemplateRegistry();
    private final RoomInstanceGenerator generator = new RoomInstanceGenerator();
    private final DropRules dropRules = new DropRules();
    private Array<RoomInstance> runRooms;

    private boolean relicDroppedThisRoom = false;

    // choice
    private boolean inChoiceRoom = false;

    // lifesteal
    private int meleeHitCounter = 0;

    // shoot cooldown
    private float shootCooldown = 0f;
    private static final float SHOOT_BASE_COOLDOWN = 0.28f;

    // polish
    private float hitstopTimer = 0f;
    private float shakeTimer = 0f;
    private float shakeIntensity = 0f;
    private float shakeSeed = 0f;

    private float fade = 0f;
    private boolean transitioning = false;
    private float transitionTimer = 0f;
    private boolean transitionToNextRoom = false;

    private float clearTextTimer = 0f;

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

        buildTemplates();
        startNewRun(true);

        worldCamera.position.set(VIRTUAL_W / 2f, VIRTUAL_H / 2f, 0f);
        worldCamera.update();
    }

    private void buildTemplates() {
        templates.clear();

        RoomTemplate arena = new RoomTemplate("arena", 120f, 140f, 1120f)
            .addSpawn(520f, 220f).addSpawn(650f, 220f).addSpawn(820f, 220f).addSpawn(950f, 220f).addSpawn(1040f, 220f);

        RoomTemplate platforms = new RoomTemplate("platforms", 120f, 140f, 1120f)
            .addSpawn(520f, 260f).addSpawn(650f, 260f).addSpawn(820f, 220f).addSpawn(950f, 220f).addSpawn(1040f, 220f);

        RoomTemplate hall = new RoomTemplate("hall", 120f, 140f, 1120f)
            .addSpawn(520f, 220f).addSpawn(650f, 220f).addSpawn(760f, 220f).addSpawn(880f, 220f).addSpawn(1000f, 220f);

        templates.add(arena);
        templates.add(platforms);
        templates.add(hall);
    }

    private void startNewRun(boolean rebuildPhysics) {
        long seed = System.currentTimeMillis();
        int totalRooms = 12;

        runRooms = generator.generate(seed, totalRooms, templates);
        run.start(seed, totalRooms, runRooms);

        loadCurrentRoom(rebuildPhysics);
    }

    private void loadCurrentRoom(boolean rebuildPhysics) {
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

        relicDroppedThisRoom = false;
        inChoiceRoom = false;

        doorBody = null;
        doorClosed = false;

        RoomInstance room = run.current();

        // place player
        player.body.setTransform(PhysicsConstants.toMeters(room.template.entryXpx), PhysicsConstants.toMeters(room.template.entryYpx), 0f);
        player.body.setLinearVelocity(0f, 0f);

        // CHOICE room: no enemies, 2 relics
        if (room.type == RoomType.CHOICE) {
            inChoiceRoom = true;
            spawnChoiceRelics(room);
            // open door immediately after choice (but we lock until player picks one)
            closeDoorAt(room.template.exitXpx, 120f);
        } else {
            // spawn enemies
            int si = 0;
            for (int i = 0; i < room.meleeCount; i++) {
                Vector2 sp = room.spawnOrder.get(si++ % room.spawnOrder.size);
                meleeEnemies.add(new MeleeEnemy(physics.world, sp.x, sp.y));
                CharacterAnimator a = new CharacterAnimator();
                DebugAnimLibrary.defineMeleeEnemy(a);
                meleeAnims.add(a);
            }
            for (int i = 0; i < room.rangedCount; i++) {
                Vector2 sp = room.spawnOrder.get(si++ % room.spawnOrder.size);
                rangedEnemies.add(new RangedEnemy(physics.world, sp.x, sp.y));
                CharacterAnimator a = new CharacterAnimator();
                DebugAnimLibrary.defineMeleeEnemy(a);
                rangedAnims.add(a);
            }

            closeDoorAt(room.template.exitXpx, 120f);
        }

        encounter.reset();
        hitstopTimer = 0f;
        shootCooldown = 0f;
        clearTextTimer = 0f;
        meleeHitCounter = 0;

        // fade in
        fade = 1f;
        transitioning = true;
        transitionTimer = 0f;
        transitionToNextRoom = false;
    }

    private void spawnChoiceRelics(RoomInstance room) {
        // elige 2 distintas
        Random rr = new Random(room.seed ^ 0x1234ABCD);
        RelicType a = dropRules.rollRelic(rr);
        RelicType b = dropRules.rollRelic(rr);
        if (a == b) b = (a == RelicType.BONUS_PROJECTILE_DAMAGE) ? RelicType.FIRE_RATE_UP : RelicType.BONUS_PROJECTILE_DAMAGE;

        // 2 pickups
        pickups.add(new RelicPickup(physics.world, a, 520f, 220f));
        pickups.add(new RelicPickup(physics.world, b, 820f, 220f));
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

        if (Gdx.input.isKeyJustPressed(Input.Keys.F1) || Gdx.input.isKeyJustPressed(Input.Keys.TAB)) debugOverlay = !debugOverlay;
        if (Gdx.input.isKeyJustPressed(Input.Keys.H)) debugHitboxes = !debugHitboxes;
        if (Gdx.input.isKeyJustPressed(Input.Keys.U)) debugHurtboxes = !debugHurtboxes;

        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) startNewRun(false);

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
        if (shootCooldown > 0f) shootCooldown = Math.max(0f, shootCooldown - delta);

        // transition fade
        if (transitioning) {
            transitionTimer += delta;
            float t = Math.min(1f, transitionTimer / 0.25f);

            if (!transitionToNextRoom) {
                fade = 1f - t;
                if (t >= 1f) transitioning = false;
            } else {
                fade = t;
                if (t >= 1f) {
                    if (run.hasNext()) {
                        run.next();
                        loadCurrentRoom(false);
                    } else {
                        startNewRun(false);
                    }
                    return;
                }
            }
        }

        if (shakeTimer > 0f) {
            shakeTimer = Math.max(0f, shakeTimer - delta);
            shakeSeed += 31.7f * delta;
        }

        if (clearTextTimer > 0f) clearTextTimer = Math.max(0f, clearTextTimer - delta);

        combat.beginFrame();

        // If dead: stop sliding
        if (!player.isAlive()) {
            Vector2 v = player.body.getLinearVelocity();
            player.body.setLinearVelocity(0f, v.y);
        } else {
            // Optional (if your Player supports it): apply dash cooldown relic
            // player.setDashCooldownMultiplier(relics.getDashCooldownMultiplier());

            player.update(delta, input);

            if (player.shouldSpawnAttackHitboxThisFrame()) {
                combat.spawnMeleeHitbox(player.body, player, player.getFaction(), player.getFacingDir(), player.getAimY(input), 1);
            }

            // Shoot with cooldown + fire rate relic + piercing
            if (input.isJustPressed(Action.SHOOT) && shootCooldown <= 0f) {
                int dmg = 1 + relics.getBonusProjectileDamage();
                float sx = player.getXpx() + player.getFacingDir() * 14f;
                float sy = player.getYpx() + 10f;

                Projectile p = new Projectile(physics.world, player.getFaction(), dmg, sx, sy, player.getFacingDir() * 8.5f, 0f, 1.2f);
                p.piercesLeft = relics.getPiercingShots();
                projectiles.spawn(p);

                shootCooldown = SHOOT_BASE_COOLDOWN * relics.getFireRateMultiplier();
            }
        }

        // enemies skipped in choice room
        if (!inChoiceRoom) {
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

                    RoomInstance room = run.current();
                    if (!relicDroppedThisRoom && rng.nextFloat() < room.relicDropChance) {
                        RelicType t = dropRules.rollRelic(new Random(room.seed ^ (long) i * 1315423911L));
                        pickups.add(new RelicPickup(physics.world, t, e.getXpx(), e.getYpx()));
                        relicDroppedThisRoom = true;
                    }
                }
            }

            for (int i = rangedEnemies.size - 1; i >= 0; i--) {
                RangedEnemy re = rangedEnemies.get(i);
                re.update(delta, player);

                rangedAnims.get(i).set(AnimKey.ENEMY_CHASE);
                rangedAnims.get(i).update(delta);

                if (re.didShootThisFrame()) {
                    float sx = re.getXpx() + re.getFacingDir() * 14f;
                    float sy = re.getYpx() + 10f;
                    Projectile p = new Projectile(physics.world, re.getFaction(), 1, sx, sy, re.getFacingDir() * 7.5f, 0f, 1.4f);
                    projectiles.spawn(p);
                }

                if (!re.isAlive()) {
                    physics.world.destroyBody(re.body);
                    rangedEnemies.removeIndex(i);
                    rangedAnims.removeIndex(i);
                }
            }
        }

        combat.update(delta);

        physics.step(delta);

        projectiles.flushImpacts();

        // Shake per event
        if (combat.consumePlayerHurt()) startShake(0.10f, 4.5f);

        // Lifesteal (counts enemy-hurt events as melee hits approximation)
        if (combat.consumeEnemyHurt()) {
            startShake(0.08f, 2.5f);

            int N = relics.getLifestealEveryHits();
            if (N > 0 && player.isAlive()) {
                meleeHitCounter++;
                if (meleeHitCounter % N == 0) {
                    player.getHealth().heal(1);
                }
            }
        }

        if (combat.consumeMeleeWorldHit()) startShake(0.08f, 2.5f);

        int pe = projectiles.consumeImpactsEnemy();
        if (pe > 0) {
            startShake(0.06f, 1.8f);
            hitstopTimer = Math.max(hitstopTimer, 0.02f);
        }
        int pw = projectiles.consumeImpactsWorld();
        if (pw > 0) {
            startShake(0.05f, 0.9f);
        }

        projectiles.update(delta);

        // pickup collection
        for (int i = pickups.size - 1; i >= 0; i--) {
            RelicPickup p = pickups.get(i);
            if (p.collected) {
                // if choice room: picking one removes the other
                if (inChoiceRoom) {
                    // remove all pickups bodies safely
                    for (int k = pickups.size - 1; k >= 0; k--) {
                        RelicPickup other = pickups.get(k);
                        if (other.body.getWorld() != null) physics.world.destroyBody(other.body);
                        pickups.removeIndex(k);
                    }
                    inChoiceRoom = false;
                    openDoor();
                    clearTextTimer = 0f;
                } else {
                    relics.add(p.type);
                    physics.world.destroyBody(p.body);
                    pickups.removeIndex(i);
                }
            }
        }

        if (combat.hitThisFrame()) {
            hitstopTimer = Math.max(hitstopTimer, 0.05f);
            startShake(0.10f, 4.0f);
        }

        int alive = meleeEnemies.size + rangedEnemies.size;
        encounter.update(delta, alive);

        // Choice room opens door when picked, otherwise locked
        if (run.current().type == RoomType.CHOICE) {
            // keep locked until picked
        } else if (encounter.getState() == EncounterManager.State.CLEAR) {
            openDoor();
            clearTextTimer = Math.max(clearTextTimer, 1.2f);
        }

        RoomInstance r = run.current();
        if (r != null && !transitioning) {
            boolean canExit = (r.type == RoomType.CHOICE) ? !inChoiceRoom : (encounter.getState() == EncounterManager.State.CLEAR);
            if (canExit && player.getXpx() > r.template.exitXpx + 20f) {
                transitioning = true;
                transitionToNextRoom = true;
                transitionTimer = 0f;
            }
        }

        // camera
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
        float sx = (float)Math.sin(shakeSeed * 17.0) * shakeIntensity;
        float sy = (float)Math.cos(shakeSeed * 23.0) * shakeIntensity;
        worldCamera.position.x += sx;
        worldCamera.position.y += sy;
    }

    private void renderWorld() {
        shapes.setProjectionMatrix(worldCamera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        if (platformRects != null) {
            for (LevelFactory.PlatformRect p : platformRects) {
                if ("oneway".equals(p.type)) shapes.setColor(0.35f, 0.35f, 0.38f, 1f);
                else shapes.setColor(0.20f, 0.20f, 0.22f, 1f);
                shapes.rect(p.cx - p.w/2f, p.cy - p.h/2f, p.w, p.h);
            }
        }

        if (doorClosed && doorBody != null) {
            float dx = PhysicsConstants.toPixels(doorBody.getPosition().x);
            float dy = PhysicsConstants.toPixels(doorBody.getPosition().y);
            shapes.setColor(0.08f, 0.08f, 0.09f, 1f);
            shapes.rect(dx - DOOR_W/2f, dy - DOOR_H/2f, DOOR_W, DOOR_H);
        }

        shapes.setColor(0.10f, 0.45f, 0.10f, 1f);
        for (RelicPickup p : pickups) shapes.circle(p.getXpx(), p.getYpx(), 6f, 16);

        for (Projectile pr : projectiles.projectiles) {
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

        for (int i = 0; i < meleeEnemies.size; i++) {
            MeleeEnemy e = meleeEnemies.get(i);
            float ex = e.getXpx();
            float ey = e.getYpx() - 14f;

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

            shapes.rect(ex - 10f, ey, 20f, 28f);
        }

        float px = player.getXpx();
        float py = player.getYpx() - 16f;
        if (player.isFlashing()) shapes.setColor(0.02f, 0.02f, 0.02f, 1f);
        else shapes.setColor(0.10f, 0.10f, 0.12f, 1f);
        charRenderer.draw(shapes, px, py, player.getFacingDir(), playerAnim.getFrame());

        shapes.end();

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
        shapes.setProjectionMatrix(uiCamera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        float barX = 10f, barY = VIRTUAL_H - 26f, barW = 200f, barH = 10f;
        int hp = player.getHealth().getHp();
        int maxHp = player.getHealth().getMaxHp();
        float pct = (maxHp <= 0) ? 0f : (hp / (float) maxHp);
        pct = Math.max(0f, Math.min(1f, pct));

        shapes.setColor(0.10f, 0.10f, 0.12f, 1f);
        shapes.rect(barX, barY, barW, barH);
        shapes.setColor(0.20f, 0.75f, 0.25f, 1f);
        shapes.rect(barX, barY, barW * pct, barH);

        if (fade > 0f) {
            shapes.setColor(0f, 0f, 0f, fade);
            shapes.rect(0, 0, VIRTUAL_W, VIRTUAL_H);
        }

        shapes.end();

        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();

        RoomInstance room = run.current();

        font.draw(batch, "Seed: " + run.seed, 10, VIRTUAL_H - 10);
        font.draw(batch, "Room: " + (run.index + 1) + "/" + run.totalRooms + " [" + room.type + "]  tpl=" + room.template.id, 10, VIRTUAL_H - 42);
        font.draw(batch, "Budget: " + room.budget + "  M:" + room.meleeCount + " R:" + room.rangedCount, 10, VIRTUAL_H - 58);

        font.draw(batch, "Relics: " + relics.getOwned().size, 10, VIRTUAL_H - 76);
        font.draw(batch, "+ProjDmg: +" + relics.getBonusProjectileDamage(), 120, VIRTUAL_H - 76);
        font.draw(batch, "FireRate x" + String.format("%.2f", (1f / relics.getFireRateMultiplier())), 260, VIRTUAL_H - 76);
        font.draw(batch, "Pierce: " + relics.getPiercingShots(), 430, VIRTUAL_H - 76);
        font.draw(batch, "Lifesteal: " + relics.getLifestealEveryHits(), 510, VIRTUAL_H - 76);

        font.draw(batch, "K shoot | J melee | R new run | F1/TAB overlay | H/U debug", 10, 20);

        if (room.type == RoomType.CHOICE && inChoiceRoom) {
            hud.drawFadingText(batch, font, "CHOOSE ONE RELIC", 250, 220, 1f);
        } else if (clearTextTimer > 0f) {
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
