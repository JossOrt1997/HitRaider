package com.analiticasoft.hitraider.physics;

public final class CollisionBits {
    private CollisionBits() {}

    // Categories (power of two)
    public static final short WORLD      = 0x0001; // ground/oneway/door/static
    public static final short PLAYER     = 0x0002; // player hurtbox
    public static final short ENEMY      = 0x0004; // enemy hurtbox
    public static final short HITBOX     = 0x0008; // melee hitbox sensors
    public static final short PROJECTILE = 0x0010; // bullets sensors
    public static final short PICKUP     = 0x0020; // relic pickups sensors
    public static final short SENSOR     = 0x0040; // foot sensors / triggers

    // Masks
    // Include SENSOR so ground sensors can detect WORLD fixtures.
    public static final short MASK_WORLD_SOLID  = (short)(PLAYER | ENEMY | PROJECTILE | SENSOR);

    public static final short MASK_PLAYER_BODY = (short)(WORLD | ENEMY | PROJECTILE);
    public static final short MASK_ENEMY_BODY  = (short)(WORLD | PLAYER | PROJECTILE | HITBOX);

    public static final short MASK_HITBOX      = (short)(PLAYER | ENEMY);
    public static final short MASK_PROJECTILE  = (short)(WORLD | PLAYER | ENEMY);
    public static final short MASK_PICKUP      = (short)(PLAYER);
}
