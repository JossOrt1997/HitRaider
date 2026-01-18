package com.analiticasoft.hitraider.render;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public interface CharacterRenderer {
    void draw(ShapeRenderer shapes, float pivotX, float pivotY, int facingDir, FrameDef frame);
}
