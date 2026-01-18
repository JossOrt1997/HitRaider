package com.analiticasoft.hitraider.render;

public class DebugAnimLibrary {

    // Player: pivot será "pies" (x, y) para dibujar fácil
    public static void definePlayer(CharacterAnimator a) {
        // Idle (2 frames)
        a.define(AnimKey.IDLE, new FrameDef[] {
            new FrameDef(0.22f).add(-10, 0, 20, 32),             // torso
            new FrameDef(0.22f).add(-10, 0, 20, 32).add(-9, -2, 18, 2) // pequeña base
        });

        // Run (4 frames) – simulación de piernas cambiando (rectángulos inferiores)
        a.define(AnimKey.RUN, new FrameDef[] {
            new FrameDef(0.10f).add(-10, 0, 20, 26).add(-10, 26, 20, 6),
            new FrameDef(0.10f).add(-10, 0, 20, 24).add(-10, 24, 20, 8),
            new FrameDef(0.10f).add(-10, 0, 20, 26).add(-10, 26, 20, 6),
            new FrameDef(0.10f).add(-10, 0, 20, 24).add(-10, 24, 20, 8)
        });

        // Jump/Fall (1 frame cada uno)
        a.define(AnimKey.JUMP, new FrameDef[] {
            new FrameDef(0.20f).add(-10, 0, 20, 32).add(-6, 22, 12, 4) // compact
        });
        a.define(AnimKey.FALL, new FrameDef[] {
            new FrameDef(0.20f).add(-10, 0, 20, 32).add(-8, 18, 16, 4) // stretched
        });

        // Dash (2 frames) – cuerpo “estirado”
        a.define(AnimKey.DASH, new FrameDef[] {
            new FrameDef(0.06f).add(-14, 4, 28, 24),
            new FrameDef(0.06f).add(-16, 6, 32, 20)
        });

        // Attack: Startup / Active / Recovery (cada uno con 2 frames simples)
        a.define(AnimKey.ATTACK_STARTUP, new FrameDef[] {
            new FrameDef(0.06f).add(-10, 0, 20, 32).add(8, 16, 10, 4),   // brazo atrás
            new FrameDef(0.06f).add(-10, 0, 20, 32).add(6, 14, 12, 6)
        });

        a.define(AnimKey.ATTACK_ACTIVE, new FrameDef[] {
            new FrameDef(0.06f).add(-10, 0, 20, 32).add(10, 14, 16, 6),  // swing forward
            new FrameDef(0.06f).add(-10, 0, 20, 32).add(12, 14, 18, 6)
        });

        a.define(AnimKey.ATTACK_RECOVERY, new FrameDef[] {
            new FrameDef(0.07f).add(-10, 0, 20, 32).add(8, 14, 10, 4),
            new FrameDef(0.07f).add(-10, 0, 20, 32)
        });

        // Hurt (1 frame)
        a.define(AnimKey.HURT, new FrameDef[] {
            new FrameDef(0.12f).add(-10, 0, 20, 32).add(-12, 10, 24, 4)
        });
    }

    // Enemy: pivot también “pies”
    public static void defineMeleeEnemy(CharacterAnimator a) {
        a.define(AnimKey.ENEMY_IDLE, new FrameDef[] {
            new FrameDef(0.24f).add(-10, 0, 20, 28),
            new FrameDef(0.24f).add(-10, 0, 20, 28).add(-10, 26, 20, 2)
        });

        a.define(AnimKey.ENEMY_CHASE, new FrameDef[] {
            new FrameDef(0.12f).add(-10, 0, 20, 28).add(-12, 10, 24, 3),
            new FrameDef(0.12f).add(-10, 0, 20, 28).add(-14, 12, 28, 3)
        });

        a.define(AnimKey.ENEMY_TELEGRAPH, new FrameDef[] {
            new FrameDef(0.18f).add(-10, 0, 20, 28).add(8, 14, 10, 4) // “arma” levantada
        });

        a.define(AnimKey.ENEMY_ATTACK, new FrameDef[] {
            new FrameDef(0.06f).add(-10, 0, 20, 28).add(10, 12, 16, 6)
        });

        a.define(AnimKey.ENEMY_STAGGER, new FrameDef[] {
            new FrameDef(0.10f).add(-10, 0, 20, 28).add(-12, 8, 24, 4)
        });
    }
}
