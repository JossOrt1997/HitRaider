package com.analiticasoft.hitraider.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class RoomInstance {

    public final int index;
    public final RoomType type;
    public final long seed;

    public final RoomTemplate template;

    public final int budget;
    public final int meleeCount;
    public final int rangedCount;

    public final Array<Vector2> spawnOrder = new Array<>();

    public final float relicDropChance;

    public RoomInstance(int index, RoomType type, long seed, RoomTemplate template,
                        int budget, int meleeCount, int rangedCount,
                        float relicDropChance) {
        this.index = index;
        this.type = type;
        this.seed = seed;
        this.template = template;
        this.budget = budget;
        this.meleeCount = meleeCount;
        this.rangedCount = rangedCount;
        this.relicDropChance = relicDropChance;
    }
}
