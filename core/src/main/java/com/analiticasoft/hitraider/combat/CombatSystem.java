package com.analiticasoft.hitraider.combat;

import com.analiticasoft.hitraider.physics.PhysicsConstants;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CombatSystem {

    private final World world;

    private static final float DEFAULT_MELEE_DURATION = 0.10f;

    private static final float KNOCKBACK_X = 3.0f;
    private static final float KNOCKBACK_Y = 2.0f;

    private static final float ATTACKER_RECOIL_X = 1.2f;
    private static final float ATTACKER_RECOIL_Y = 0.2f;

    private static class ActiveHitbox {
        final Body ownerBody;
        final Fixture fixture;
        final Hitbox hitbox;

        ActiveHitbox(Body ownerBody, Fixture fixture, Hitbox hitbox) {
            this.ownerBody = ownerBody;
            this.fixture = fixture;
            this.hitbox = hitbox;
        }
    }

    private final List<ActiveHitbox> active = new ArrayList<>();
    private boolean hitThisFrame = false;

    public CombatSystem(World world) {
        this.world = world;
    }

    public void beginFrame() {
        hitThisFrame = false;
    }

    public boolean hitThisFrame() {
        return hitThisFrame;
    }

    /** Para debug: fixtures de hitbox actualmente activas (solo lectura) */
    public List<Fixture> getActiveHitboxFixtures() {
        List<Fixture> out = new ArrayList<>(active.size());
        for (ActiveHitbox ah : active) out.add(ah.fixture);
        return out;
    }

    /** Llamar antes de destruir un Body para evitar crashes por fixtures activas */
    public void purgeForBody(Body body) {
        if (body == null) return;

        Iterator<ActiveHitbox> it = active.iterator();
        while (it.hasNext()) {
            ActiveHitbox ah = it.next();
            if (ah.ownerBody == body) {
                safeDestroyFixture(body, ah.fixture);
                it.remove();
            }
        }
    }

    public void spawnMeleeHitbox(Body ownerBody,
                                 Damageable owner,
                                 Faction ownerFaction,
                                 int facingDir,
                                 int aimY,
                                 int damage) {

        for (ActiveHitbox ah : active) {
            if (ah.ownerBody == ownerBody) return;
        }

        float halfW = PhysicsConstants.toMeters(13f);
        float halfH = PhysicsConstants.toMeters(9f);

        float offsetX = PhysicsConstants.toMeters(16f) * facingDir;
        float offsetY = 0f;

        if (aimY == 1) {
            offsetX = PhysicsConstants.toMeters(10f) * facingDir;
            offsetY = PhysicsConstants.toMeters(16f);
        } else if (aimY == -1) {
            offsetX = PhysicsConstants.toMeters(10f) * facingDir;
            offsetY = PhysicsConstants.toMeters(-14f);
        }

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(halfW, halfH, new Vector2(offsetX, offsetY), 0f);

        FixtureDef fd = new FixtureDef();
        fd.shape = shape;
        fd.isSensor = true;

        Fixture fx = ownerBody.createFixture(fd);

        Hitbox hb = new Hitbox(DEFAULT_MELEE_DURATION, damage, ownerFaction, owner);
        fx.setUserData(hb);

        shape.dispose();

        active.add(new ActiveHitbox(ownerBody, fx, hb));
    }

    public void update(float delta) {
        Iterator<ActiveHitbox> it = active.iterator();
        while (it.hasNext()) {
            ActiveHitbox ah = it.next();
            ah.hitbox.timeLeft -= delta;

            if (ah.hitbox.timeLeft <= 0f) {
                safeDestroyFixture(ah.ownerBody, ah.fixture);
                it.remove();
            }
        }
    }

    private void safeDestroyFixture(Body body, Fixture fixture) {
        if (body == null || fixture == null) return;
        if (body.getWorld() == null) return;

        if (!body.getFixtureList().contains(fixture, true)) return;
        if (body.getFixtureList().size == 0) return;

        body.destroyFixture(fixture);
    }

    public void handleHitboxContact(Fixture hitboxFix, Fixture otherFix) {
        if (hitboxFix == null || otherFix == null) return;
        if (!Hitbox.isHitboxFixture(hitboxFix)) return;

        Hitbox hb = Hitbox.get(hitboxFix);

        Object otherUD = otherFix.getUserData();
        if (!(otherUD instanceof Damageable target)) return;
        if (!target.isAlive()) return;

        if (target.getFaction() == hb.ownerFaction) return;
        if (!hb.canHit(target)) return;

        float hx = hitboxFix.getBody().getPosition().x;
        float ox = otherFix.getBody().getPosition().x;
        float dir = (ox >= hx) ? 1f : -1f;

        Vector2 knock = new Vector2(dir * KNOCKBACK_X, KNOCKBACK_Y);
        target.applyDamage(hb.damage, knock);
        hb.markHit(target);

        if (hb.owner != null && hb.owner.isAlive()) {
            Body ownerBody = hitboxFix.getBody();
            Vector2 ov = ownerBody.getLinearVelocity();
            ownerBody.setLinearVelocity(ov.x - dir * ATTACKER_RECOIL_X, ov.y + ATTACKER_RECOIL_Y);
        }

        hitThisFrame = true;
    }
}
