package com.analiticasoft.hitraider.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/**
 * Controla la progresión de salas (rooms) de forma secuencial.
 * NO maneja física ni rendering, solo datos de sala.
 */
public class RoomManager {

    /* =======================
       Room Definition
       ======================= */
    public static class RoomDef {
        public final float entryXpx;
        public final float entryYpx;
        public final float exitXpx;
        public final int enemyCount;

        public final Array<Vector2> spawns = new Array<>();

        public RoomDef(float entryXpx, float entryYpx, float exitXpx, int enemyCount) {
            this.entryXpx = entryXpx;
            this.entryYpx = entryYpx;
            this.exitXpx = exitXpx;
            this.enemyCount = enemyCount;
        }

        public RoomDef addSpawn(float xPx, float yPx) {
            spawns.add(new Vector2(xPx, yPx));
            return this;
        }
    }

    /* =======================
       Manager State
       ======================= */
    private final Array<RoomDef> rooms = new Array<>();
    private int index = 0;

    /* =======================
       API
       ======================= */

    /** Limpia todo y vuelve a estado inicial */
    public void reset() {
        rooms.clear();
        index = 0;
    }

    /** Agrega una sala */
    public void addRoom(RoomDef room) {
        rooms.add(room);
    }

    /** Sala actual */
    public RoomDef current() {
        if (rooms.size == 0) return null;
        return rooms.get(index);
    }

    /** Avanza a la siguiente sala si existe */
    public void next() {
        if (index < rooms.size - 1) {
            index++;
        }
    }

    /** ¿Hay una sala siguiente? */
    public boolean hasNext() {
        return index < rooms.size - 1;
    }

    /** Índice actual (0-based) */
    public int getIndex() {
        return index;
    }

    /** Total de salas */
    public int size() {
        return rooms.size;
    }
}
