package com.analiticasoft.hitraider.combat;

import com.analiticasoft.hitraider.physics.PhysicsDestroyQueue;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;

/**
 * ProjectileSystem (fortified):
 * - Never destroys bodies directly if destroyQueue is provided
 * - flushImpacts() must be called after physics.step()
 */
public class ProjectileSystem {

    private final World world;
    public final Array<Projectile> projectiles = new Array<>();

    private int impactsEnemyThisFrame = 0;
    private int impactsWorldThisFrame = 0;

    private PhysicsDestroyQueue destroyQueue; // optional but recommended

    public ProjectileSystem(World world) {
        this.world = world;
    }

    public void setDestroyQueue(PhysicsDestroyQueue q) {
        this.destroyQueue = q;
    }

    public void spawn(Projectile p) { projectiles.add(p); }

    public void queueImpact(Projectile p) {
        if (p == null) return;
        if (p.state != Projectile.State.ALIVE) return;
        p.impactQueued = true;
    }

    public void notifyImpactEnemy() { impactsEnemyThisFrame++; }
    public void notifyImpactWorld() { impactsWorldThisFrame++; }

    public int consumeImpactsEnemy() { int v = impactsEnemyThisFrame; impactsEnemyThisFrame = 0; return v; }
    public int consumeImpactsWorld() { int v = impactsWorldThisFrame; impactsWorldThisFrame = 0; return v; }

    /** Call AFTER physics.step(delta) */
    public void flushImpacts() {
        for (Projectile p : projectiles) {
            if (p.state == Projectile.State.ALIVE && p.impactQueued) {
                if (p.body != null && p.body.getWorld() != null) {
                    p.lastXpx = com.analiticasoft.hitraider.physics.PhysicsConstants.toPixels(p.body.getPosition().x);
                    p.lastYpx = com.analiticasoft.hitraider.physics.PhysicsConstants.toPixels(p.body.getPosition().y);

                    if (destroyQueue != null) destroyQueue.queueBody(p.body);
                    else world.destroyBody(p.body);
                }
                p.beginImpactFx();
                p.impactQueued = false;
            }
        }
    }

    public void update(float delta) {
        for (int i = projectiles.size - 1; i >= 0; i--) {
            Projectile p = projectiles.get(i);

            if (p.state == Projectile.State.ALIVE) {
                p.tickAlive(delta);

                if (p.timeLeft <= 0f) {
                    if (p.body != null && p.body.getWorld() != null) {
                        if (destroyQueue != null) destroyQueue.queueBody(p.body);
                        else world.destroyBody(p.body);
                    }
                    projectiles.removeIndex(i);
                }
            } else {
                p.tickImpact(delta);
                if (p.impactDone()) {
                    projectiles.removeIndex(i);
                }
            }
        }
    }
}
