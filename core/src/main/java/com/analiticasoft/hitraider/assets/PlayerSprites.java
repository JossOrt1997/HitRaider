package com.analiticasoft.hitraider.assets;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

/** Player sprite config + animations. */
public class PlayerSprites {

    // Visual tuning
    public float scale = 0.30f;
    public float feetOffsetPx = 26f;

    // Per-state offsets
    public float offIdle = 0f, offRun = 0f, offJump = 0f, offFall = 0f, offDash = 0f, offAttack = 0f, offShoot = 0f, offHurt = 0f, offDead = 0f;
    // Per-state scales (0 means use base scale)
    public float scIdle = 0f, scRun = 0f, scJump = 0f, scFall = 0f, scDash = 0f, scAttack = 0f, scShoot = 0f, scHurt = 0f, scDead = 0f;

    // fps per animation
    public float fpsIdle = 6f;
    public float fpsRun = 10f;
    public float fpsJump = 1f;
    public float fpsFall = 1f;
    public float fpsDash = 12f;
    public float fpsAttack = 24f;
    public float fpsShoot = 24f;
    public float fpsHurt = 12f;
    public float fpsDead = 6f;

    public final AnimSet idle = new AnimSet();
    public final AnimSet run = new AnimSet();
    public final AnimSet jump = new AnimSet();
    public final AnimSet fall = new AnimSet();
    public final AnimSet dash = new AnimSet();
    public final AnimSet attack = new AnimSet();
    public final AnimSet shoot = new AnimSet();
    public final AnimSet hurt = new AnimSet();
    public final AnimSet dead = new AnimSet();

    public enum State { IDLE, RUN, JUMP, FALL, DASH, ATTACK, SHOOT, HURT, DEAD }

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
        return s == State.ATTACK || s == State.SHOOT || s == State.HURT || s == State.DEAD;
    }

    private AnimSet pickSet(State state) {
        return switch (state) {
            case RUN -> run;
            case JUMP -> jump;
            case FALL -> fall;
            case DASH -> dash;
            case ATTACK -> attack;
            case SHOOT -> shoot;
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
            case SHOOT -> fpsShoot;
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
        dash.dispose(); attack.dispose(); shoot.dispose(); hurt.dispose(); dead.dispose();
    }

    public float getOffset(State state) {
        float off = switch (state) {
            case RUN -> offRun;
            case JUMP -> offJump;
            case FALL -> offFall;
            case DASH -> offDash;
            case ATTACK -> offAttack;
            case SHOOT -> offShoot;
            case HURT -> offHurt;
            case DEAD -> offDead;
            default -> offIdle;
        };
        return feetOffsetPx + off;
    }

    public float getScale(State state) {
        float s = switch (state) {
            case RUN -> scRun;
            case JUMP -> scJump;
            case FALL -> scFall;
            case DASH -> scDash;
            case ATTACK -> scAttack;
            case SHOOT -> scShoot;
            case HURT -> scHurt;
            case DEAD -> scDead;
            default -> scIdle;
        };
        return (s > 0f) ? s : scale;
    }
}
