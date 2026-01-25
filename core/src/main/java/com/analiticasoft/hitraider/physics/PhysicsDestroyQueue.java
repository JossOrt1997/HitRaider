package com.analiticasoft.hitraider.physics;

import com.analiticasoft.hitraider.combat.CombatSystem;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;

/**
 * Cola única de destrucción Box2D.
 *
 * Regla: NO destruir bodies/fixtures directo desde gameplay ni callbacks.
 * Encolar y ejecutar flush() SOLO después del physics.step().
 *
 * Best-effort: si el world está locked, reintenta el siguiente frame.
 */
public class PhysicsDestroyQueue {

    private static class FixtureRef {
        final Body body;
        final Fixture fixture;
        FixtureRef(Body body, Fixture fixture) {
            this.body = body;
            this.fixture = fixture;
        }
    }

    private final Array<Body> bodies = new Array<>();
    private final Array<FixtureRef> fixtures = new Array<>();

    public void clear() {
        bodies.clear();
        fixtures.clear();
    }

    public void queueBody(Body body) {
        if (body == null) return;
        for (int i = 0; i < bodies.size; i++) if (bodies.get(i) == body) return;
        bodies.add(body);
    }

    public void queueFixture(Body body, Fixture fixture) {
        if (body == null || fixture == null) return;
        for (int i = 0; i < fixtures.size; i++) {
            FixtureRef fr = fixtures.get(i);
            if (fr.body == body && fr.fixture == fixture) return;
        }
        fixtures.add(new FixtureRef(body, fixture));
    }

    /** Ejecuta destrucciones. Llamar DESPUÉS de physics.step(delta). */
    public void flush(World world, CombatSystem combat) {
        if (world == null) return;
        if (world.isLocked()) return;

        // Destroy fixtures first
        for (int i = fixtures.size - 1; i >= 0; i--) {
            FixtureRef fr = fixtures.get(i);
            fixtures.removeIndex(i);

            if (fr.body == null || fr.fixture == null) continue;
            if (fr.body.getWorld() != world) continue;
            if (!fr.body.getFixtureList().contains(fr.fixture, true)) continue;

            try { fr.body.destroyFixture(fr.fixture); } catch (Throwable ignored) {}
        }

        // Destroy bodies
        for (int i = bodies.size - 1; i >= 0; i--) {
            Body b = bodies.get(i);
            bodies.removeIndex(i);

            if (b == null) continue;
            if (b.getWorld() != world) continue;

            try { if (combat != null) combat.purgeForBody(b); } catch (Throwable ignored) {}
            try { world.destroyBody(b); } catch (Throwable ignored) {}
        }
    }
}
