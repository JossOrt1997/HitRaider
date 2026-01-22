package com.analiticasoft.hitraider.ui;

import com.analiticasoft.hitraider.config.GameConfig;
import com.analiticasoft.hitraider.relics.RelicManager;
import com.analiticasoft.hitraider.world.RoomInstance;
import com.analiticasoft.hitraider.world.RunManager;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class HudRenderer {

    public void drawHeader(SpriteBatch batch, BitmapFont font, RunManager run, RoomInstance room) {
        font.draw(batch, "Seed: " + run.seed, 10, GameConfig.VIRTUAL_H - 10);
        font.draw(batch, "Room: " + (run.index + 1) + "/" + run.totalRooms + " [" + room.type + "] tpl=" + room.template.id, 10, GameConfig.VIRTUAL_H - 42);
        font.draw(batch, "Budget: " + room.budget + " M:" + room.meleeCount + " R:" + room.rangedCount, 10, GameConfig.VIRTUAL_H - 58);
    }

    public void drawRelics(SpriteBatch batch, BitmapFont font, RelicManager relics) {
        font.draw(batch, "Relics: " + relics.getOwned().size, 10, GameConfig.VIRTUAL_H - 76);
        font.draw(batch, "+ProjDmg: +" + relics.getBonusProjectileDamage(), 120, GameConfig.VIRTUAL_H - 76);
        font.draw(batch, "FireRate x" + String.format("%.2f", (1f / relics.getFireRateMultiplier())), 260, GameConfig.VIRTUAL_H - 76);
        font.draw(batch, "Pierce: " + relics.getPiercingShots(), 430, GameConfig.VIRTUAL_H - 76);
        font.draw(batch, "Lifesteal: " + relics.getLifestealEveryHits(), 510, GameConfig.VIRTUAL_H - 76);
    }
}
