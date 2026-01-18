package com.analiticasoft.hitraider.screens;

import com.analiticasoft.hitraider.combat.CombatSystem;
import com.analiticasoft.hitraider.entities.MeleeEnemy;
import com.analiticasoft.hitraider.entities.Player;
import com.analiticasoft.hitraider.input.DesktopInputProvider;
import com.analiticasoft.hitraider.input.InputState;
import com.analiticasoft.hitraider.physics.GameContactListener;
import com.analiticasoft.hitraider.physics.PhysicsConstants;
import com.analiticasoft.hitraider.physics.PhysicsWorld;
import com.analiticasoft.hitraider.world.EncounterManager;
import com.analiticasoft.hitraider.world.LevelFactory;
import com.analiticasoft.hitraider.render.AnimKey;
import com.analiticasoft.hitraider.render.CharacterAnimator;
import com.analiticasoft.hitraider.render.DebugAnimLibrary;
import com.analiticasoft.hitraider.render.DebugCharacterRenderer;
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
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

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

    private final InputState input = new InputState();
    private final DesktopInputProvider inputProvider = new DesktopInputProvider();

    private PhysicsWorld physics;
    private CombatSystem combat;
    private GameContactListener contactListener;

    private Box2DDebugRenderer box2dDebug;
    private final Matrix4 box2dMatrix = new Matrix4();

    private Player player;

    private Array<LevelFactory.PlatformRect> platformRects;

    private final Array<MeleeEnemy> enemies = new Array<>();
    private final Array<CharacterAnimator> enemyAnims = new Array<>();

    private float hitstopTimer = 0f;
    private final EncounterManager encounter = new EncounterManager();

    // Door / barrier
    private Body doorBody;
    private boolean doorClosed = false;

    private static final float DOOR_X = 1180f;
    private static final float DOOR_Y = 120f;
    private static final float DOOR_W = 28f;
    private static final float DOOR_H = 220f;

    // Debug “animation frames”
    private final DebugCharacterRenderer debugCharRenderer = new DebugCharacterRenderer();
    private CharacterAnimator playerAnim;

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

        // Define debug animations
        playerAnim = new CharacterAnimator();
        DebugAnimLibrary.definePlayer(playerAnim);

        initWorld();

        worldCamera.position.set(VIRTUAL_W / 2f, VIRTUAL_H / 2f, 0f);
        worldCamera.update();
    }

    private void initWorld() {
        if (physics != null) physics.dispose();

        enemies.clear();
        enemyAnims.clear();

        doorBody = null;
        doorClosed = false;

        physics = new PhysicsWorld(new Vector2(0f, -25f));

        combat = new CombatSystem(physics.world);
        contactListener = new GameContactListener(combat);
        physics.world.setContactListener(contactListener);

        platformRects = LevelFactory.createTestLevel(physics.world);

        player = new Player(physics.world, 120f, 140f);

        // Spawn melee enemies + animator per enemy
        spawnMeleeEnemy(520f, 220f);
        spawnMeleeEnemy(820f, 220f);
        spawnMeleeEnemy(650f, 220f);

        encounter.reset();
        hitstopTimer = 0f;

        closeDoor();
    }

    private void spawnMeleeEnemy(float xPx, float yPx) {
        enemies.add(new MeleeEnemy(physics.world, xPx, yPx));
        CharacterAnimator ea = new CharacterAnimator();
        DebugAnimLibrary.defineMeleeEnemy(ea);
        enemyAnims.add(ea);
    }

    private void closeDoor() {
        if (doorBody != null) return;

        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.StaticBody;
        bd.position.set(
            PhysicsConstants.toMeters(DOOR_X),
            PhysicsConstants.toMeters(DOOR_Y)
        );

        doorBody = physics.world.createBody(bd);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(
            PhysicsConstants.toMeters(DOOR_W / 2f),
            PhysicsConstants.toMeters(DOOR_H / 2f)
        );

        FixtureDef fd = new FixtureDef();
        fd.shape = shape;
        fd.friction = 0.8f;
        fd.restitution = 0f;

        Fixture fx = doorBody.createFixture(fd);
        fx.setUserData("ground");

        shape.dispose();

        doorClosed = true;
    }

    private void openDoor() {
        if (doorBody == null) return;

        // Safety purge (in case any system tracked fixtures for this body)
        combat.purgeForBody(doorBody);

        physics.world.destroyBody(doorBody);
        doorBody = null;
        doorClosed = false;
    }

    @Override
    public void render(float delta) {
        inputProvider.poll(input);

        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) debugOverlay = !debugOverlay;
        if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) debugWorld = !debugWorld;
        if (Gdx.input.isKeyJustPressed(Input.Keys.B)) box2dDebugOn = !box2dDebugOn;

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

        // Player update
        player.update(delta, input);

        // Spawn player attack hitbox (use your current Player implementation trigger)
        // If your Player uses shouldSpawnAttackHitboxThisFrame(), prefer that.
        // Here we keep compatibility with didStartAttackThisFrame() if you still have it.
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

        // Enemies update + attacks + cleanup
        for (int i = enemies.size - 1; i >= 0; i--) {
            MeleeEnemy e = enemies.get(i);
            e.update(delta, player);

            CharacterAnimator ea = enemyAnims.get(i);
            ea.set(mapEnemyAnim(e));
            ea.update(delta);

            if (e.didStartAttackThisFrame()) {
                combat.spawnMeleeHitbox(
                    e.body,
                    e,
                    e.getFaction(),
                    e.getFacingDir(),
                    0,
                    e.getDamage()
                );
            }

            if (!e.isAlive()) {
                combat.purgeForBody(e.body);
                physics.world.destroyBody(e.body);
                enemies.removeIndex(i);
                enemyAnims.removeIndex(i);
            }
        }

        // Player animator update
        playerAnim.set(mapPlayerAnim());
        playerAnim.update(delta);

        combat.update(delta);

        physics.step(delta);

        if (combat.hitThisFrame()) hitstopTimer = 0.05f;

        // Encounter + door
        encounter.update(delta, enemies.size);
        if (encounter.getState() == EncounterManager.State.FIGHT) closeDoor();
        else openDoor();

        // Camera follow
        float px = player.getXpx();
        float py = player.getYpx();
        worldCamera.position.x += (px - worldCamera.position.x) * 0.12f;
        worldCamera.position.y += ((py + 40f) - worldCamera.position.y) * 0.10f;
        worldCamera.update();
    }

    private AnimKey mapPlayerAnim() {
        return switch (player.getState()) {
            case RUN -> AnimKey.RUN;
            case JUMP -> AnimKey.JUMP;
            case FALL -> AnimKey.FALL;
            case DASH -> AnimKey.DASH;
            case ATTACK -> AnimKey.ATTACK_ACTIVE; // simplified preview
            case HURT -> AnimKey.HURT;
            default -> AnimKey.IDLE;
        };
    }

    private AnimKey mapEnemyAnim(MeleeEnemy e) {
        return switch (e.getState()) {
            case CHASE -> AnimKey.ENEMY_CHASE;
            case TELEGRAPH -> AnimKey.ENEMY_TELEGRAPH;
            case ATTACK -> AnimKey.ENEMY_ATTACK;
            case STAGGER -> AnimKey.ENEMY_STAGGER;
            default -> AnimKey.ENEMY_IDLE;
        };
    }

    private void renderWorld() {
        shapes.setProjectionMatrix(worldCamera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        // Platforms
        if (platformRects != null) {
            for (LevelFactory.PlatformRect p : platformRects) {
                if ("oneway".equals(p.type)) shapes.setColor(0.35f, 0.35f, 0.38f, 1f);
                else shapes.setColor(0.20f, 0.20f, 0.22f, 1f);
                shapes.rect(p.cx - p.w / 2f, p.cy - p.h / 2f, p.w, p.h);
            }
        }

        // Door visual
        if (doorClosed) {
            shapes.setColor(0.08f, 0.08f, 0.09f, 1f);
            shapes.rect(DOOR_X - DOOR_W / 2f, DOOR_Y - DOOR_H / 2f, DOOR_W, DOOR_H);
        }

        // Enemies (frame-based)
        for (int i = 0; i < enemies.size; i++) {
            MeleeEnemy e = enemies.get(i);
            CharacterAnimator ea = enemyAnims.get(i);

            float ex = e.getXpx();
            float ey = e.getYpx() - 14f; // pivot at "feet" approx

            if (e.isFlashing()) {
                shapes.setColor(0.05f, 0.05f, 0.05f, 1f);
            } else if (e.getState() == MeleeEnemy.State.TELEGRAPH) {
                // lighten during telegraph
                float a = e.getTelegraphAlpha();
                float base = 0.14f;
                float glow = 0.40f * a;
                float c = base + glow;
                shapes.setColor(c, c, c, 1f);
            } else {
                shapes.setColor(0.12f, 0.12f, 0.16f, 1f);
            }

            debugCharRenderer.draw(shapes, ex, ey, e.getFacingDir(), ea.getFrame());
        }

        // Player (frame-based)
        float px = player.getXpx();
        float py = player.getYpx() - 16f;

        if (player.isFlashing()) shapes.setColor(0.02f, 0.02f, 0.02f, 1f);
        else shapes.setColor(0.10f, 0.10f, 0.12f, 1f);

        debugCharRenderer.draw(shapes, px, py, player.getFacingDir(), playerAnim.getFrame());

        shapes.end();

        // Outlines
        if (debugWorld && platformRects != null) {
            shapes.begin(ShapeRenderer.ShapeType.Line);
            shapes.setColor(0.05f, 0.05f, 0.06f, 1f);
            for (LevelFactory.PlatformRect p : platformRects) {
                shapes.rect(p.cx - p.w / 2f, p.cy - p.h / 2f, p.w, p.h);
            }
            if (doorClosed) {
                shapes.rect(DOOR_X - DOOR_W / 2f, DOOR_Y - DOOR_H / 2f, DOOR_W, DOOR_H);
            }
            shapes.end();
        }

        // Box2D debug
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
        font.draw(batch, "Player HP: " + player.getHealth().getHp() + "/" + player.getHealth().getMaxHp(), 10, VIRTUAL_H - 28);
        font.draw(batch, "Enemies: " + enemies.size, 10, VIRTUAL_H - 46);
        font.draw(batch, "Encounter: " + encounter.getState(), 10, VIRTUAL_H - 64);

        font.draw(batch, "PAnim: " + playerAnim.getCurrent() + " frame " + playerAnim.getFrameIndex(), 10, VIRTUAL_H - 82);

        if (!player.isAlive()) {
            font.draw(batch, "YOU DIED - Press R to restart", 180, 200);
        } else if (encounter.shouldShowClearMessage()) {
            font.draw(batch, "CLEAR!", 300, 200);
        } else if (doorClosed) {
            font.draw(batch, "DOOR LOCKED", 280, 200);
        }

        font.draw(batch, "F1 Overlay | F2 Outlines | B Box2D | R Restart", 10, 20);

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
