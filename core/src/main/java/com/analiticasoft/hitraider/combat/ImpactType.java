package com.analiticasoft.hitraider.combat;

public enum ImpactType {
    PLAYER_HURT,          // te pegan a ti (fuerte)
    ENEMY_HURT,           // tú le pegas a un enemigo (medio)
    PROJECTILE_HIT_ENEMY, // proyectil pega enemigo (medio-bajo)
    PROJECTILE_WORLD,     // proyectil pega mundo (leve)
    MELEE_WORLD           // melee pega mundo (como ENEMY_HURT)
}
