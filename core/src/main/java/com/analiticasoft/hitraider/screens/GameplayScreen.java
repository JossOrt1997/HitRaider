package com.analiticasoft.hitraider.screens;

import com.analiticasoft.hitraider.combat.weapons.WeaponRuntime;
import com.analiticasoft.hitraider.combat.weapons.WeaponType;
import com.analiticasoft.hitraider.config.GameConfig;
import com.analiticasoft.hitraider.config.ParallaxTuning;
import com.analiticasoft.hitraider.gameplay.GameplayContext;
import com.analiticasoft.hitraider.gameplay.GameplayRuntime;
import com.analiticasoft.hitraider.gameplay.render.UiRenderSystem;
import com.analiticasoft.hitraider.gameplay.render.WorldRenderSystem;
import com.analiticasoft.hitraider.input.Action;
import com.analiticasoft.hitraider.input.DesktopInputProvider;
import com.analiticasoft.hitraider.input.InputState;
import com.analiticasoft.hitraider.render.BackgroundParallax;
import com.analiticasoft.hitraider.world.RoomInstance;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class GameplayScreen implements Screen {

    private final GameplayContext ctx = new GameplayContext();
    private final GameplayRuntime runtime = new GameplayRuntime();
    private final WorldRenderSystem worldRenderer = new WorldRenderSystem();
    private final UiRenderSystem uiRenderer = new UiRenderSystem();

    private OrthographicCamera worldCamera;
    private Viewport worldViewport;
    private OrthographicCamera uiCamera;

    private ShapeRenderer shapes;
    private SpriteBatch batch;
    private BitmapFont font;

    // ✅ Input system (FIX for NPE)
    private final InputState input = new InputState();
    private final DesktopInputProvider inputProvider = new DesktopInputProvider();

    // HUD toggles
    private boolean hudEssentialOn = true;
    private boolean hudInfoOn = true;

    // Debug toggles
    private boolean debugHitboxes = true;
    private boolean debugHurtboxes = true;

    // Weapon HUD runtime (Phase A)
    private WeaponType currentWeapon = WeaponType.THUNDER_HAMMER;
    private WeaponRuntime weaponRuntime;
    private float weaponCooldown = 0f;

    private static final int MAG_SIZE = 30;
    private static final float RELOAD_TIME = 1.10f;
    private int ammoInMag = MAG_SIZE;
    private int ammoReserve = 120;
    private boolean reloading = false;
    private float reloadTimer = 0f;

    // ✅ Used to rebuild parallax after runtime reload clears its own flag
    private boolean backgroundRebuildPending = false;

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

        ctx.worldCamera = worldCamera;
        ctx.uiCamera = uiCamera;

        // Sprites + background
        ctx.sprites.load();
        rebuildBackground();

        // Run init (IMPORTANT: queue wiring happens inside RunController too)
        ctx.run.buildTemplates();
        ctx.run.setDestroyQueue(ctx.destroyQueue);
        ctx.run.startNewRun(true);

        // Ensure projectiles are also linked to queue (belt & suspenders)
        if (ctx.run.projectiles != null) ctx.run.projectiles.setDestroyQueue(ctx.destroyQueue);

        // Transition + door
        ctx.transition.startFadeIn();
        runtime.spawnDoorForCurrentRoom(ctx);
        runtime.syncEnemyTimers(ctx);

        // Runtime init
        runtime.init(ctx);

        // Weapon runtime
        weaponRuntime = new WeaponRuntime(ctx.run.physics.world, ctx.run.combat, ctx.run.projectiles, ctx.run.relics);
        weaponCooldown = 0f;
    }

    @Override
    public void render(float delta) {
        // Poll input FIRST (so runtime/player never receives null input)
        inputProvider.poll(input);

        ctx.frameStats.update(delta);

        // HUD toggles
        if (Gdx.input.isKeyJustPressed(Input.Keys.F1) || Gdx.input.isKeyJustPressed(Input.Keys.TAB)) hudEssentialOn = !hudEssentialOn;
        if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) hudInfoOn = !hudInfoOn;

        // Debug toggles
        if (Gdx.input.isKeyJustPressed(Input.Keys.H)) debugHitboxes = !debugHitboxes;
        if (Gdx.input.isKeyJustPressed(Input.Keys.U)) debugHurtboxes = !debugHurtboxes;

        // Strict toggles
        if (Gdx.input.isKeyJustPressed(Input.Keys.F7)) runtime.toggleStrict(ctx);
        if (Gdx.input.isKeyJustPressed(Input.Keys.F8)) runtime.toggleStrictFreeze(ctx);
        if (Gdx.input.isKeyJustPressed(Input.Keys.F9)) runtime.unfreeze(ctx);

        // Deferred reload/restart
        if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) {
            runtime.requestReload(ctx);
            backgroundRebuildPending = true; // ✅ we rebuild after tick
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) runtime.requestRestart(ctx);

        // Snapshot manual
        if (Gdx.input.isKeyJustPressed(Input.Keys.F6)) {
            Gdx.app.log("SNAPSHOT", buildSnapshotString("manual"));
        }

        // Weapon switching
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) currentWeapon = WeaponType.THUNDER_HAMMER;
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) currentWeapon = WeaponType.BOLTER;

        // Reload manual
        if (Gdx.input.isKeyJustPressed(Input.Keys.L)) startReload();

        // cooldown timers
        if (weaponCooldown > 0f) weaponCooldown = Math.max(0f, weaponCooldown - delta);

        // reload timer
        if (reloading) {
            reloadTimer = Math.max(0f, reloadTimer - delta);
            if (reloadTimer <= 0f) finishReload();
        }

        // ✅ Runtime tick with real input (FIX)
        runtime.tick(ctx, input, delta);

        // Weapon use (only blocks shooting during reload)
        handleWeaponUse(delta);

        // If runtime processed reload this frame, rebuild background safely once
        if (backgroundRebuildPending && !ctx.reloadRequested) {
            rebuildBackground();
            backgroundRebuildPending = false;
        }

        // Clear
        Gdx.gl.glClearColor(0.92f, 0.93f, 0.95f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        worldViewport.apply();

        // Render world + UI
        worldRenderer.render(ctx, shapes, batch, debugHitboxes, debugHurtboxes);

        if (hudEssentialOn || hudInfoOn) {
            uiRenderer.renderUI(
                ctx, shapes, batch, font,
                hudEssentialOn, hudInfoOn,
                weaponCooldown,
                weaponName(currentWeapon),
                ammoLabel(),
                ctx.strictFreezeOnFail
            );
        }

        uiRenderer.renderCriticalOverlay(ctx, batch, font);

        input.endFrame();
    }

    private void handleWeaponUse(float delta) {
        if (ctx.run.player == null || !ctx.run.player.isAlive()) return;

        // Only bolter is executed here (ranged).
        // Hammer is handled by Player.attack -> CombatSystem hitbox spawn in runtime (stable).
        if (currentWeapon != WeaponType.BOLTER) return;

        if (reloading) return;

        // Auto-reload on empty if trying to shoot
        if (input.isJustPressed(Action.SHOOT) && ammoInMag <= 0) {
            startReload();
            return;
        }

        if (input.isJustPressed(Action.SHOOT) && weaponCooldown <= 0f) {
            if (ammoInMag <= 0) return;

            ammoInMag--;
            int aimY = ctx.run.player.getAimY(input);
            weaponRuntime.usePrimary(ctx.run.player, currentWeapon, aimY);
            weaponCooldown = weaponRuntime.cooldownFor(currentWeapon);
        }
    }

    private void startReload() {
        if (currentWeapon != WeaponType.BOLTER) return;
        if (reloading) return;
        if (ammoInMag >= MAG_SIZE) return;
        if (ammoReserve <= 0) return;

        reloading = true;
        reloadTimer = RELOAD_TIME;
    }

    private void finishReload() {
        reloading = false;

        int need = MAG_SIZE - ammoInMag;
        if (need <= 0) return;

        int taken = Math.min(need, ammoReserve);
        ammoInMag += taken;
        ammoReserve -= taken;
    }

    private String ammoLabel() {
        if (currentWeapon != WeaponType.BOLTER) return "Ammo: --/--";
        String line = "Ammo: " + ammoInMag + "/" + ammoReserve;
        if (reloading) line += String.format("  (Reloading %.1fs)", reloadTimer);
        return line;
    }

    private String weaponName(WeaponType t) {
        return switch (t) {
            case THUNDER_HAMMER -> "Thunder Hammer";
            case BOLTER -> "Bolter";
        };
    }

    private void rebuildBackground() {
        ctx.background = new BackgroundParallax(
            ctx.sprites.forestBase(),
            ctx.sprites.forestMid(),
            ParallaxTuning.BASE_FACTOR,
            ParallaxTuning.MID_FACTOR,
            GameConfig.VIRTUAL_H
        );
    }

    private String buildSnapshotString(String phase) {
        try {
            RoomInstance r = ctx.run.run.current();
            return "phase=" + phase +
                " seed=" + ctx.run.run.seed +
                " room=" + (ctx.run.run.index + 1) + "/" + ctx.run.run.totalRooms +
                " type=" + r.type +
                " tpl=" + r.template.id +
                " hp=" + ctx.run.player.getHealth().getHp() + "/" + ctx.run.player.getHealth().getMaxHp() +
                " pState=" + ctx.run.player.getState() +
                " melee=" + ctx.run.meleeEnemies.size +
                " ranged=" + ctx.run.rangedEnemies.size +
                " proj=" + ctx.run.projectiles.projectiles.size +
                " pickups=" + ctx.run.pickups.size +
                " doorClosed=" + ctx.doorClosed;
        } catch (Throwable t) {
            return "phase=" + phase + " (snapshot failed: " + t.getMessage() + ")";
        }
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
        ctx.sprites.dispose();
        shapes.dispose();
        batch.dispose();
        font.dispose();
        if (ctx.run.physics != null) ctx.run.physics.dispose();
    }
}
