package com.analiticasoft.hitraider.physics;

public final class PhysicsConstants {
    private PhysicsConstants() {}

    // Pixels Per Meter: 100px = 1m es cómodo en 2D
    public static final float PPM = 100f;

    public static float toMeters(float pixels) { return pixels / PPM; }
    public static float toPixels(float meters) { return meters * PPM; }
}
