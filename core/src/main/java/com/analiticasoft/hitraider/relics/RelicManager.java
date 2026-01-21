package com.analiticasoft.hitraider.relics;

import com.badlogic.gdx.utils.Array;

public class RelicManager {

    private final Array<RelicType> owned = new Array<>();

    private int bonusProjectileDamage = 0;
    private float fireRateMultiplier = 1.0f;     // cooldownFinal = base * multiplier
    private float dashCooldownMultiplier = 1.0f; // dashCooldownFinal = base * multiplier
    private int piercingShots = 0;               // 0 or 1 (por ahora)
    private int lifestealEveryHits = 0;          // 0 = off, else heal 1 each N melee hits

    public void add(RelicType type) {
        owned.add(type);
        recompute();
    }

    private void recompute() {
        bonusProjectileDamage = 0;
        fireRateMultiplier = 1.0f;
        dashCooldownMultiplier = 1.0f;
        piercingShots = 0;
        lifestealEveryHits = 0;

        int lifestealStacks = 0;

        for (RelicType r : owned) {
            switch (r) {
                case BONUS_PROJECTILE_DAMAGE -> bonusProjectileDamage += 1;
                case FIRE_RATE_UP -> fireRateMultiplier *= 0.85f;
                case DASH_COOLDOWN_DOWN -> dashCooldownMultiplier *= 0.85f;
                case PIERCING_SHOT -> piercingShots = Math.min(1, piercingShots + 1);
                case MELEE_LIFESTEAL -> lifestealStacks++;
            }
        }

        // clamp (para no romper feel)
        if (fireRateMultiplier < 0.45f) fireRateMultiplier = 0.45f;
        if (dashCooldownMultiplier < 0.45f) dashCooldownMultiplier = 0.45f;

        // Lifesteal: 1 stack => cura cada 8 hits, 2 stacks => cada 6 hits, 3+ => cada 5 hits
        if (lifestealStacks == 1) lifestealEveryHits = 8;
        else if (lifestealStacks == 2) lifestealEveryHits = 6;
        else if (lifestealStacks >= 3) lifestealEveryHits = 5;
    }

    public Array<RelicType> getOwned() { return owned; }

    public int getBonusProjectileDamage() { return bonusProjectileDamage; }
    public float getFireRateMultiplier() { return fireRateMultiplier; }
    public float getDashCooldownMultiplier() { return dashCooldownMultiplier; }
    public int getPiercingShots() { return piercingShots; }
    public int getLifestealEveryHits() { return lifestealEveryHits; }
}
