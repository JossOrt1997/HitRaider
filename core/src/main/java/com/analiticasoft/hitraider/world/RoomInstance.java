package com.analiticasoft.hitraider.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class RoomInstance {

    public final long seed;
    public final RoomType type;
    public final RoomTemplate template;

    // Difficulty / budget (simple now, scalable later)
    public final int budget;
    public final int meleeCount;
    public final int rangedCount;

    // Drop chance per room
    public final float relicDropChance;

    // Spawn order already decided for determinism
    public final Array<Vector2> spawnOrder = new Array<>();

    public RoomInstance(long seed,
                        RoomType type,
                        RoomTemplate template,
                        int budget,
                        int meleeCount,
                        int rangedCount,
                        float relicDropChance) {

        this.seed = seed;
        this.type = type;
        this.template = template;
        this.budget = budget;
        this.meleeCount = meleeCount;
        this.rangedCount = rangedCount;
        this.relicDropChance = relicDropChance;
    }
}
