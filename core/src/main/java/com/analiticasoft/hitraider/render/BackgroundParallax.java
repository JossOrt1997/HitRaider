package com.analiticasoft.hitraider.render;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * 2-layer parallax. If any texture is null, it just skips it.
 * Textures are expected to repeat horizontally.
 */
public class BackgroundParallax {

    private final Texture base;
    private final Texture mid;

    private final float baseFactor;
    private final float midFactor;
    private final float worldHeightPx;

    public BackgroundParallax(Texture base, Texture mid, float baseFactor, float midFactor, float worldHeightPx) {
        this.base = base;
        this.mid = mid;
        this.baseFactor = baseFactor;
        this.midFactor = midFactor;
        this.worldHeightPx = worldHeightPx;

        if (this.base != null) this.base.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.ClampToEdge);
        if (this.mid != null) this.mid.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.ClampToEdge);
    }

    public void render(SpriteBatch batch, float cameraCenterXpx, float cameraCenterYpx, float viewWidthPx, float viewHeightPx) {
        float viewBottomY = cameraCenterYpx - viewHeightPx / 2f;
        if (base != null) drawLayer(batch, base, baseFactor, cameraCenterXpx, viewBottomY, viewWidthPx);
        if (mid != null) drawLayer(batch, mid, midFactor, cameraCenterXpx, viewBottomY, viewWidthPx);
    }

    private void drawLayer(SpriteBatch batch, Texture tex, float factor,
                           float cameraCenterXpx, float viewBottomY, float viewWidthPx) {

        float texW = tex.getWidth();
        float texH = tex.getHeight();

        float scale = worldHeightPx / texH;
        float drawH = worldHeightPx;
        float drawW = texW * scale;

        float layerCenterX = cameraCenterXpx * factor;
        float left = (layerCenterX - viewWidthPx / 2f);
        float startX = (float)Math.floor(left / drawW) * drawW;

        float y = viewBottomY;

        int tiles = (int)Math.ceil(viewWidthPx / drawW) + 2;
        for (int i = 0; i < tiles; i++) {
            float x = startX + i * drawW;
            batch.draw(tex, x, y, drawW, drawH);
        }
    }
}
