package com.analiticasoft.hitraider.render;

public class VisualStateSnapshot {
    public float xPx, yPx;
    public int facingDir = 1;

    public boolean flashing;
    public boolean telegraphing;
    public float telegraphAlpha; // 0..1

    public AnimKey animKey = AnimKey.IDLE;

    public VisualStateSnapshot setPos(float x, float y) { this.xPx = x; this.yPx = y; return this; }
}
