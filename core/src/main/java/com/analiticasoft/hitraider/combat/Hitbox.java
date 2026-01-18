package com.analiticasoft.hitraider.combat;

import com.badlogic.gdx.physics.box2d.Fixture;
import java.util.HashSet;
import java.util.Set;

/**
 * UserData del Fixture sensor de ataque.
 * Incluye facción y owner para evitar friendly fire y doble-hit.
 */
public class Hitbox {

    public float timeLeft;
    public final int damage;
    public final Faction ownerFaction;
    public final Damageable owner;

    private final Set<Damageable> alreadyHit = new HashSet<>();

    public Hitbox(float durationSeconds, int damage, Faction ownerFaction, Damageable owner) {
        this.timeLeft = durationSeconds;
        this.damage = damage;
        this.ownerFaction = ownerFaction;
        this.owner = owner;
    }

    public boolean canHit(Damageable target) {
        if (target == null) return false;
        if (target == owner) return false;
        return !alreadyHit.contains(target);
    }

    public void markHit(Damageable target) {
        alreadyHit.add(target);
    }

    public static boolean isHitboxFixture(Fixture f) {
        return f != null && f.getUserData() instanceof Hitbox;
    }

    public static Hitbox get(Fixture f) {
        return (Hitbox) f.getUserData();
    }
}
