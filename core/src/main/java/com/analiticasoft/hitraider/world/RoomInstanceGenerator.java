package com.analiticasoft.hitraider.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import java.util.Random;

/**
 * Generates a run: sequence of RoomInstance with deterministic spawns based on seed.
 * Current rules (Phase A):
 * - Every N rooms insert CHOICE room
 * - Budget ramps slowly
 */
public class RoomInstanceGenerator {

    public Array<RoomInstance> generate(long seed, int totalRooms, RoomTemplateRegistry templates) {
        Random rng = new Random(seed);

        Array<RoomInstance> out = new Array<>(totalRooms);

        for (int i = 0; i < totalRooms; i++) {
            boolean isChoice = (i > 0 && i % 4 == 0); // every 4 rooms

            RoomType type = isChoice ? RoomType.CHOICE : RoomType.COMBAT;

            RoomTemplate tpl = templates.get(rng.nextInt(Math.max(1, templates.size())));

            // Budget ramp
            int budget = 2 + i / 2;

            // Enemy counts from budget
            int melee = 0;
            int ranged = 0;

            if (type == RoomType.COMBAT) {
                // simple split
                melee = Math.max(1, budget / 2);
                ranged = Math.max(0, budget - melee - 1);

                // clamp
                if (melee < 0) melee = 0;
                if (ranged < 0) ranged = 0;
            }

            float dropChance = 0.25f + Math.min(0.25f, i * 0.02f);

            RoomInstance room = new RoomInstance(seed ^ (long)i * 1315423911L, type, tpl, budget, melee, ranged, dropChance);

            // Deterministic spawn order
            if (tpl.spawns.size > 0) {
                // copy spawns then shuffle
                Array<Vector2> tmp = new Array<>(tpl.spawns.size);
                for (Vector2 v : tpl.spawns) tmp.add(new Vector2(v));
                tmp.shuffle(); // uses MathUtils; but Array.shuffle is deterministic per JVM (good enough for now)
                for (Vector2 v : tmp) room.spawnOrder.add(v);
            }

            out.add(room);
        }

        // Ensure first room is combat (avoid start choice)
        if (out.size > 0 && out.get(0).type == RoomType.CHOICE) {
            RoomInstance r0 = out.get(0);
            out.set(0, new RoomInstance(seed ^ 777, RoomType.COMBAT, r0.template, r0.budget, Math.max(1, r0.meleeCount), r0.rangedCount, r0.relicDropChance));
        }

        return out;
    }
}
