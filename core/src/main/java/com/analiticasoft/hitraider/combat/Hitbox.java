package com.analiticasoft.hitraider.combat;

import com.badlogic.gdx.physics.box2d.Fixture;

import java.util.HashSet;
import java.util.Set;

/**
 * UserData for melee hitbox fixtures.
 * Tracks timeLeft + damage + faction + owner + already-hit targets.
 */
public class Hitbox {

    public float timeLeft;
    public final int damage;
    public final Faction ownerFaction;
    public final Damageable owner;

    private final Set<Damageable> hit = new HashSet<>();

    public Hitbox(float duration, int damage, Faction ownerFaction, Damageable owner) {
        this.timeLeft = duration;
        this.damage = damage;
        this.ownerFaction = ownerFaction;
        this.owner = owner;
    }

    public boolean canHit(Damageable target) {
        return !hit.contains(target);
    }

    public void markHit(Damageable target) {
        hit.add(target);
    }

    public static boolean isHitboxFixture(Fixture f) {
        if (f == null) return false;
        return f.getUserData() instanceof Hitbox;
    }

    public static Hitbox get(Fixture f) {
        return (Hitbox) f.getUserData();
    }
}
