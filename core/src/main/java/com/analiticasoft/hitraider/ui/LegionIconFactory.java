package com.analiticasoft.hitraider.ui;

import com.analiticasoft.hitraider.game.Legion;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap.Format;

/**
 * Genera iconos placeholder por legión (sin assets).
 * Cuando tengas PNG real, puedes reemplazarlo por Texture(Gdx.files.internal(...)).
 */
public final class LegionIconFactory {
    private LegionIconFactory(){}

    public static Texture create(Legion legion, int sizePx) {
        Pixmap pm = new Pixmap(sizePx, sizePx, Format.RGBA8888);

        // Transparent background
        pm.setColor(0, 0, 0, 0);
        pm.fill();

        switch (legion) {
            case SALAMANDERS -> drawSalamanders(pm);
            default -> drawFallback(pm);
        }

        Texture tex = new Texture(pm);
        pm.dispose();
        return tex;
    }

    private static void drawSalamanders(Pixmap pm) {
        int w = pm.getWidth();
        int h = pm.getHeight();

        // Base circle (dark green)
        pm.setColor(0.06f, 0.18f, 0.08f, 1f);
        fillCircle(pm, w/2, h/2, (int)(w * 0.46f));

        // Inner ring (slightly lighter)
        pm.setColor(0.12f, 0.32f, 0.16f, 1f);
        drawCircle(pm, w/2, h/2, (int)(w * 0.40f));

        // Flame-ish symbol (orange)
        pm.setColor(0.85f, 0.45f, 0.10f, 1f);
        // simple "flame": triangle + small notch
        fillTriangle(pm,
            w/2, (int)(h*0.18f),
            (int)(w*0.30f), (int)(h*0.72f),
            (int)(w*0.70f), (int)(h*0.72f)
        );

        pm.setColor(0.10f, 0.10f, 0.10f, 0.55f);
        fillCircle(pm, w/2, (int)(h*0.60f), (int)(w*0.10f)); // small “void” to hint flame cutout
    }

    private static void drawFallback(Pixmap pm) {
        int w = pm.getWidth();
        int h = pm.getHeight();
        pm.setColor(0.12f, 0.12f, 0.14f, 1f);
        fillCircle(pm, w/2, h/2, (int)(w * 0.46f));
        pm.setColor(0.75f, 0.75f, 0.80f, 1f);
        drawCircle(pm, w/2, h/2, (int)(w * 0.40f));
    }

    // --- tiny raster helpers ---

    private static void fillCircle(Pixmap pm, int cx, int cy, int r) {
        int r2 = r * r;
        for (int y = cy - r; y <= cy + r; y++) {
            for (int x = cx - r; x <= cx + r; x++) {
                int dx = x - cx;
                int dy = y - cy;
                if (dx*dx + dy*dy <= r2) {
                    if (x >= 0 && x < pm.getWidth() && y >= 0 && y < pm.getHeight()) {
                        pm.drawPixel(x, y);
                    }
                }
            }
        }
    }

    private static void drawCircle(Pixmap pm, int cx, int cy, int r) {
        int x = r, y = 0;
        int err = 0;

        while (x >= y) {
            plot8(pm, cx, cy, x, y);
            y++;
            if (err <= 0) {
                err += 2*y + 1;
            } else {
                x--;
                err -= 2*x + 1;
            }
        }
    }

    private static void plot8(Pixmap pm, int cx, int cy, int x, int y) {
        drawSafe(pm, cx + x, cy + y);
        drawSafe(pm, cx + y, cy + x);
        drawSafe(pm, cx - y, cy + x);
        drawSafe(pm, cx - x, cy + y);
        drawSafe(pm, cx - x, cy - y);
        drawSafe(pm, cx - y, cy - x);
        drawSafe(pm, cx + y, cy - x);
        drawSafe(pm, cx + x, cy - y);
    }

    private static void drawSafe(Pixmap pm, int x, int y) {
        if (x >= 0 && x < pm.getWidth() && y >= 0 && y < pm.getHeight()) {
            pm.drawPixel(x, y);
        }
    }

    private static void fillTriangle(Pixmap pm, int x1, int y1, int x2, int y2, int x3, int y3) {
        // sort by y
        if (y2 < y1) { int t=x1; x1=x2; x2=t; t=y1; y1=y2; y2=t; }
        if (y3 < y1) { int t=x1; x1=x3; x3=t; t=y1; y1=y3; y3=t; }
        if (y3 < y2) { int t=x2; x2=x3; x3=t; t=y2; y2=y3; y3=t; }

        // compute slopes
        float dx12 = (y2 - y1) != 0 ? (x2 - x1) / (float)(y2 - y1) : 0;
        float dx13 = (y3 - y1) != 0 ? (x3 - x1) / (float)(y3 - y1) : 0;
        float dx23 = (y3 - y2) != 0 ? (x3 - x2) / (float)(y3 - y2) : 0;

        float sx = x1;
        float ex = x1;

        // top -> middle
        for (int y = y1; y <= y2; y++) {
            drawHLine(pm, (int)sx, (int)ex, y);
            sx += dx13;
            ex += dx12;
        }

        // middle -> bottom
        sx = x1 + dx13 * (y2 - y1);
        ex = x2;
        for (int y = y2; y <= y3; y++) {
            drawHLine(pm, (int)sx, (int)ex, y);
            sx += dx13;
            ex += dx23;
        }
    }

    private static void drawHLine(Pixmap pm, int x1, int x2, int y) {
        if (y < 0 || y >= pm.getHeight()) return;
        if (x1 > x2) { int t=x1; x1=x2; x2=t; }
        x1 = Math.max(0, x1);
        x2 = Math.min(pm.getWidth()-1, x2);
        for (int x = x1; x <= x2; x++) pm.drawPixel(x, y);
    }
}
