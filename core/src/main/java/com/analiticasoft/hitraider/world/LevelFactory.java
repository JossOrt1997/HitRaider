package com.analiticasoft.hitraider.world;

import com.analiticasoft.hitraider.physics.CollisionBits;
import com.analiticasoft.hitraider.physics.PhysicsConstants;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;

public class LevelFactory {

    public static class PlatformRect {
        public final float cx, cy, w, h;
        public final String type; // "ground" / "oneway"
        public PlatformRect(float cx, float cy, float w, float h, String type) {
            this.cx = cx; this.cy = cy; this.w = w; this.h = h; this.type = type;
        }
    }

    public static Array<PlatformRect> createTestLevel(World world) {
        Array<PlatformRect> rects = new Array<>();

        // Main floor
        createStaticBox(world, 0f, 48f, 20000f, 16f, "ground");
        rects.add(new PlatformRect(0f, 48f, 20000f, 16f, "ground"));

        // Platform sections
        // Section 1
        createStaticBox(world, 220f, 120f, 180f, 16f, "ground");
        rects.add(new PlatformRect(220f, 120f, 180f, 16f, "ground"));

        createStaticBox(world, 520f, 170f, 140f, 16f, "ground");
        rects.add(new PlatformRect(520f, 170f, 140f, 16f, "ground"));

        // Oneway platform
        createStaticBox(world, 650f, 230f, 220f, 12f, "oneway");
        rects.add(new PlatformRect(650f, 230f, 220f, 12f, "oneway"));

        // Section 2
        createStaticBox(world, 820f, 140f, 220f, 16f, "ground");
        rects.add(new PlatformRect(820f, 140f, 220f, 16f, "ground"));

        createStaticBox(world, 1100f, 180f, 200f, 16f, "ground");
        rects.add(new PlatformRect(1100f, 180f, 200f, 16f, "ground"));

        createStaticBox(world, 1400f, 130f, 250f, 16f, "ground");
        rects.add(new PlatformRect(1400f, 130f, 250f, 16f, "ground"));

        createStaticBox(world, 1700f, 190f, 200f, 16f, "oneway");
        rects.add(new PlatformRect(1700f, 190f, 200f, 16f, "oneway"));

        // End wall (far away now)
        createStaticBox(world, 2000f, 120f, 16f, 400f, "ground");
        rects.add(new PlatformRect(2000f, 120f, 16f, 400f, "ground"));

        return rects;
    }

    private static Body createStaticBox(World world,
                                        float centerXpx, float centerYpx,
                                        float widthPx, float heightPx,
                                        String userData) {

        float cx = PhysicsConstants.toMeters(centerXpx);
        float cy = PhysicsConstants.toMeters(centerYpx);

        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.StaticBody;
        bd.position.set(cx, cy);

        Body body = world.createBody(bd);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(
            PhysicsConstants.toMeters(widthPx / 2f),
            PhysicsConstants.toMeters(heightPx / 2f)
        );

        FixtureDef fd = new FixtureDef();
        fd.shape = shape;
        fd.friction = 0.9f;
        fd.restitution = 0f;

        // ✅ WORLD filter (solid surfaces)
        fd.filter.categoryBits = CollisionBits.WORLD;
        fd.filter.maskBits = CollisionBits.MASK_WORLD_SOLID;

        Fixture f = body.createFixture(fd);
        f.setUserData(userData);

        shape.dispose();
        return body;
    }
}
