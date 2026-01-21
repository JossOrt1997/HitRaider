package com.analiticasoft.hitraider.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import java.util.Random;

public class RoomInstanceGenerator {

    public Array<RoomInstance> generate(long runSeed, int totalRooms, RoomTemplateRegistry registry) {
        Array<RoomInstance> out = new Array<>();
        Random runRng = new Random(runSeed);

        int i = 0;
        while (out.size < totalRooms) {
            RoomType type;

            if (out.size == 0) type = RoomType.START;
            else if (out.size % 5 == 3) type = RoomType.ELITE;   // cada bloque
            else if (out.size % 5 == 4) type = RoomType.CHOICE;  // after elite
            else type = RoomType.NORMAL;

            long roomSeed = runRng.nextLong();
            Random roomRng = new Random(roomSeed);

            int templateIdx = Math.floorMod(roomRng.nextInt(), registry.size());
            RoomTemplate tpl = registry.get(templateIdx);

            int base = 3 + out.size; // sube con progreso
            float mult = switch (type) {
                case START -> 0.6f;
                case NORMAL -> 1.0f;
                case ELITE -> 1.5f;
                case CHOICE -> 0.0f;
            };

            int budget = Math.max(0, Math.round(base * mult));

            float rangedChance = switch (type) {
                case START -> 0.10f;
                case NORMAL -> 0.25f;
                case ELITE -> 0.40f;
                case CHOICE -> 0.0f;
            };

            int melee = 0, ranged = 0;
            int b = budget;
            while (b > 0) {
                boolean spawnRanged = (b >= 2) && roomRng.nextFloat() < rangedChance;
                if (spawnRanged) { ranged++; b -= 2; }
                else { melee++; b -= 1; }
            }

            float relicChance = switch (type) {
                case START -> 0.10f;
                case NORMAL -> 0.25f;
                case ELITE -> 0.45f;
                case CHOICE -> 0.0f; // choice usa elección, no RNG
            };

            RoomInstance inst = new RoomInstance(out.size, type, roomSeed, tpl, budget, melee, ranged, relicChance);

            inst.spawnOrder.addAll(tpl.spawns);
            shuffle(inst.spawnOrder, new Random(roomSeed ^ 0x9E3779B97F4A7C15L));

            out.add(inst);
            i++;
        }

        return out;
    }

    private void shuffle(Array<Vector2> arr, Random rng) {
        for (int i = arr.size - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            Vector2 tmp = arr.get(i);
            arr.set(i, arr.get(j));
            arr.set(j, tmp);
        }
    }
}
