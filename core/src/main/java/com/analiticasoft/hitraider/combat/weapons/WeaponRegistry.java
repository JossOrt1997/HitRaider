package com.analiticasoft.hitraider.combat.weapons;

import java.util.EnumMap;

/**
 * WeaponRegistry:
 * - One source of truth for weapons (Phase A: 2 weapons)
 * - Add new weapons by registering a WeaponDefinition here
 */
public final class WeaponRegistry {

    private static final EnumMap<WeaponType, WeaponDefinition> defs = new EnumMap<>(WeaponType.class);

    static {
        // Thunder Hammer (melee)
        register(WeaponDefinition.builder(WeaponType.THUNDER_HAMMER)
            .melee(true)
            .baseDamage(2)
            .cooldownSec(0.25f)
            .meleeDurationSec(0.10f)
            .build());

        // Bolter (ranged)
        register(WeaponDefinition.builder(WeaponType.BOLTER)
            .melee(false)
            .baseDamage(1)
            .cooldownSec(0.18f)
            .projectileSpeedMps(8.5f)
            .projectileLifetimeSec(1.2f)
            .basePierce(0)
            .build());
    }

    private WeaponRegistry() {}

    private static void register(WeaponDefinition def) {
        defs.put(def.type, def);
    }

    public static WeaponDefinition get(WeaponType type) {
        return defs.get(type);
    }
}
