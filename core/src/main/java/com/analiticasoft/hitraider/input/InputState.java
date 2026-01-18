package com.analiticasoft.hitraider.input;

import java.util.EnumMap;

public class InputState {
    private final EnumMap<Action, Boolean> down = new EnumMap<>(Action.class);
    private final EnumMap<Action, Boolean> prevDown = new EnumMap<>(Action.class);

    private float moveX; // -1..1

    public InputState() {
        for (Action a : Action.values()) {
            down.put(a, false);
            prevDown.put(a, false);
        }
    }

    public void setDown(Action action, boolean isDownNow) {
        down.put(action, isDownNow);
    }

    public boolean isDown(Action action) {
        return Boolean.TRUE.equals(down.get(action));
    }

    /** True solo el frame en que pasa de false -> true */
    public boolean isJustPressed(Action action) {
        boolean d = Boolean.TRUE.equals(down.get(action));
        boolean pd = Boolean.TRUE.equals(prevDown.get(action));
        return d && !pd;
    }

    public float getMoveX() { return moveX; }
    public void setMoveX(float moveX) { this.moveX = moveX; }

    /** Llamar 1 vez por frame, al final del update */
    public void endFrame() {
        for (Action a : Action.values()) {
            prevDown.put(a, down.get(a));
        }
    }
}
