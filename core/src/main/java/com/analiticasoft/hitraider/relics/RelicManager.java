package com.analiticasoft.hitraider.relics;

import com.badlogic.gdx.utils.Array;

public class RelicManager {

    private final Array<RelicType> owned = new Array<>();

    private int bonusProjectileDamage = 0;

    public void add(RelicType type) {
        owned.add(type);
        recompute();
    }

    private void recompute() {
        bonusProjectileDamage = 0;
        for (RelicType r : owned) {
            if (r == RelicType.BONUS_PROJECTILE_DAMAGE) bonusProjectileDamage += 1; // +1 por reliquia
        }
    }

    public int getBonusProjectileDamage() {
        return bonusProjectileDamage;
    }

    public Array<RelicType> getOwned() {
        return owned;
    }
}
