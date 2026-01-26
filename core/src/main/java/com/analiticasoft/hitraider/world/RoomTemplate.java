package com.analiticasoft.hitraider.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class RoomTemplate {

    public final String id;
    public final float entryXpx;
    public final float entryYpx;
    public final float exitXpx;

    public final Array<Vector2> spawns = new Array<>();

    public RoomTemplate(String id, float entryXpx, float entryYpx, float exitXpx) {
        this.id = id;
        this.entryXpx = entryXpx;
        this.entryYpx = entryYpx;
        this.exitXpx = exitXpx;
    }

    public RoomTemplate addSpawn(float xPx, float yPx) {
        spawns.add(new Vector2(xPx, yPx));
        return this;
    }
}
