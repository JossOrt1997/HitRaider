package com.analiticasoft.hitraider.combat;

import com.analiticasoft.hitraider.physics.PhysicsConstants;
import com.badlogic.gdx.physics.box2d.*;

public class Projectile {

    public final Body body;
    public final Faction faction;
    public final int damage;

    public float timeLeft;
    public boolean dead = false;

    public Projectile(World world, Faction faction, int damage, float xPx, float yPx, float vxMps, float vyMps, float lifetimeSec) {
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

        Fixture fx = body.createFixture(fd);
        fx.setUserData(this);

        s.dispose();

        body.setGravityScale(0f);
        body.setLinearVelocity(vxMps, vyMps);
    }

    public float getXpx() { return PhysicsConstants.toPixels(body.getPosition().x); }
    public float getYpx() { return PhysicsConstants.toPixels(body.getPosition().y); }
}
