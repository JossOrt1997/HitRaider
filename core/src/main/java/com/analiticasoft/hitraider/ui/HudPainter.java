package com.analiticasoft.hitraider.ui;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Small UI helper for fading text.
 */
public class HudPainter {

    public void drawFadingText(SpriteBatch batch, BitmapFont font, String text, float x, float y, float alpha) {
        float oldA = font.getColor().a;
        font.getColor().a = Math.max(0f, Math.min(1f, alpha));
        font.draw(batch, text, x, y);
        font.getColor().a = oldA;
    }
}
