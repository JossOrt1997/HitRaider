package com.analiticasoft.hitraider.render;

import com.analiticasoft.hitraider.entities.MeleeEnemy;
import com.analiticasoft.hitraider.entities.Player;

public final class VisualMapper {
    private VisualMapper() {}

    public static AnimKey playerKey(Player p) {
        return switch (p.getState()) {
            case RUN -> AnimKey.RUN;
            case JUMP -> AnimKey.JUMP;
            case FALL -> AnimKey.FALL;
            case DASH -> AnimKey.DASH;
            case ATTACK -> AnimKey.ATTACK_ACTIVE;
            case HURT -> AnimKey.HURT;
            default -> AnimKey.IDLE;
        };
    }

    public static AnimKey enemyKey(MeleeEnemy e) {
        return switch (e.getState()) {
            case CHASE -> AnimKey.ENEMY_CHASE;
            case TELEGRAPH -> AnimKey.ENEMY_TELEGRAPH;
            case ATTACK -> AnimKey.ENEMY_ATTACK;
            case STAGGER -> AnimKey.ENEMY_STAGGER;
            default -> AnimKey.ENEMY_IDLE;
        };
    }
}
