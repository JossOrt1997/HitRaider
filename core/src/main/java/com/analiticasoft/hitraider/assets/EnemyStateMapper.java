package com.analiticasoft.hitraider.assets;

public final class EnemyStateMapper {
    private EnemyStateMapper() {}

    public static EnemySprites.State map(String stateName) {
        if (stateName == null) return EnemySprites.State.IDLE;
        String s = stateName.toUpperCase();

        if (s.contains("DEAD")) return EnemySprites.State.DEAD;
        if (s.contains("HURT") || s.contains("STAGGER")) return EnemySprites.State.HURT;
        if (s.contains("TELEGRAPH")) return EnemySprites.State.TELEGRAPH;
        if (s.contains("ATTACK") || s.contains("SHOOT")) return EnemySprites.State.ATTACK;
        if (s.contains("CHASE") || s.contains("RUN") || s.contains("KITE")) return EnemySprites.State.RUN;

        return EnemySprites.State.IDLE;
    }
}
