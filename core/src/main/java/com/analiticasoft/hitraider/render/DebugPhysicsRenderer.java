package com.analiticasoft.hitraider.render;

import com.analiticasoft.hitraider.physics.PhysicsConstants;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

public class DebugPhysicsRenderer {

    private final Vector2 tmp = new Vector2();
    private final Vector2 tmpWorld = new Vector2();
    private final Vector2 first = new Vector2();
    private final Vector2 prev = new Vector2();

    public void drawFixtureOutline(com.badlogic.gdx.graphics.glutils.ShapeRenderer shapes, Fixture fx) {
        if (fx == null) return;
        Shape s = fx.getShape();
        if (s instanceof PolygonShape poly) {
            drawPolygon(shapes, fx.getBody(), poly);
        } else if (s instanceof CircleShape circle) {
            drawCircle(shapes, fx.getBody(), circle);
        }
    }

    private void drawPolygon(com.badlogic.gdx.graphics.glutils.ShapeRenderer shapes, Body body, PolygonShape poly) {
        int vc = poly.getVertexCount();
        if (vc <= 1) return;

        // first
        poly.getVertex(0, tmp);
        toWorld(body, tmp, tmpWorld);
        first.set(tmpWorld);
        prev.set(tmpWorld);

        for (int i = 1; i < vc; i++) {
            poly.getVertex(i, tmp);
            toWorld(body, tmp, tmpWorld);

            shapes.line(
                PhysicsConstants.toPixels(prev.x), PhysicsConstants.toPixels(prev.y),
                PhysicsConstants.toPixels(tmpWorld.x), PhysicsConstants.toPixels(tmpWorld.y)
            );
            prev.set(tmpWorld);
        }

        // close
        shapes.line(
            PhysicsConstants.toPixels(prev.x), PhysicsConstants.toPixels(prev.y),
            PhysicsConstants.toPixels(first.x), PhysicsConstants.toPixels(first.y)
        );
    }

    private void drawCircle(com.badlogic.gdx.graphics.glutils.ShapeRenderer shapes, Body body, CircleShape circle) {
        // center in world meters
        tmp.set(circle.getPosition());
        toWorld(body, tmp, tmpWorld);

        float cx = PhysicsConstants.toPixels(tmpWorld.x);
        float cy = PhysicsConstants.toPixels(tmpWorld.y);
        float r = PhysicsConstants.toPixels(circle.getRadius());

        // ShapeRenderer.circle usa pixeles; perfecto
        shapes.circle(cx, cy, r, 20);
    }

    private void toWorld(Body body, Vector2 localMeters, Vector2 outWorldMeters) {
        // body.getWorldPoint hace transform local->world
        outWorldMeters.set(body.getWorldPoint(localMeters));
    }
}
