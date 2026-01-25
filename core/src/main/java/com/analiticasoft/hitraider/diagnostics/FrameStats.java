package com.analiticasoft.hitraider.diagnostics;

/**
 * FrameStats: mide estabilidad del frame sin overhead.
 * - avgDelta: EMA (exponential moving average)
 * - maxDelta: máximo reciente (ventana)
 * - spikeCount: cuantos frames superan un umbral en una ventana
 */
public class FrameStats {

    private float avgDelta = 0f;
    private float maxDelta = 0f;

    private int spikeCount = 0;
    private float spikeThreshold = 1f / 30f; // >33ms = spike

    private float windowTimer = 0f;
    private float windowSeconds = 5f;

    public void update(float delta) {
        if (avgDelta <= 0f) avgDelta = delta;
        else avgDelta = avgDelta * 0.95f + delta * 0.05f;

        if (delta > maxDelta) maxDelta = delta;
        if (delta > spikeThreshold) spikeCount++;

        windowTimer += delta;
        if (windowTimer >= windowSeconds) {
            windowTimer = 0f;
            maxDelta = 0f;
            spikeCount = 0;
        }
    }

    public float avgMs() { return avgDelta * 1000f; }
    public float maxMs() { return maxDelta * 1000f; }
    public int spikeCount() { return spikeCount; }

    public void setSpikeThresholdMs(float ms) {
        spikeThreshold = ms / 1000f;
    }

    public void setWindowSeconds(float seconds) {
        if (seconds > 0.5f) windowSeconds = seconds;
    }
}
