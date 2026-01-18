package com.analiticasoft.hitraider.combat;

import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;

public class ProjectileSystem {

    private final World world;
    public final Array<Projectile> projectiles = new Array<>();

    public ProjectileSystem(World world) {
        this.world = world;
    }

    public void spawn(Projectile p) {
        projectiles.add(p);
    }

    public void update(float delta) {
        for (int i = projectiles.size - 1; i >= 0; i--) {
            Projectile p = projectiles.get(i);
            p.timeLeft -= delta;
            if (p.dead || p.timeLeft <= 0f) {
                // destruir body de manera segura
                if (p.body.getWorld() != null) world.destroyBody(p.body);
                projectiles.removeIndex(i);
            }
        }
    }
}
