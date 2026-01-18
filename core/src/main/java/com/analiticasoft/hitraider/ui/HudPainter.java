package com.analiticasoft.hitraider.ui;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class HudPainter {

    public void drawHpBar(ShapeRenderer shapes, float x, float y, float w, float h, int hp, int maxHp) {
        float pct = (maxHp <= 0) ? 0f : (hp / (float) maxHp);
        pct = Math.max(0f, Math.min(1f, pct));

        // background
        shapes.rect(x, y, w, h);
        // fill (draw later with another color)
        shapes.rect(x, y, w * pct, h);
    }

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
