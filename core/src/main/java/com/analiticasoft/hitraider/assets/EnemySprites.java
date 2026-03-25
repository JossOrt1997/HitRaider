package com.analiticasoft.hitraider.assets;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class EnemySprites {

    public float scale = 0.16f;
    public float feetOffsetPx = 26f;

    // Per-state offsets
    public float offIdle = 0f, offRun = 0f, offTelegraph = 0f, offAttack = 0f, offHurt = 0f, offDead = 0f;
    // Per-state scales (0 means use base scale)
    public float scIdle = 0f, scRun = 0f, scTelegraph = 0f, scAttack = 0f, scHurt = 0f, scDead = 0f;

    public float fpsIdle = 6f;
    public float fpsRun = 10f;
    public float fpsTelegraph = 10f;
    public float fpsAttack = 24f;
    public float fpsHurt = 12f;
    public float fpsDead = 6f;

    public final AnimSet idle = new AnimSet();
    public final AnimSet run = new AnimSet();
    public final AnimSet telegraph = new AnimSet();
    public final AnimSet attack = new AnimSet();
    public final AnimSet hurt = new AnimSet();
    public final AnimSet dead = new AnimSet();

    public enum State { IDLE, RUN, TELEGRAPH, ATTACK, HURT, DEAD }

    public TextureRegion get(State state, float timeSec) {
        AnimSet set = pickSet(state);
        if (set.frames.size == 0 && idle.frames.size > 0) set = idle;
        if (set.frames.size == 0) return null;

        float fps = pickFps(state);
        int idx = loopIndex(timeSec, fps, set.frames.size);
        return set.frame(idx);
    }

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
            case TELEGRAPH -> telegraph;
            case ATTACK -> attack;
            case HURT -> hurt;
            case DEAD -> dead;
            default -> idle;
        };
    }

    private float pickFps(State state) {
        return switch (state) {
            case RUN -> fpsRun;
            case TELEGRAPH -> fpsTelegraph;
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
        idle.dispose(); run.dispose(); telegraph.dispose();
        attack.dispose(); hurt.dispose(); dead.dispose();
    }

    public float getOffset(State state) {
        float off = switch (state) {
            case RUN -> offRun;
            case TELEGRAPH -> offTelegraph;
            case ATTACK -> offAttack;
            case HURT -> offHurt;
            case DEAD -> offDead;
            default -> offIdle;
        };
        return feetOffsetPx + off;
    }

    public float getScale(State state) {
        float s = switch (state) {
            case RUN -> scRun;
            case TELEGRAPH -> scTelegraph;
            case ATTACK -> scAttack;
            case HURT -> scHurt;
            case DEAD -> scDead;
            default -> scIdle;
        };
        return (s > 0f) ? s : scale;
    }
}
