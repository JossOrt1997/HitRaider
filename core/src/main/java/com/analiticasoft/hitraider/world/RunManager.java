package com.analiticasoft.hitraider.world;

import com.badlogic.gdx.utils.Array;

public class RunManager {

    public long seed;
    public int totalRooms;
    public int index;

    public Array<RoomInstance> rooms;

    public void start(long seed, int totalRooms, Array<RoomInstance> rooms) {
        this.seed = seed;
        this.totalRooms = totalRooms;
        this.rooms = rooms;
        this.index = 0;
    }

    public RoomInstance current() {
        return rooms.get(index);
    }

    public boolean hasNext() {
        return index < totalRooms - 1;
    }

    public void next() {
        if (hasNext()) index++;
    }

    public boolean isLast() {
        return index >= totalRooms - 1;
    }
}
