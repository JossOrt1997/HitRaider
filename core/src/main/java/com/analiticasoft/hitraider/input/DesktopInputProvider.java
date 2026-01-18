package com.analiticasoft.hitraider.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

public class DesktopInputProvider {

    public void poll(InputState state) {
        boolean left  = Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT);
        boolean right = Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT);

        float moveX = 0f;
        if (left) moveX -= 1f;
        if (right) moveX += 1f;
        state.setMoveX(moveX);

        // NEW: vertical intent (aim)
        state.setDown(Action.MOVE_UP, Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP));
        state.setDown(Action.MOVE_DOWN, Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN));

        state.setDown(Action.JUMP,   Gdx.input.isKeyPressed(Input.Keys.SPACE));
        state.setDown(Action.ATTACK, Gdx.input.isKeyPressed(Input.Keys.J));
        state.setDown(Action.SHOOT,  Gdx.input.isKeyPressed(Input.Keys.K));
        state.setDown(Action.DASH,   Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT));

        // PAUSE should be tap (justPressed)
        state.setDown(Action.PAUSE,  Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE));
    }
}
