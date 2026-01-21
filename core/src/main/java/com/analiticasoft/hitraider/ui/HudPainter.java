package com.analiticasoft.hitraider.ui;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class HudPainter {
    public void drawFadingText(SpriteBatch batch, BitmapFont font, String text, float x, float y, float alpha) {
        alpha = Math.max(0f, Math.min(1f, alpha));
        var c = font.getColor();
        float oldA = c.a;
        c.a = alpha;
        font.setColor(c);
        font.draw(batch, text, x, y);
        c.a = oldA;
        font.setColor(c);
    }
}
