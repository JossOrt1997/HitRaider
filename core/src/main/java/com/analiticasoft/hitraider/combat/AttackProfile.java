package com.analiticasoft.hitraider.combat;

/**
 * Datos de un ataque melee en pixeles. 100% reemplazable luego por “datos desde sprites”.
 * aimY: -1 abajo, 0 neutral, +1 arriba
 */
public class AttackProfile {
    public final float durationSec;
    public final int damage;

    // tamaño hitbox (px)
    public final float halfWpx;
    public final float halfHpx;

    // offset base (px)
    public final float offsetXpx;
    public final float offsetYpx;

    // offset si apuntas arriba/abajo
    public final float aimOffsetXpx;
    public final float aimUpOffsetYpx;
    public final float aimDownOffsetYpx;

    public AttackProfile(
        float durationSec,
        int damage,
        float halfWpx,
        float halfHpx,
        float offsetXpx,
        float offsetYpx,
        float aimOffsetXpx,
        float aimUpOffsetYpx,
        float aimDownOffsetYpx
    ) {
        this.durationSec = durationSec;
        this.damage = damage;
        this.halfWpx = halfWpx;
        this.halfHpx = halfHpx;
        this.offsetXpx = offsetXpx;
        this.offsetYpx = offsetYpx;
        this.aimOffsetXpx = aimOffsetXpx;
        this.aimUpOffsetYpx = aimUpOffsetYpx;
        this.aimDownOffsetYpx = aimDownOffsetYpx;
    }

    public float getOffsetXpx(int facingDir, int aimY) {
        float x = offsetXpx;
        if (aimY != 0) x = aimOffsetXpx;
        return x * facingDir;
    }

    public float getOffsetYpx(int aimY) {
        if (aimY == 1) return aimUpOffsetYpx;
        if (aimY == -1) return aimDownOffsetYpx;
        return offsetYpx;
    }
}
