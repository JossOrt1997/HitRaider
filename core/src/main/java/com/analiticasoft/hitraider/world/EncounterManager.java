package com.analiticasoft.hitraider.world;

public class EncounterManager {

    public enum State {
        FIGHT, CLEAR
    }

    private State state = State.FIGHT;

    private float clearMessageTimer = 0f;

    public void update(float delta, int enemiesAlive) {
        if (state == State.FIGHT && enemiesAlive <= 0) {
            state = State.CLEAR;
            clearMessageTimer = 1.2f; // mostrar "CLEAR" por 1.2s
        }

        if (clearMessageTimer > 0f) {
            clearMessageTimer = Math.max(0f, clearMessageTimer - delta);
        }
    }

    public State getState() {
        return state;
    }

    public boolean shouldShowClearMessage() {
        return clearMessageTimer > 0f;
    }

    public void reset() {
        state = State.FIGHT;
        clearMessageTimer = 0f;
    }
}
