package com.analiticasoft.hitraider.screens;

import com.analiticasoft.hitraider.combat.CombatSystem;
import com.analiticasoft.hitraider.combat.Projectile;
import com.analiticasoft.hitraider.combat.ProjectileSystem;
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
import com.analiticasoft.hitraider.world.EncounterManager;
import com.analiticasoft.hitraider.world.LevelFactory;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.Fixture;
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

    private boolean debugOverlay = true;
    private boolean debugWorld = true;
    private boolean box2dDebugOn = false;
    private boolean debugHitboxes = true;
    private boolean debugHurtboxes = true;

    private final InputState input = new InputState();
    private final DesktopInputProvider inputProvider = new DesktopInputProvider();

    private PhysicsWorld physics;
    private CombatSystem combat;
    private ProjectileSystem projectiles;
    private GameContactListener contactListener;

    private Box2DDebugRenderer box2dDebug;
    private final Matrix4 box2dMatrix = new Matrix4();

    private Array<LevelFactory.PlatformRect> platformRects;

    private Player player;

    private final Array<MeleeEnemy> meleeEnemies = new Array<>();
    private final Array<CharacterAnimator> meleeAnims = new Array<>();

    private final Array<RangedEnemy> rangedEnemies = new Array<>();
    private final Array<CharacterAnimator> rangedAnims = new Array<>();

    private final Array<RelicPickup> pickups = new Array<>();
    private final RelicManager relics = new RelicManager();

    private float hitstopTimer = 0f;
    private final EncounterManager encounter = new EncounterManager();

    private final CharacterRenderer charRenderer = new DebugCharacterRenderer();
    private final DebugPhysicsRenderer debugPhysics = new DebugPhysicsRenderer();
    private CharacterAnimator playerAnim;

    private final Random rng = new Random();

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

        box2dDebug = new Box2DDebugRenderer();

        playerAnim = new CharacterAnimator();
        DebugAnimLibrary.definePlayer(playerAnim);

        initWorld();

        worldCamera.position.set(VIRTUAL_W / 2f, VIRTUAL_H / 2f, 0f);
        worldCamera.update();
    }

    private void initWorld() {
        if (physics != null) physics.dispose();

        physics = new PhysicsWorld(new Vector2(0f, -25f));
        combat = new CombatSystem(physics.world);
        projectiles = new ProjectileSystem(physics.world);

        contactListener = new GameContactListener(combat);
        physics.world.setContactListener(contactListener);

        platformRects = LevelFactory.createTestLevel(physics.world);

        meleeEnemies.clear();
        meleeAnims.clear();
        rangedEnemies.clear();
        rangedAnims.clear();
        pickups.clear();

        player = new Player(physics.world, 120f, 140f);

        spawnMelee(520f, 220f);
        spawnMelee(650f, 220f);
        spawnRanged(820f, 220f);

        encounter.reset();
        hitstopTimer = 0f;
    }

    private void spawnMelee(float x, float y) {
        meleeEnemies.add(new MeleeEnemy(physics.world, x, y, true));
        CharacterAnimator a = new CharacterAnimator();
        DebugAnimLibrary.defineMeleeEnemy(a);
        meleeAnims.add(a);
    }

    private void spawnRanged(float x, float y) {
        rangedEnemies.add(new RangedEnemy(physics.world, x, y));
        CharacterAnimator a = new CharacterAnimator();
        DebugAnimLibrary.defineMeleeEnemy(a); // reuso frames por ahora
        rangedAnims.add(a);
    }

    @Override
    public void render(float delta) {
        inputProvider.poll(input);

        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) debugOverlay = !debugOverlay;
        if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) debugWorld = !debugWorld;
        if (Gdx.input.isKeyJustPressed(Input.Keys.B)) box2dDebugOn = !box2dDebugOn;
        if (Gdx.input.isKeyJustPressed(Input.Keys.H)) debugHitboxes = !debugHitboxes;
        if (Gdx.input.isKeyJustPressed(Input.Keys.U)) debugHurtboxes = !debugHurtboxes;

        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) initWorld();

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
        renderOverlay();

        input.endFrame();
    }

    private void update(float delta) {
        combat.beginFrame();

        // Player
        player.update(delta, input);

        // Player melee
        if (player.shouldSpawnAttackHitboxThisFrame()) {
            combat.spawnMeleeHitbox(
                player.body,
                player,
                player.getFaction(),
                player.getFacingDir(),
                player.getAimY(input),
                1
            );
        }

        // Player shoot (K)
        if (input.isJustPressed(Action.SHOOT)) {
            int damage = 1 + relics.getBonusProjectileDamage();
            float startX = player.getXpx() + player.getFacingDir() * 14f;
            float startY = player.getYpx() + 10f;

            // velocidad en m/s
            float vx = player.getFacingDir() * 8.5f;
            float vy = 0f;

            projectiles.spawn(new Projectile(
                physics.world,
                player.getFaction(),
                damage,
                startX, startY,
                vx, vy,
                1.2f
            ));
        }

        // Melee enemies
        for (int i = meleeEnemies.size - 1; i >= 0; i--) {
            MeleeEnemy e = meleeEnemies.get(i);
            e.update(delta, player);

            CharacterAnimator a = meleeAnims.get(i);
            a.set(VisualMapper.enemyKey(e));
            a.update(delta);

            if (e.didStartAttackThisFrame()) {
                combat.spawnMeleeHitbox(
                    e.body,
                    e,
                    e.getFaction(),
                    e.getFacingDir(),
                    0,
                    1
                );
            }

            if (!e.isAlive()) {
                combat.purgeForBody(e.body);
                physics.world.destroyBody(e.body);
                meleeEnemies.removeIndex(i);
                meleeAnims.removeIndex(i);

                // Drop chance (25%)
                if (rng.nextFloat() < 0.25f) {
                    pickups.add(new RelicPickup(physics.world, RelicType.BONUS_PROJECTILE_DAMAGE, e.getXpx(), e.getYpx()));
                }
            }
        }

        // Ranged enemies
        for (int i = rangedEnemies.size - 1; i >= 0; i--) {
            RangedEnemy e = rangedEnemies.get(i);
            e.update(delta, player);

            CharacterAnimator a = rangedAnims.get(i);
            // Reuso de keys melee para simplificar visual: chase/telegraph/attack
            a.set(AnimKey.ENEMY_CHASE);
            a.update(delta);

            if (e.didShootThisFrame()) {
                float startX = e.getXpx() + e.getFacingDir() * 14f;
                float startY = e.getYpx() + 10f;

                float vx = e.getFacingDir() * 7.5f;
                float vy = 0f;

                projectiles.spawn(new Projectile(
                    physics.world,
                    e.getFaction(),
                    1,
                    startX, startY,
                    vx, vy,
                    1.4f
                ));
            }

            if (!e.isAlive()) {
                physics.world.destroyBody(e.body);
                rangedEnemies.removeIndex(i);
                rangedAnims.removeIndex(i);
            }
        }

        // Update projectiles & pickups AFTER physics step
        combat.update(delta);

        physics.step(delta);

        // projectile cleanup
        projectiles.update(delta);

        // pickup collection (flagged by listener)
        for (int i = pickups.size - 1; i >= 0; i--) {
            RelicPickup p = pickups.get(i);
            if (p.collected) {
                relics.add(p.type);
                physics.world.destroyBody(p.body);
                pickups.removeIndex(i);
            }
        }

        if (combat.hitThisFrame()) hitstopTimer = 0.05f;

        // Encounter
        int enemiesAlive = meleeEnemies.size + rangedEnemies.size;
        encounter.update(delta, enemiesAlive);

        // Camera
        float px = player.getXpx();
        float py = player.getYpx();
        worldCamera.position.x += (px - worldCamera.position.x) * 0.12f;
        worldCamera.position.y += ((py + 40f) - worldCamera.position.y) * 0.10f;
        worldCamera.update();

        // Player anim
        playerAnim.set(VisualMapper.playerKey(player));
        playerAnim.update(delta);
    }

    private void renderWorld() {
        shapes.setProjectionMatrix(worldCamera.combined);

        // FILLED
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        // Platforms
        if (platformRects != null) {
            for (LevelFactory.PlatformRect p : platformRects) {
                if ("oneway".equals(p.type)) shapes.setColor(0.35f, 0.35f, 0.38f, 1f);
                else shapes.setColor(0.20f, 0.20f, 0.22f, 1f);
                shapes.rect(p.cx - p.w / 2f, p.cy - p.h / 2f, p.w, p.h);
            }
        }

        // Pickups
        shapes.setColor(0.10f, 0.45f, 0.10f, 1f);
        for (RelicPickup p : pickups) {
            shapes.circle(p.getXpx(), p.getYpx(), 6f, 16);
        }

        // Projectiles
        shapes.setColor(0.10f, 0.10f, 0.10f, 1f);
        for (Projectile pr : projectiles.projectiles) {
            shapes.rect(pr.getXpx() - 3f, pr.getYpx() - 3f, 6f, 6f);
        }

        // Melee enemies
        for (int i = 0; i < meleeEnemies.size; i++) {
            MeleeEnemy e = meleeEnemies.get(i);
            CharacterAnimator a = meleeAnims.get(i);

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

            charRenderer.draw(shapes, ex, ey, e.getFacingDir(), a.getFrame());
        }

        // Ranged enemies (color distinto)
        for (int i = 0; i < rangedEnemies.size; i++) {
            RangedEnemy e = rangedEnemies.get(i);
            float ex = e.getXpx();
            float ey = e.getYpx() - 14f;

            if (e.isFlashing()) shapes.setColor(0.05f, 0.05f, 0.05f, 1f);
            else shapes.setColor(0.10f, 0.10f, 0.22f, 1f);

            // rect simple por ahora
            shapes.rect(ex - 10f, ey, 20f, 28f);
        }

        // Player
        float px = player.getXpx();
        float py = player.getYpx() - 16f;

        if (player.isFlashing()) shapes.setColor(0.02f, 0.02f, 0.02f, 1f);
        else shapes.setColor(0.10f, 0.10f, 0.12f, 1f);

        charRenderer.draw(shapes, px, py, player.getFacingDir(), playerAnim.getFrame());

        shapes.end();

        // LINES debug
        shapes.begin(ShapeRenderer.ShapeType.Line);

        if (debugWorld && platformRects != null) {
            shapes.setColor(0.05f, 0.05f, 0.06f, 1f);
            for (LevelFactory.PlatformRect p : platformRects) {
                shapes.rect(p.cx - p.w / 2f, p.cy - p.h / 2f, p.w, p.h);
            }
        }

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

        if (box2dDebugOn) {
            box2dMatrix.set(worldCamera.combined);
            box2dMatrix.scl(1f / PhysicsConstants.PPM);
            box2dDebug.render(physics.world, box2dMatrix);
        }
    }

    private void renderOverlay() {
        if (!debugOverlay) return;

        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();

        font.draw(batch, "FPS: " + Gdx.graphics.getFramesPerSecond(), 10, VIRTUAL_H - 10);
        font.draw(batch, "HP: " + player.getHealth().getHp() + "/" + player.getHealth().getMaxHp(), 10, VIRTUAL_H - 28);
        font.draw(batch, "Enemies: " + (meleeEnemies.size + rangedEnemies.size), 10, VIRTUAL_H - 46);
        font.draw(batch, "Relics: " + relics.getOwned().size, 10, VIRTUAL_H - 64);
        font.draw(batch, "Projectile+DMG: +" + relics.getBonusProjectileDamage(), 10, VIRTUAL_H - 82);
        font.draw(batch, "Encounter: " + encounter.getState(), 10, VIRTUAL_H - 100);
        font.draw(batch, "K shoot | J melee | H hitboxes | U hurtboxes | R restart", 10, 20);

        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        worldViewport.update(width, height, true);
        uiCamera.setToOrtho(false, VIRTUAL_W, VIRTUAL_H);
        uiCamera.update();
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        shapes.dispose();
        batch.dispose();
        font.dispose();
        box2dDebug.dispose();
        if (physics != null) physics.dispose();
    }
}
