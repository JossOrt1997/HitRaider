package com.analiticasoft.hitraider.world;

import com.badlogic.gdx.utils.Array;

public class RoomTemplateRegistry {

    private final Array<RoomTemplate> templates = new Array<>();

    public void clear() {
        templates.clear();
    }

    public void add(RoomTemplate t) {
        templates.add(t);
    }

    public int size() {
        return templates.size;
    }

    public RoomTemplate get(int idx) {
        return templates.get(idx);
    }

    public Array<RoomTemplate> all() {
        return templates;
    }
}
