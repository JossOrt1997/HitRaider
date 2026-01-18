package com.analiticasoft.hitraider.combat;

public final class AttackProfiles {
    private AttackProfiles() {}

    // Ajustables con tu debug hitboxes (H/U)
    public static final AttackProfile PLAYER_MELEE = new AttackProfile(
        0.10f, // duration
        1,     // damage
        13f,   // halfW px
        9f,    // halfH px
        18f,   // offsetX neutral
        0f,    // offsetY neutral
        12f,   // offsetX when aiming (up/down)
        16f,   // aim up offsetY
        -14f   // aim down offsetY
    );

    public static final AttackProfile ENEMY_MELEE = new AttackProfile(
        0.08f, // duration
        1,     // damage
        12f,   // halfW
        8f,    // halfH
        16f,   // offsetX
        0f,    // offsetY
        10f,   // aim offsetX (no se usa en enemy ahora)
        14f,
        -12f
    );
}
