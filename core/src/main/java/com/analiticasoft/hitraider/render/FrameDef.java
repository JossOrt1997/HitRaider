package com.analiticasoft.hitraider.render;

import com.badlogic.gdx.utils.Array;

/**
 * Un "frame" de animación hecho de rectángulos.
 * Cada rect se define relativo al pivote del personaje (x,y) y se voltea con facing.
 */
public class FrameDef {

    public static class Rect {
        public final float x, y, w, h; // en pixeles, relativo al pivote
        public Rect(float x, float y, float w, float h) { this.x=x; this.y=y; this.w=w; this.h=h; }
    }

    public final Array<Rect> parts = new Array<>();
    public float duration; // seconds

    public FrameDef(float duration) {
        this.duration = duration;
    }

    public FrameDef add(float x, float y, float w, float h) {
        parts.add(new Rect(x, y, w, h));
        return this;
    }
}
