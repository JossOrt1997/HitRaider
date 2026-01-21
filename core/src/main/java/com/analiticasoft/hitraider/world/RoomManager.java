package com.analiticasoft.hitraider.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/**
 * Rooms hardcoded (Semana 5 demo ready).
 * Maneja progreso lineal: current -> next -> reset.
 */
public class RoomManager {

    public static class RoomDef {
        public final float entryXpx;
        public final float entryYpx;
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

        public RoomDef addSpawn(float xPx, float yPx) {
            spawns.add(new Vector2(xPx, yPx));
            return this;
        }
    }

    private final Array<RoomDef> rooms = new Array<>();
    private int idx = 0;

    public void clear() {
        rooms.clear();
        idx = 0;
    }

    public void add(RoomDef room) {
        rooms.add(room);
    }

    public RoomDef current() {
        if (rooms.size == 0) return null;
        return rooms.get(idx);
    }

    public boolean hasNext() {
        return idx < rooms.size - 1;
    }

    public void next() {
        if (hasNext()) idx++;
    }

    public void reset() {
        idx = 0;
    }

    public int index() {
        return idx;
    }

    public int size() {
        return rooms.size;
    }
}
