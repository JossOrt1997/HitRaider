package com.analiticasoft.hitraider.world;

import com.analiticasoft.hitraider.relics.RelicType;

import java.util.Random;

/**
 * Centralized relic roll rules.
 * Keep deterministic (pass Random with known seed).
 */
public class DropRules {

    public RelicType rollRelic(Random rng) {
        // Simple weighted example (Phase A)
        float r = rng.nextFloat();

        if (r < 0.45f) return RelicType.BONUS_PROJECTILE_DAMAGE;
        if (r < 0.75f) return RelicType.FIRE_RATE_UP;
        return RelicType.MELEE_LIFESTEAL;
    }
}
