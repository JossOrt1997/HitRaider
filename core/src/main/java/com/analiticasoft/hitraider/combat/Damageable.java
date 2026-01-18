package com.analiticasoft.hitraider.combat;

import com.badlogic.gdx.math.Vector2;

public interface Damageable {
    Faction getFaction();
    void applyDamage(int amount, Vector2 knockback);
    boolean isAlive();
    HealthComponent getHealth();
}
