package com.analiticasoft.hitraider.combat;

public final class EnemyProfiles {
    private EnemyProfiles() {}

    public static class MeleeAIProfile {
        public final float chaseSpeed;      // m/s
        public final float aggroRangePx;
        public final float attackRangePx;

        public final float telegraphTime;
        public final float attackTime;
        public final float cooldownTime;

        public final AttackProfile attackProfile;

        public MeleeAIProfile(
            float chaseSpeed,
            float aggroRangePx,
            float attackRangePx,
            float telegraphTime,
            float attackTime,
            float cooldownTime,
            AttackProfile attackProfile
        ) {
            this.chaseSpeed = chaseSpeed;
            this.aggroRangePx = aggroRangePx;
            this.attackRangePx = attackRangePx;
            this.telegraphTime = telegraphTime;
            this.attackTime = attackTime;
            this.cooldownTime = cooldownTime;
            this.attackProfile = attackProfile;
        }
    }

    // “Rápido” (hostigador)
    public static final MeleeAIProfile ELDER_BLADE = new MeleeAIProfile(
        2.3f,
        260f,
        52f,
        0.14f,
        0.06f,
        0.28f,
        AttackProfiles.ENEMY_MELEE
    );

    // “Pesado” (guardia)
    public static final MeleeAIProfile WRAITH_GUARD = new MeleeAIProfile(
        1.4f,
        280f,
        62f,
        0.22f,
        0.08f,
        0.42f,
        AttackProfiles.ENEMY_MELEE
    );
}
