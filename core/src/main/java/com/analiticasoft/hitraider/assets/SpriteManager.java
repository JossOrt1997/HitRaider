package com.analiticasoft.hitraider.assets;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.function.IntFunction;

/**
 * Central Sprite System:
 * - Loads player/enemy/UI/backgrounds safely
 * - Missing assets => null/fallback
 * - reload() supported
 */
public class SpriteManager {

    private boolean loaded = false;

    // UI
    private Texture legionTex;
    private TextureRegion legionSalamanders;

    // Backgrounds
    private Texture forestBase;
    private Texture forestMid;
    private Texture castleBase;
    private Texture castleMid;

    // World & Combat
    private Texture bullet;
    private Texture platformGround;
    private Texture platformOneway;

    // Screen Backgrounds
    private Texture screenMenu;
    private Texture screenWin;

    // Player & Enemies
    private final PlayerSprites player = new PlayerSprites();
    private final EnemySprites eldarMelee = new EnemySprites();
    private final EnemySprites eldarRanged = new EnemySprites();

    public void load() {
        if (loaded) return;
        loaded = true;

        // UI
        legionTex = SafeAssets.textureOrNull(SpritePaths.LEGION_SALAMANDERS);
        legionSalamanders = (legionTex != null) ? new TextureRegion(legionTex) : null;
        screenMenu = SafeAssets.textureOrNull(SpritePaths.SCREEN_MENU);
        screenWin = SafeAssets.textureOrNull(SpritePaths.SCREEN_WIN);

        // Backgrounds
        forestBase = SafeAssets.textureOrNull(SpritePaths.FOREST_BASE);
        forestMid  = SafeAssets.textureOrNull(SpritePaths.FOREST_MID);
        castleBase = SafeAssets.textureOrNull(SpritePaths.CASTLE_BASE);
        castleMid  = SafeAssets.textureOrNull(SpritePaths.CASTLE_MID);

        // World & Combat
        bullet = SafeAssets.textureOrNull(SpritePaths.BULLET);
        platformGround = SafeAssets.textureOrNull(SpritePaths.PLATFORM_GROUND);
        platformOneway = SafeAssets.textureOrNull(SpritePaths.PLATFORM_ONEWAY);

        // Player sequences
        loadFrames(player.idle, SpritePaths::pIdle, 64);
        loadFrames(player.run, SpritePaths::pRun, 64);
        loadFrames(player.jump, SpritePaths::pJump, 32);
        loadFrames(player.fall, SpritePaths::pFall, 32);
        loadFrames(player.dash, SpritePaths::pDash, 32);
        loadFrames(player.attack, SpritePaths::pAttack, 64);
        loadFrames(player.shoot, SpritePaths::pShoot, 64);
        loadFrames(player.hurt, SpritePaths::pHurt, 32);
        loadFrames(player.dead, SpritePaths::pDead, 32);

        // Enemy melee
        loadFrames(eldarMelee.idle, SpritePaths::emIdle, 64);
        loadFrames(eldarMelee.run, SpritePaths::emRun, 64);
        loadFrames(eldarMelee.telegraph, SpritePaths::emTelegraph, 64);
        loadFrames(eldarMelee.attack, SpritePaths::emAttack, 64);
        loadFrames(eldarMelee.hurt, SpritePaths::emHurt, 32);
        loadFrames(eldarMelee.dead, SpritePaths::emDead, 32);

        // Enemy ranged
        loadFrames(eldarRanged.idle, SpritePaths::erIdle, 64);
        loadFrames(eldarRanged.run, SpritePaths::erRun, 64);
        loadFrames(eldarRanged.telegraph, SpritePaths::erTelegraph, 64);
        loadFrames(eldarRanged.attack, SpritePaths::erShoot, 64);
        loadFrames(eldarRanged.hurt, SpritePaths::erHurt, 32);
        loadFrames(eldarRanged.dead, SpritePaths::erDead, 32);

        // Tuning via VisualTuning
        applyVisualTuning();
    }

