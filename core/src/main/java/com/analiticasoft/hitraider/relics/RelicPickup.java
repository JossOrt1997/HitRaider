package com.analiticasoft.hitraider.relics;

import com.analiticasoft.hitraider.physics.PhysicsConstants;
import com.badlogic.gdx.physics.box2d.*;

public class RelicPickup {

    public final Body body;
    public final RelicType type;
    public boolean collected = false;

    public RelicPickup(World world, RelicType type, float xPx, float yPx) {
        this.type = type;

        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.StaticBody;
        bd.position.set(PhysicsConstants.toMeters(xPx), PhysicsConstants.toMeters(yPx));
        body = world.createBody(bd);

        CircleShape s = new CircleShape();
        s.setRadius(PhysicsConstants.toMeters(6f));

        FixtureDef fd = new FixtureDef();
        fd.shape = s;
        fd.isSensor = true;

        Fixture fx = body.createFixture(fd);
        fx.setUserData(this);

        s.dispose();
    }

    public float getXpx() { return PhysicsConstants.toPixels(body.getPosition().x); }
    public float getYpx() { return PhysicsConstants.toPixels(body.getPosition().y); }
}
