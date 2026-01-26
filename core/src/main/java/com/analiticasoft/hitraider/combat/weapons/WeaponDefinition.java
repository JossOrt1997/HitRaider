package com.analiticasoft.hitraider.combat.weapons;

public final class WeaponDefinition {

    public final WeaponType type;

    // Core
    public final boolean melee;
    public final int baseDamage;
    public final float cooldownSec;

    // Melee (kept for future; current CombatSystem uses its own duration internally)
    public final float meleeDurationSec;

    // Ranged
    public final float projectileSpeedMps;
    public final float projectileLifetimeSec;
    public final int basePierce;

    private WeaponDefinition(Builder b) {
        this.type = b.type;
        this.melee = b.melee;
        this.baseDamage = b.baseDamage;
        this.cooldownSec = b.cooldownSec;
        this.meleeDurationSec = b.meleeDurationSec;
        this.projectileSpeedMps = b.projectileSpeedMps;
        this.projectileLifetimeSec = b.projectileLifetimeSec;
        this.basePierce = b.basePierce;
    }

    public static Builder builder(WeaponType type) { return new Builder(type); }

    public static final class Builder {
        private final WeaponType type;

        private boolean melee;
        private int baseDamage;
        private float cooldownSec;

        private float meleeDurationSec = 0.10f;

        private float projectileSpeedMps = 0f;
        private float projectileLifetimeSec = 1.2f;
        private int basePierce = 0;

        private Builder(WeaponType type) {
            this.type = type;
        }

        public Builder melee(boolean v) { this.melee = v; return this; }
        public Builder baseDamage(int v) { this.baseDamage = v; return this; }
        public Builder cooldownSec(float v) { this.cooldownSec = v; return this; }

        public Builder meleeDurationSec(float v) { this.meleeDurationSec = v; return this; }

        public Builder projectileSpeedMps(float v) { this.projectileSpeedMps = v; return this; }
        public Builder projectileLifetimeSec(float v) { this.projectileLifetimeSec = v; return this; }
        public Builder basePierce(int v) { this.basePierce = v; return this; }

        public WeaponDefinition build() { return new WeaponDefinition(this); }
    }
}
