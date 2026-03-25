package com.analiticasoft.hitraider.config;

public final class VisualTuning {
    private VisualTuning() {}

    // ✅ PLAYER Tuning
    public static final float PLAYER_BASE_SCALE = 0.30f;
    public static final float PLAYER_FEET_OFFSET = 26f;

    public static final float P_OFF_IDLE = 0f;    public static final float P_SC_IDLE = 0f;
    public static final float P_OFF_RUN = 0f;     public static final float P_SC_RUN = 0f;
    public static final float P_OFF_JUMP = 0f;    public static final float P_SC_JUMP = 0f;
    public static final float P_OFF_FALL = 0f;    public static final float P_SC_FALL = 0f;
    public static final float P_OFF_DASH = 2f;   public static final float P_SC_DASH = 0.20f;
    public static final float P_OFF_ATTACK = 28f; public static final float P_SC_ATTACK = 0.21f;
    public static final float P_OFF_SHOOT = 2f;  public static final float P_SC_SHOOT = 0.16f;
    public static final float P_OFF_HURT = 2f;   public static final float P_SC_HURT = 0.16f;
    public static final float P_OFF_DEAD = 2f;   public static final float P_SC_DEAD = 0.16f;

    // ✅ ENEMY MELEE Tuning
    public static final float EM_BASE_SCALE = 0.16f;
    public static final float EM_FEET_OFFSET = 26f;

    public static final float EM_OFF_IDLE = 4f;    public static final float EM_SC_IDLE = 0.16f;
    public static final float EM_OFF_RUN = 2f;     public static final float EM_SC_RUN = 0f;
    public static final float EM_OFF_ATTACK = 26f; public static final float EM_SC_ATTACK = 0.21f;
    public static final float EM_OFF_HURT = 2f;   public static final float EM_SC_HURT = 0.16f;
    public static final float EM_OFF_DEAD = 33f;   public static final float EM_SC_DEAD = 0.18f;

    // ✅ ENEMY RANGED Tuning
    public static final float ER_BASE_SCALE = 0.16f;
    public static final float ER_FEET_OFFSET = 28f;

    public static final float ER_OFF_IDLE = 4f;    public static final float ER_SC_IDLE = 0.16f;
    public static final float ER_OFF_RUN = 2f;     public static final float ER_SC_RUN = 0f;
    public static final float ER_OFF_ATTACK = 20f; public static final float ER_SC_ATTACK = 0.17f;
    public static final float ER_OFF_HURT = 2f;   public static final float ER_SC_HURT = 0.16f;
    public static final float ER_OFF_DEAD = 33f;   public static final float ER_SC_DEAD = 0.18f;

    // ✅ ANIMATION FPS
    public static final float FPS_IDLE = 10f; // More frames = faster anim
    public static final float FPS_RUN = 14f;
    public static final float FPS_ATTACK = 36f; // More frames = faster sweep
    public static final float FPS_DEAD = 8f;   // Slower death anim so we can see it!

    public static final float DEATH_PAUSE = 1.2f; // Time to wait before removing dead enemy
}
