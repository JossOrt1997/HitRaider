package com.analiticasoft.hitraider.game;

import com.badlogic.gdx.Game;
import com.analiticasoft.hitraider.screens.GameplayScreen;

public class HitRaiderGame extends Game {
    @Override
    public void create() {
        setScreen(new GameplayScreen());
    }

    @Override
    public void dispose() {
        if (getScreen() != null) getScreen().dispose();
    }
}
