package com.analiticasoft.hitraider.physics;

import com.analiticasoft.hitraider.combat.CombatSystem;
import com.analiticasoft.hitraider.combat.Damageable;
import com.analiticasoft.hitraider.combat.Projectile;
import com.analiticasoft.hitraider.combat.Hitbox;
import com.analiticasoft.hitraider.entities.Player;
import com.analiticasoft.hitraider.relics.RelicPickup;
import com.badlogic.gdx.physics.box2d.*;

public class GameContactListener implements ContactListener {

    private final CombatSystem combat;

    public GameContactListener(CombatSystem combat) {
        this.combat = combat;
    }

    @Override
    public void beginContact(Contact contact) {
        Fixture a = contact.getFixtureA();
        Fixture b = contact.getFixtureB();

        // grounded
        if (isGroundSensor(a) && isGroundLike(b)) incGroundContacts(a);
        if (isGroundSensor(b) && isGroundLike(a)) incGroundContacts(b);

        // melee hitbox routing
        if (Hitbox.isHitboxFixture(a)) combat.handleHitboxContact(a, b);
        if (Hitbox.isHitboxFixture(b)) combat.handleHitboxContact(b, a);

        // projectile routing
        handleProjectileContact(a, b);
        handleProjectileContact(b, a);

        // relic pickup routing
        handleRelicPickup(a, b);
        handleRelicPickup(b, a);
    }

    @Override
    public void endContact(Contact contact) {
        Fixture a = contact.getFixtureA();
        Fixture b = contact.getFixtureB();

        if (isGroundSensor(a) && isGroundLike(b)) decGroundContacts(a);
        if (isGroundSensor(b) && isGroundLike(a)) decGroundContacts(b);
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {
        // One-way platforms (player only)
        Fixture a = contact.getFixtureA();
        Fixture b = contact.getFixtureB();

        Fixture playerFix = null;
        Fixture onewayFix = null;

        if (isPlayerFixture(a) && isOneWay(b)) { playerFix = a; onewayFix = b; }
        else if (isPlayerFixture(b) && isOneWay(a)) { playerFix = b; onewayFix = a; }

        if (playerFix == null) return;

        Body playerBody = playerFix.getBody();
        Body platformBody = onewayFix.getBody();

        float margin = PhysicsConstants.toMeters(18f);
        float playerY = playerBody.getPosition().y;
        float platformY = platformBody.getPosition().y;

        if (playerY < platformY + margin) {
            contact.setEnabled(false);
        }
    }

    @Override public void postSolve(Contact contact, ContactImpulse impulse) {}

    private void handleProjectileContact(Fixture projFix, Fixture otherFix) {
        if (projFix == null || otherFix == null) return;
        Object pud = projFix.getUserData();
        if (!(pud instanceof Projectile p)) return;

        Object oud = otherFix.getUserData();
        if (!(oud instanceof Damageable target)) return;

        // friendly fire off
        if (target.getFaction() == p.faction) return;

        // apply damage and kill projectile
        target.applyDamage(p.damage, new com.badlogic.gdx.math.Vector2(0, 0));
        p.dead = true;
    }

    private void handleRelicPickup(Fixture pickupFix, Fixture otherFix) {
        if (pickupFix == null || otherFix == null) return;
        Object pud = pickupFix.getUserData();
        if (!(pud instanceof RelicPickup pickup)) return;

        Object oud = otherFix.getUserData();
        if (!(oud instanceof Player)) return;

        pickup.collected = true;
    }

    private boolean isGroundSensor(Fixture f) { return f != null && "player_ground_sensor".equals(f.getUserData()); }
    private boolean isPlayerFixture(Fixture f) {
        if (f == null) return false;
        Object ud = f.getUserData();
        return (ud instanceof Player) || "player".equals(ud);
    }
    private boolean isGround(Fixture f) { return f != null && "ground".equals(f.getUserData()); }
    private boolean isOneWay(Fixture f) { return f != null && "oneway".equals(f.getUserData()); }
    private boolean isGroundLike(Fixture f) { return isGround(f) || isOneWay(f); }

    private void incGroundContacts(Fixture sensor) {
        Object ud = sensor.getBody().getUserData();
        if (ud instanceof GroundContactCounter gcc) gcc.contacts++;
    }
    private void decGroundContacts(Fixture sensor) {
        Object ud = sensor.getBody().getUserData();
        if (ud instanceof GroundContactCounter gcc) gcc.contacts = Math.max(0, gcc.contacts - 1);
    }

    public static class GroundContactCounter {
        public int contacts = 0;
        public boolean grounded() { return contacts > 0; }
    }
}
