package com.analiticasoft.hitraider.world;

public class MeleeEnemyProfile {

    public final boolean facingRight;

    // extensible
    public final int hp;
    public final int damage;

    public MeleeEnemyProfile(boolean facingRight, int hp, int damage) {
        this.facingRight = facingRight;
        this.hp = hp;
        this.damage = damage;
    }

    // Perfil default
    public static MeleeEnemyProfile DEFAULT =
        new MeleeEnemyProfile(true, 4, 1);
}
