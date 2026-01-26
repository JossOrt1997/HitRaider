package com.analiticasoft.hitraider.assets;

public final class SpritePaths {
    private SpritePaths() {}

    // Roots (your runtime resolves "assets/sprites/..." => path starts with "sprites/")
    public static final String ROOT = "sprites/";
    public static final String PLAYER = ROOT + "player/salamander/";
    public static final String ENEMIES = ROOT + "enemies/";
    public static final String WEAPONS = ROOT + "weapons/";
    public static final String UI = ROOT + "ui/";
    public static final String BG = ROOT + "backgrounds/";

    // UI
    public static final String LEGION_SALAMANDERS = UI + "legions/salamanders.png";

    // Backgrounds (forest/castle)
    public static final String FOREST_BASE = BG + "forest/forest_bg_base.png";
    public static final String FOREST_MID  = BG + "forest/forest_bg_mid.png";
    public static final String CASTLE_BASE = BG + "castle/castle_bg_base.png";
    public static final String CASTLE_MID  = BG + "castle/castle_bg_mid.png";

    // Player frames
    public static String pIdle(int i)   { return PLAYER + "idle/idle_" + f(i) + ".png"; }
    public static String pRun(int i)    { return PLAYER + "run/run_" + f(i) + ".png"; }
    public static String pJump(int i)   { return PLAYER + "jump/jump_" + f(i) + ".png"; }
    public static String pFall(int i)   { return PLAYER + "fall/fall_" + f(i) + ".png"; }
    public static String pDash(int i)   { return PLAYER + "dash/dash_" + f(i) + ".png"; }
    public static String pAttack(int i) { return PLAYER + "attack/attack_" + f(i) + ".png"; }
    public static String pHurt(int i)   { return PLAYER + "hurt/hurt_" + f(i) + ".png"; }
    public static String pDead(int i)   { return PLAYER + "dead/dead_" + f(i) + ".png"; }

    // Enemies
    public static final String ELDAR_MELEE = ENEMIES + "eldar_melee/";
    public static String emIdle(int i)      { return ELDAR_MELEE + "idle/idle_" + f(i) + ".png"; }
    public static String emRun(int i)       { return ELDAR_MELEE + "run/run_" + f(i) + ".png"; }
    public static String emTelegraph(int i) { return ELDAR_MELEE + "telegraph/telegraph_" + f(i) + ".png"; }
    public static String emAttack(int i)    { return ELDAR_MELEE + "attack/attack_" + f(i) + ".png"; }
    public static String emHurt(int i)      { return ELDAR_MELEE + "hurt/hurt_" + f(i) + ".png"; }
    public static String emDead(int i)      { return ELDAR_MELEE + "dead/dead_" + f(i) + ".png"; }

    public static final String ELDAR_RANGED = ENEMIES + "eldar_ranged/";
    public static String erIdle(int i)      { return ELDAR_RANGED + "idle/idle_" + f(i) + ".png"; }
    public static String erRun(int i)       { return ELDAR_RANGED + "run/run_" + f(i) + ".png"; }
    public static String erTelegraph(int i) { return ELDAR_RANGED + "telegraph/telegraph_" + f(i) + ".png"; }
    public static String erShoot(int i)     { return ELDAR_RANGED + "shoot/shoot_" + f(i) + ".png"; }
    public static String erHurt(int i)      { return ELDAR_RANGED + "hurt/hurt_" + f(i) + ".png"; }
    public static String erDead(int i)      { return ELDAR_RANGED + "dead/dead_" + f(i) + ".png"; }

    // Weapons (optional)
    public static String wIcon(String weaponFolder) { return WEAPONS + weaponFolder + "/icon.png"; }
    public static String wIdle(String weaponFolder, int i)  { return WEAPONS + weaponFolder + "/idle/idle_" + f(i) + ".png"; }
    public static String wShoot(String weaponFolder, int i) { return WEAPONS + weaponFolder + "/shoot/shoot_" + f(i) + ".png"; }
    public static String wSwing(String weaponFolder, int i) { return WEAPONS + weaponFolder + "/swing/swing_" + f(i) + ".png"; }

    private static String f(int i) {
        if (i < 0) i = 0;
        if (i > 999) i = 999;
        return String.format("%03d", i);
    }
}
