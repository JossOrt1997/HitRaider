package com.analiticasoft.hitraider.render;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Dibuja "frames" con rectángulos. Reemplazable por SpriteRenderer después.
 */
public class DebugCharacterRenderer {

    public void draw(ShapeRenderer shapes, float pivotX, float pivotY, int facingDir, FrameDef frame) {
        if (frame == null) return;

        for (FrameDef.Rect r : frame.parts) {
            float rx = r.x;
            // mirror en X si mira a la izquierda
            if (facingDir < 0) rx = -(r.x + r.w);

            shapes.rect(pivotX + rx, pivotY + r.y, r.w, r.h);
        }
    }
}
