package com.analiticasoft.hitraider.combat;

public class HealthComponent {
    private final int maxHp;
    private int hp;

    private float invulnTimer = 0f;
    private float flashTimer = 0f;

    // NEW: stun / stagger timer (seconds)
    private float stunTimer = 0f;

    public HealthComponent(int maxHp) {
        this.maxHp = maxHp;
        this.hp = maxHp;
    }

    public void update(float delta) {
        if (invulnTimer > 0f) invulnTimer = Math.max(0f, invulnTimer - delta);
        if (flashTimer > 0f) flashTimer = Math.max(0f, flashTimer - delta);
        if (stunTimer > 0f) stunTimer = Math.max(0f, stunTimer - delta);
    }

    public boolean isAlive() {
        return hp > 0;
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public boolean isInvulnerable() {
        return invulnTimer > 0f;
    }

    public boolean isFlashing() {
        return flashTimer > 0f;
    }

    public boolean isStunned() {
        return stunTimer > 0f;
    }

    public float getStunTimer() {
        return stunTimer;
    }

    /**
     * Devuelve true si aplicó daño (no estaba invulnerable).
     */
    public boolean tryDamage(int amount, float invulnSeconds, float flashSeconds, float stunSeconds) {
        if (!isAlive()) return false;
        if (isInvulnerable()) return false;

        hp -= amount;
        if (hp < 0) hp = 0;

        invulnTimer = invulnSeconds;
        flashTimer = flashSeconds;
        stunTimer = Math.max(stunTimer, stunSeconds);

        return true;
    }
}
