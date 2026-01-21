package com.analiticasoft.hitraider.physics;

public final class PhysicsConstants {
    private PhysicsConstants() {}
    public static final float PPM = 100f;
    public static float toMeters(float px) { return px / PPM; }
    public static float toPixels(float m) { return m * PPM; }
}
