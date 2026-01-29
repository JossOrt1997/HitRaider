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

        // Backgrounds
        forestBase = SafeAssets.textureOrNull(SpritePaths.FOREST_BASE);
        forestMid  = SafeAssets.textureOrNull(SpritePaths.FOREST_MID);

        // Player sequences
        loadFrames(player.idle, SpritePaths::pIdle, 64);
        loadFrames(player.run, SpritePaths::pRun, 64);
        loadFrames(player.jump, SpritePaths::pJump, 32);
        loadFrames(player.fall, SpritePaths::pFall, 32);
        loadFrames(player.dash, SpritePaths::pDash, 32);
        loadFrames(player.attack, SpritePaths::pAttack, 64);
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

        // Tuning defaults
        player.scale = 0.30f;
        player.feetOffsetPx = 18f;

        eldarMelee.scale = 0.28f;
        eldarMelee.feetOffsetPx = 18f;

        eldarRanged.scale = 0.28f;
        eldarRanged.feetOffsetPx = 18f;
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
        forestBase = null;
        forestMid = null;
    }
}
