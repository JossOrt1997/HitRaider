package com.analiticasoft.hitraider.world;

import com.analiticasoft.hitraider.combat.EnemyProfiles;
import com.analiticasoft.hitraider.entities.MeleeEnemy;
import com.analiticasoft.hitraider.render.CharacterAnimator;
import com.analiticasoft.hitraider.render.DebugAnimLibrary;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;

public class SpawnManager {

    private final Array<Vector2> spawnPoints = new Array<>();
    private int rr = 0;

    public void setSpawnPoints(Array<Vector2> points) {
        spawnPoints.clear();
        spawnPoints.addAll(points);
        rr = 0;
    }

    public void spawnMeleeWave(
        World world,
        float playerXpx,
        float minDistPx,
        int count,
        EnemyProfiles.MeleeAIProfile profile,
        Array<MeleeEnemy> enemiesOut,
        Array<CharacterAnimator> enemyAnimsOut
    ) {
        if (spawnPoints.size == 0) return;

        int spawned = 0;
        int guard = 0;

        while (spawned < count && guard++ < 200) {
            Vector2 p = spawnPoints.get(rr % spawnPoints.size);
            rr++;

            if (Math.abs(p.x - playerXpx) < minDistPx) continue;

            MeleeEnemy e = new MeleeEnemy(world, p.x, p.y, profile);
            enemiesOut.add(e);

            CharacterAnimator ea = new CharacterAnimator();
            DebugAnimLibrary.defineMeleeEnemy(ea);
            enemyAnimsOut.add(ea);

            spawned++;
        }
    }
}
