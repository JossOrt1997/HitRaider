package com.analiticasoft.hitraider.world;

public class EncounterManager {

    public enum State {
        FIGHT,
        CLEAR
    }

    private State state = State.FIGHT;

    public void reset() {
        state = State.FIGHT;
    }

    public void update(float delta, int aliveEnemies) {
        if (state == State.CLEAR) return;
        if (aliveEnemies <= 0) state = State.CLEAR;
    }

    public State getState() {
        return state;
    }
}
