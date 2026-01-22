package com.analiticasoft.hitraider.controllers;

import com.analiticasoft.hitraider.config.TransitionTuning;

public class TransitionController {

    public float fade = 0f;               // 0..1
    private boolean transitioning = false;
    private boolean fadeOut = false;
    private float timer = 0f;

    public void startFadeIn() {
        transitioning = true;
        fadeOut = false;
        timer = 0f;
        fade = 1f;
    }

    public void startFadeOut() {
        transitioning = true;
        fadeOut = true;
        timer = 0f;
        fade = 0f;
    }

    /** returns true if fade-out finished this frame */
    public boolean update(float delta) {
        if (!transitioning) return false;

        timer += delta;
        float t = Math.min(1f, timer / TransitionTuning.FADE_DURATION);

        if (!fadeOut) {
            fade = 1f - t;
            if (t >= 1f) transitioning = false;
            return false;
        } else {
            fade = t;
            if (t >= 1f) {
                transitioning = false;
                return true; // finished fade out
            }
            return false;
        }
    }

    public boolean isTransitioning() {
        return transitioning;
    }
}
