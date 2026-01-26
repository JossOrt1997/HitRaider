package com.analiticasoft.hitraider.combat;

import com.analiticasoft.hitraider.physics.CollisionBits;
import com.analiticasoft.hitraider.physics.PhysicsConstants;
import com.badlogic.gdx.physics.box2d.*;

import java.util.concurrent.atomic.AtomicLong;

public class Projectile {

    private static final AtomicLong SEQ = new AtomicLong(1);

    public enum State { ALIVE, IMPACT }

    public final long id;
    public final Body body;
    public final Faction faction;
    public final int damage;

    public State state = State.ALIVE;

    public float timeLeft;
    public float hitLock = 0f;

    public float lastXpx;
    public float lastYpx;

    public float impactFxLeft = 0f;
    public boolean impactQueued = false;

    // piercing
    public int piercesLeft = 0;

    public Projectile(World world, Faction faction, int damage,
                      float xPx, float yPx,
                      float vxMps, float vyMps,
                      float lifetimeSec) {

        this.id = SEQ.getAndIncrement();
        this.faction = faction;
        this.damage = damage;
        this.timeLeft = lifetimeSec;

        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.DynamicBody;
        bd.bullet = true;
        bd.position.set(PhysicsConstants.toMeters(xPx), PhysicsConstants.toMeters(yPx));

        body = world.createBody(bd);

        CircleShape s = new CircleShape();
        s.setRadius(PhysicsConstants.toMeters(3f));

        FixtureDef fd = new FixtureDef();
        fd.shape = s;
        fd.isSensor = true;

        // ✅ Projectile only hits WORLD + bodies, not pickups/sensors
        fd.filter.categoryBits = CollisionBits.PROJECTILE;
        fd.filter.maskBits = CollisionBits.MASK_PROJECTILE;

        Fixture fx = body.createFixture(fd);
        fx.setUserData(this);

        s.dispose();

        body.setGravityScale(0f);
        body.setLinearVelocity(vxMps, vyMps);

        lastXpx = xPx;
        lastYpx = yPx;
    }

    public void tickAlive(float delta) {
        if (hitLock > 0f) hitLock = Math.max(0f, hitLock - delta);

        lastXpx = PhysicsConstants.toPixels(body.getPosition().x);
        lastYpx = PhysicsConstants.toPixels(body.getPosition().y);

        timeLeft -= delta;
    }

    public void beginImpactFx() {
        state = State.IMPACT;
        impactFxLeft = 0.10f;
    }

    public void tickImpact(float delta) {
        impactFxLeft = Math.max(0f, impactFxLeft - delta);
    }

    public boolean impactDone() {
        return impactFxLeft <= 0f;
    }
}
