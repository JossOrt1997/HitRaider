package com.analiticasoft.hitraider.config;

public final class PlayerTuning {
    private PlayerTuning() {}

    // Combat base
    public static final int BASE_MELEE_DAMAGE = 1;
    public static final int BASE_PROJECTILE_DAMAGE = 1;

    // Shooting
    public static final float SHOOT_BASE_COOLDOWN = 0.28f;

    // Projectile spawn offsets (pixels)
    public static final float PROJECTILE_SPAWN_OFFSET_X = 14f;
    public static final float PROJECTILE_SPAWN_OFFSET_Y = 10f;

    // Projectile travel (m/s) and lifetime (seconds)
    public static final float PROJECTILE_SPEED = 8.5f;
    public static final float PROJECTILE_LIFETIME = 1.2f;
}
