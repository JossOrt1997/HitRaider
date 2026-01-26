package com.analiticasoft.hitraider.config;

public final class ShakeTuning {
    private ShakeTuning() {}

    // Player hurt (strongest)
    public static final float PLAYER_HURT_DUR = 0.10f;
    public static final float PLAYER_HURT_INT = 4.5f;

    // Enemy hurt (medium)
    public static final float ENEMY_HURT_DUR = 0.08f;
    public static final float ENEMY_HURT_INT = 2.5f;

    // Melee hits world (medium)
    public static final float MELEE_WORLD_DUR = 0.08f;
    public static final float MELEE_WORLD_INT = 2.5f;

    // Projectiles hit enemy (lighter)
    public static final float PROJ_HIT_ENEMY_DUR = 0.06f;
    public static final float PROJ_HIT_ENEMY_INT = 1.8f;

    // Projectiles hit world (lightest)
    public static final float PROJ_HIT_WORLD_DUR = 0.05f;
    public static final float PROJ_HIT_WORLD_INT = 0.9f;
}
