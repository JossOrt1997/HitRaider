package com.analiticasoft.hitraider.gameplay.render;

import com.analiticasoft.hitraider.config.GameConfig;
import com.analiticasoft.hitraider.config.UiTuning;
import com.analiticasoft.hitraider.gameplay.GameplayContext;
import com.analiticasoft.hitraider.ui.HudPainter;
import com.analiticasoft.hitraider.world.RoomInstance;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class UiRenderSystem {

    private final HudPainter hudPainter = new HudPainter();

    public void renderUI(GameplayContext ctx, ShapeRenderer shapes, SpriteBatch batch, BitmapFont font,
                         boolean hudEssentialOn, boolean hudInfoOn,
                         float weaponCooldown, String weaponLabel,
                         String ammoLabel,
                         boolean strictFreezeOnFail) {

        shapes.setProjectionMatrix(ctx.uiCamera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        if (hudEssentialOn) {
            float baseX = UiTuning.HUD_BASE_X;
            float baseY = GameConfig.VIRTUAL_H - UiTuning.HUD_BASE_Y_OFFSET;

            float circleR = 16f;
            float circleCx = baseX + circleR;
            float circleCy = baseY - 10f;

            shapes.setColor(0.05f, 0.05f, 0.06f, 1f);
            shapes.circle(circleCx, circleCy, circleR + 2f, 24);
            shapes.setColor(0.12f, 0.12f, 0.14f, 1f);
            shapes.circle(circleCx, circleCy, circleR, 24);

            float barsX = circleCx + circleR + 10f;
            float barW = 170f;
            float barH = 7f;
            float gap = 4f;

            int hp = ctx.run.player.getHealth().getHp();
            int hpMax = ctx.run.player.getHealth().getMaxHp();
            float hpPct = (hpMax <= 0) ? 0f : (hp / (float) hpMax);
            hpPct = Math.max(0f, Math.min(1f, hpPct));

            shapes.setColor(0.05f, 0.05f, 0.06f, 1f);
            shapes.rect(barsX, baseY - 0f, barW, barH);
            shapes.rect(barsX, baseY - (barH + gap), barW, barH);
            shapes.rect(barsX, baseY - 2f * (barH + gap), barW, barH);

            shapes.setColor(0.55f, 0.12f, 0.12f, 1f);
            shapes.rect(barsX, baseY - 0f, barW * hpPct, barH);

            shapes.setColor(0.15f, 0.55f, 0.18f, 1f);
            shapes.rect(barsX, baseY - (barH + gap), barW, barH);

            shapes.setColor(0.14f, 0.25f, 0.65f, 1f);
            shapes.rect(barsX, baseY - 2f * (barH + gap), barW, barH);
        }

        if (ctx.transition.fade > 0f) {
            shapes.setColor(0f, 0f, 0f, ctx.transition.fade);
            shapes.rect(0, 0, GameConfig.VIRTUAL_W, GameConfig.VIRTUAL_H);
        }

        shapes.end();

        batch.setProjectionMatrix(ctx.uiCamera.combined);
        batch.begin();

        if (hudEssentialOn) {
            TextureRegion legion = ctx.sprites.legionSalamanders();
            if (legion != null) {
                float baseX = UiTuning.HUD_BASE_X;
                float baseY = GameConfig.VIRTUAL_H - UiTuning.HUD_BASE_Y_OFFSET;
                float circleR = 16f;
                float circleCx = baseX + circleR;
                float circleCy = baseY - 10f;
                float size = 32f;
                batch.draw(legion, circleCx - size / 2f, circleCy - size / 2f, size, size);
            }

            int hp = ctx.run.player.getHealth().getHp();
            int maxHp = ctx.run.player.getHealth().getMaxHp();
            int enemiesAlive = ctx.run.meleeEnemies.size + ctx.run.rangedEnemies.size;

            font.draw(batch, "HP " + hp + "/" + maxHp, 14f, GameConfig.VIRTUAL_H - 60f);
            font.draw(batch, "Enemies: " + enemiesAlive, 14f, GameConfig.VIRTUAL_H - 78f);

            float x = GameConfig.VIRTUAL_W - UiTuning.WEAPON_PANEL_W - UiTuning.WEAPON_PANEL_MARGIN;
            float y = UiTuning.WEAPON_PANEL_Y;

            font.draw(batch, "WEAPON", x + 10f, y + 50f);
            font.draw(batch, "Current: " + weaponLabel, x + 10f, y + 34f);
            font.draw(batch, String.format("CD: %.2f", weaponCooldown), x + 140f, y + 34f);
            font.draw(batch, ammoLabel, x + 10f, y + 18f);
            font.draw(batch, "[1] Hammer  [2] Bolter  [L] Reload", x + 10f, y + 2f);
        }

        if (hudInfoOn) {
            float x = GameConfig.VIRTUAL_W - UiTuning.INFO_X_OFFSET;
            float y = GameConfig.VIRTUAL_H - UiTuning.INFO_Y;

            int fps = Gdx.graphics.getFramesPerSecond();
            RoomInstance room = ctx.run.run.current();

            font.draw(batch, "DEBUG / INFO", x, y); y -= 18f;
            font.draw(batch, "FPS: " + fps, x, y); y -= 18f;
            font.draw(batch, String.format("Frame avg: %.1fms max: %.1fms spikes:%d",
                ctx.frameStats.avgMs(), ctx.frameStats.maxMs(), ctx.frameStats.spikeCount()), x, y); y -= 18f;

            font.draw(batch, "Seed: " + ctx.run.run.seed, x, y); y -= 18f;
            font.draw(batch, "Room: " + (ctx.run.run.index + 1) + "/" + ctx.run.run.totalRooms + " [" + room.type + "]", x, y); y -= 18f;
            font.draw(batch, "Tpl: " + room.template.id + " | Budget: " + room.budget, x, y); y -= 18f;

            font.draw(batch, "STRICT: " + (ctx.strictModeOn ? "ON" : "OFF")
                + " freeze=" + (strictFreezeOnFail ? "ON" : "OFF")
                + " frozen=" + (ctx.frozenByStrict ? "YES" : "NO"), x, y); y -= 18f;

            if (ctx.lastStrictError != null) {
                font.draw(batch, "Last strict: " + ctx.lastStrictError, x, y); y -= 18f;
            }

            font.draw(batch, "F5 reload | F6 snapshot | F7 strict | F8 freeze | F9 unfreeze", x, 20f);
        }

        batch.end();
    }

    public void renderCriticalOverlay(GameplayContext ctx, SpriteBatch batch, BitmapFont font) {
        if (ctx.run.player == null) return;

        if (!ctx.run.player.isAlive()) {
            batch.setProjectionMatrix(ctx.uiCamera.combined);
            batch.begin();
            hudPainter.drawFadingText(batch, font, "YOU DIED - Press R to restart run", 200, 200, 1f);
            batch.end();
        }
    }
}
