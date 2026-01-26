package com.analiticasoft.hitraider.assets;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

/** Player sprite config + animations. */
public class PlayerSprites {

    // Visual tuning
    public float scale = 0.30f;
    public float feetOffsetPx = 48f;

    // fps per animation
    public float fpsIdle = 6f;
    public float fpsRun = 10f;
    public float fpsJump = 1f;
    public float fpsFall = 1f;
    public float fpsDash = 12f;
    public float fpsAttack = 12f;
    public float fpsHurt = 12f;
    public float fpsDead = 6f;

    public final AnimSet idle = new AnimSet();
    public final AnimSet run = new AnimSet();
    public final AnimSet jump = new AnimSet();
    public final AnimSet fall = new AnimSet();
    public final AnimSet dash = new AnimSet();
    public final AnimSet attack = new AnimSet();
    public final AnimSet hurt = new AnimSet();
    public final AnimSet dead = new AnimSet();

    public enum State { IDLE, RUN, JUMP, FALL, DASH, ATTACK, HURT, DEAD }

    public TextureRegion get(State state, float timeSec) {
        AnimSet set = pickSet(state);
        if (set.frames.size == 0 && idle.frames.size > 0) set = idle;
        if (set.frames.size == 0) return null;

        float fps = pickFps(state);
        int idx = loopIndex(timeSec, fps, set.frames.size);
        return set.frame(idx);
    }

    /** For one-shot animations: sticks to last frame */
    public TextureRegion getOnce(State state, float timeSec) {
        AnimSet set = pickSet(state);
        if (set.frames.size == 0 && idle.frames.size > 0) set = idle;
        if (set.frames.size == 0) return null;

        float fps = pickFps(state);
        int idx = (int)Math.floor(timeSec * fps);
        if (idx < 0) idx = 0;
        if (idx >= set.frames.size) idx = set.frames.size - 1;
        return set.frame(idx);
    }

    public static boolean isOneShot(State s) {
        return s == State.ATTACK || s == State.HURT || s == State.DEAD;
    }

    private AnimSet pickSet(State state) {
        return switch (state) {
            case RUN -> run;
            case JUMP -> jump;
            case FALL -> fall;
            case DASH -> dash;
            case ATTACK -> attack;
            case HURT -> hurt;
            case DEAD -> dead;
            default -> idle;
        };
    }

    private float pickFps(State state) {
        return switch (state) {
            case RUN -> fpsRun;
            case JUMP -> fpsJump;
            case FALL -> fpsFall;
            case DASH -> fpsDash;
            case ATTACK -> fpsAttack;
            case HURT -> fpsHurt;
            case DEAD -> fpsDead;
            default -> fpsIdle;
        };
    }

    private int loopIndex(float timeSec, float fps, int size) {
        if (size <= 1 || fps <= 0f) return 0;
        int idx = (int)Math.floor(timeSec * fps);
        idx = idx % size;
        if (idx < 0) idx += size;
        return idx;
    }

    public void dispose() {
        idle.dispose(); run.dispose(); jump.dispose(); fall.dispose();
        dash.dispose(); attack.dispose(); hurt.dispose(); dead.dispose();
    }
}
