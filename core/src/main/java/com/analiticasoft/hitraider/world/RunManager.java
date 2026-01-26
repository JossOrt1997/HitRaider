package com.analiticasoft.hitraider.world;

import com.badlogic.gdx.utils.Array;

public class RunManager {

    public long seed;
    public int totalRooms;
    public int index;

    private Array<RoomInstance> rooms;

    public void start(long seed, int totalRooms, Array<RoomInstance> rooms) {
        this.seed = seed;
        this.totalRooms = totalRooms;
        this.rooms = rooms;
        this.index = 0;
    }

    public RoomInstance current() {
        if (rooms == null || rooms.size == 0) return null;
        return rooms.get(index);
    }

    public boolean hasNext() {
        return rooms != null && index + 1 < rooms.size;
    }

    public void next() {
        if (!hasNext()) return;
        index++;
    }

    public void reset() {
        index = 0;
    }

    public int size() {
        return rooms == null ? 0 : rooms.size;
    }
}
