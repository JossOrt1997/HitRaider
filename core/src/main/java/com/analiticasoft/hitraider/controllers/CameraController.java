package com.analiticasoft.hitraider.controllers;

import com.analiticasoft.hitraider.config.CameraTuning;
import com.analiticasoft.hitraider.entities.Player;
import com.badlogic.gdx.graphics.OrthographicCamera;

public class CameraController {

    public void follow(OrthographicCamera cam, Player player) {
        float targetX = player.getXpx();
        float targetY = player.getYpx() + CameraTuning.FOLLOW_OFFSET_Y;

        cam.position.x += (targetX - cam.position.x) * CameraTuning.FOLLOW_LERP_X;
        cam.position.y += (targetY - cam.position.y) * CameraTuning.FOLLOW_LERP_Y;
    }
}
