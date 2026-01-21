package com.analiticasoft.hitraider.physics;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;

public class PhysicsWorld {
    public final World world;
    private float accumulator = 0f;

    private static final float TIME_STEP = 1f / 60f;
    private static final int VELOCITY_ITERS = 6;
    private static final int POSITION_ITERS = 2;

    public PhysicsWorld(Vector2 gravity) {
        this.world = new World(gravity, true);
    }

    public void step(float delta) {
        float frameTime = Math.min(delta, 0.25f);
        accumulator += frameTime;
        while (accumulator >= TIME_STEP) {
            world.step(TIME_STEP, VELOCITY_ITERS, POSITION_ITERS);
            accumulator -= TIME_STEP;
        }
    }

    public void dispose() {
        world.dispose();
    }
}
