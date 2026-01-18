package com.analiticasoft.hitraider.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class RoomManager {

    public static class RoomDef {
        public final float entryXpx, entryYpx;
        public final float exitXpx;
        public final int meleeCount;
        public final int rangedCount;
        public final Array<Vector2> spawns = new Array<>();

        public RoomDef(float entryXpx, float entryYpx, float exitXpx, int meleeCount, int rangedCount) {
            this.entryXpx = entryXpx;
            this.entryYpx = entryYpx;
            this.exitXpx = exitXpx;
            this.meleeCount = meleeCount;
            this.rangedCount = rangedCount;
        }

        public RoomDef addSpawn(float x, float y) { spawns.add(new Vector2(x, y)); return this; }
    }

    private final Array<RoomDef> rooms = new Array<>();
    private int idx = 0;

    public void clear() { rooms.clear(); idx = 0; }
    public void add(RoomDef r) { rooms.add(r); }
    public RoomDef current() { return rooms.size == 0 ? null : rooms.get(idx); }
    public boolean hasNext() { return idx < rooms.size - 1; }
    public void next() { if (hasNext()) idx++; }
    public void reset() { idx = 0; }
    public int index() { return idx; }
}
