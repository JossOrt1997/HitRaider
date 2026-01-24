package com.analiticasoft.hitraider.assets;

import com.analiticasoft.hitraider.entities.Player;

public final class PlayerStateMapper {
    private PlayerStateMapper() {}

    public static PlayerSprites.State map(Player p) {
        if (p == null) return PlayerSprites.State.IDLE;

        return switch (p.getState()) {
            case RUN -> PlayerSprites.State.RUN;
            case JUMP -> PlayerSprites.State.JUMP;
            case FALL -> PlayerSprites.State.FALL;
            case DASH -> PlayerSprites.State.DASH;
            case ATTACK -> PlayerSprites.State.ATTACK;
            case HURT -> PlayerSprites.State.HURT;
            case DEAD -> PlayerSprites.State.DEAD;
            case IDLE -> PlayerSprites.State.IDLE;
        };
    }
}
