package com.analiticasoft.hitraider.controllers;

import com.badlogic.gdx.graphics.OrthographicCamera;

public class ShakeController {

    private float timer = 0f;
    private float intensity = 0f;
    private float seed = 0f;

    public void start(float duration, float intensity) {
        this.timer = Math.max(this.timer, duration);
        this.intensity = Math.max(this.intensity, intensity);
    }

    public void update(float delta) {
        if (timer > 0f) {
            timer = Math.max(0f, timer - delta);
            seed += 31.7f * delta;
        }
    }

    public void apply(OrthographicCamera camera) {
        if (timer <= 0f) return;
        float sx = (float) Math.sin(seed * 17.0) * intensity;
        float sy = (float) Math.cos(seed * 23.0) * intensity;
        camera.position.x += sx;
        camera.position.y += sy;
    }

    public void reset() {
        timer = 0f;
        intensity = 0f;
        seed = 0f;
    }
}
