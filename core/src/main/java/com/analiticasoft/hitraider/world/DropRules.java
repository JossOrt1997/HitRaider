package com.analiticasoft.hitraider.world;

import com.analiticasoft.hitraider.relics.RelicType;

import java.util.Random;

/**
 * Semana 6 demo-ready:
 * - Max 1 reliquia por sala
 * - Anti-repetición (reroll 1 vez)
 * - Pool de 2 reliquias (extensible)
 */
public class DropRules {

    private RelicType lastDrop = null;

    public RelicType rollRelic(Random rng) {
        RelicType t = rng.nextBoolean() ? RelicType.BONUS_PROJECTILE_DAMAGE : RelicType.FIRE_RATE_UP;

        if (lastDrop != null && t == lastDrop) {
            t = (t == RelicType.BONUS_PROJECTILE_DAMAGE) ? RelicType.FIRE_RATE_UP : RelicType.BONUS_PROJECTILE_DAMAGE;
        }

        lastDrop = t;
        return t;
    }
}