    private void applyVisualTuning() {
        var vt = com.analiticasoft.hitraider.config.VisualTuning.class;
        try {
            // Player
            player.scale = vt.getField("PLAYER_BASE_SCALE").getFloat(null);
            player.feetOffsetPx = vt.getField("PLAYER_FEET_OFFSET").getFloat(null);
            player.offIdle = vt.getField("P_OFF_IDLE").getFloat(null);     player.scIdle = vt.getField("P_SC_IDLE").getFloat(null);
            player.offRun = vt.getField("P_OFF_RUN").getFloat(null);       player.scRun = vt.getField("P_SC_RUN").getFloat(null);
            player.offJump = vt.getField("P_OFF_JUMP").getFloat(null);     player.scJump = vt.getField("P_SC_JUMP").getFloat(null);
            player.offFall = vt.getField("P_OFF_FALL").getFloat(null);     player.scFall = vt.getField("P_SC_FALL").getFloat(null);
            player.offDash = vt.getField("P_OFF_DASH").getFloat(null);     player.scDash = vt.getField("P_SC_DASH").getFloat(null);
            player.offAttack = vt.getField("P_OFF_ATTACK").getFloat(null); player.scAttack = vt.getField("P_SC_ATTACK").getFloat(null);
            player.offShoot = vt.getField("P_OFF_SHOOT").getFloat(null);   player.scShoot = vt.getField("P_SC_SHOOT").getFloat(null);
            player.offHurt = vt.getField("P_OFF_HURT").getFloat(null);     player.scHurt = vt.getField("P_SC_HURT").getFloat(null);
            player.offDead = vt.getField("P_OFF_DEAD").getFloat(null);     player.scDead = vt.getField("P_SC_DEAD").getFloat(null);

            player.fpsIdle = vt.getField("FPS_IDLE").getFloat(null);
            player.fpsRun = vt.getField("FPS_RUN").getFloat(null);
            player.fpsAttack = vt.getField("FPS_ATTACK").getFloat(null);
            player.fpsDead = vt.getField("FPS_DEAD").getFloat(null);

            // Melee
            eldarMelee.scale = vt.getField("EM_BASE_SCALE").getFloat(null);
            eldarMelee.feetOffsetPx = vt.getField("EM_FEET_OFFSET").getFloat(null);
            eldarMelee.offIdle = vt.getField("EM_OFF_IDLE").getFloat(null);     eldarMelee.scIdle = vt.getField("EM_SC_IDLE").getFloat(null);
            eldarMelee.offRun = vt.getField("EM_OFF_RUN").getFloat(null);       eldarMelee.scRun = vt.getField("EM_SC_RUN").getFloat(null);
            eldarMelee.offAttack = vt.getField("EM_OFF_ATTACK").getFloat(null); eldarMelee.scAttack = vt.getField("EM_SC_ATTACK").getFloat(null);
            eldarMelee.offHurt = vt.getField("EM_OFF_HURT").getFloat(null);     eldarMelee.scHurt = vt.getField("EM_SC_HURT").getFloat(null);
            eldarMelee.offDead = vt.getField("EM_OFF_DEAD").getFloat(null);     eldarMelee.scDead = vt.getField("EM_SC_DEAD").getFloat(null);
            eldarMelee.fpsDead = vt.getField("FPS_DEAD").getFloat(null);

            // Ranged
            eldarRanged.scale = vt.getField("ER_BASE_SCALE").getFloat(null);
            eldarRanged.feetOffsetPx = vt.getField("ER_FEET_OFFSET").getFloat(null);
            eldarRanged.offIdle = vt.getField("ER_OFF_IDLE").getFloat(null);     eldarRanged.scIdle = vt.getField("ER_SC_IDLE").getFloat(null);
            eldarRanged.offRun = vt.getField("ER_OFF_RUN").getFloat(null);       eldarRanged.scRun = vt.getField("ER_SC_RUN").getFloat(null);
            eldarRanged.offAttack = vt.getField("ER_OFF_ATTACK").getFloat(null); eldarRanged.scAttack = vt.getField("ER_SC_ATTACK").getFloat(null);
            eldarRanged.offHurt = vt.getField("ER_OFF_HURT").getFloat(null);     eldarRanged.scHurt = vt.getField("ER_SC_HURT").getFloat(null);
            eldarRanged.offDead = vt.getField("ER_OFF_DEAD").getFloat(null);     eldarRanged.scDead = vt.getField("ER_SC_DEAD").getFloat(null);
            eldarRanged.fpsDead = vt.getField("FPS_DEAD").getFloat(null);

        } catch (Exception e) {
            com.badlogic.gdx.Gdx.app.error("VISUAL_TUNING", "Failed to apply tuning: " + e.getMessage());
        }
    }

    private void loadFrames(AnimSet set, IntFunction<String> pathFn, int maxFrames) {
        for (int i = 0; i < maxFrames; i++) {
            Texture t = SafeAssets.textureOrNull(pathFn.apply(i));
            if (t == null) break;
            set.add(t);
        }
    }

    // Getters
    public PlayerSprites player() { return player; }
    public EnemySprites eldarMelee() { return eldarMelee; }
    public EnemySprites eldarRanged() { return eldarRanged; }

    public TextureRegion legionSalamanders() { return legionSalamanders; }

    public Texture forestBase() { return forestBase; }
    public Texture forestMid() { return forestMid; }
    public Texture castleBase() { return castleBase; }
    public Texture castleMid() { return castleMid; }
    public Texture bullet() { return bullet; }
    public Texture platformGround() { return platformGround; }
    public Texture platformOneway() { return platformOneway; }
    public Texture screenMenu() { return screenMenu; }
    public Texture screenWin() { return screenWin; }

    public void reload() {
        dispose();
        loaded = false;
        load();
    }

    public void dispose() {
        player.dispose();
        eldarMelee.dispose();
        eldarRanged.dispose();

        if (legionTex != null) legionTex.dispose();
        legionTex = null;
        legionSalamanders = null;

        if (forestBase != null) forestBase.dispose();
        if (forestMid != null) forestMid.dispose();
        if (castleBase != null) castleBase.dispose();
        if (castleMid != null) castleMid.dispose();
        if (bullet != null) bullet.dispose();
        if (platformGround != null) platformGround.dispose();
        if (platformOneway != null) platformOneway.dispose();
        if (screenMenu != null) screenMenu.dispose();
        if (screenWin != null) screenWin.dispose();

        forestBase = null;
        forestMid = null;
        castleBase = null;
        castleMid = null;
        bullet = null;
        platformGround = null;
        platformOneway = null;
        screenMenu = null;
        screenWin = null;
    }
}
