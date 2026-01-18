package com.analiticasoft.hitraider.render;

import java.util.EnumMap;

/**
 * Mantiene el tiempo por animación y entrega el índice de frame actual.
 * Es agnóstico a sprites: solo decide frames.
 */
public class CharacterAnimator {

    private final EnumMap<AnimKey, FrameDef[]> anims = new EnumMap<>(AnimKey.class);

    private AnimKey current = AnimKey.IDLE;
    private float t = 0f;

    public void define(AnimKey key, FrameDef[] frames) {
        anims.put(key, frames);
    }

    public void set(AnimKey key) {
        if (key != current) {
            current = key;
            t = 0f;
        }
    }

    public void update(float delta) {
        t += delta;
    }

    public AnimKey getCurrent() { return current; }

    public int getFrameIndex() {
        FrameDef[] frames = anims.get(current);
        if (frames == null || frames.length == 0) return 0;

        float total = 0f;
        for (FrameDef f : frames) total += f.duration;
        if (total <= 0f) return 0;

        float tt = t % total;
        float acc = 0f;
        for (int i = 0; i < frames.length; i++) {
            acc += frames[i].duration;
            if (tt <= acc) return i;
        }
        return frames.length - 1;
    }

    public FrameDef getFrame() {
        FrameDef[] frames = anims.get(current);
        if (frames == null || frames.length == 0) return null;
        return frames[getFrameIndex()];
    }
}
