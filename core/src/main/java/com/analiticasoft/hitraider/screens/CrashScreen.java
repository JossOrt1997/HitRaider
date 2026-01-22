package com.analiticasoft.hitraider.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class CrashScreen implements Screen {

    private final String title;
    private final String message;
    private final String reportPath;
    private final Runnable onRestart;

    private SpriteBatch batch;
    private BitmapFont font;

    public CrashScreen(String title, String message, String reportPath, Runnable onRestart) {
        this.title = title;
        this.message = message;
        this.reportPath = reportPath;
        this.onRestart = onRestart;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        font = new BitmapFont();
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.R) && onRestart != null) {
            onRestart.run();
            return;
        }

        Gdx.gl.glClearColor(0.05f, 0.05f, 0.06f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        float y = Gdx.graphics.getHeight() - 30;

        font.draw(batch, "=== HIT-RAIDER ERROR ===", 30, y); y -= 25;
        font.draw(batch, "Type: " + title, 30, y); y -= 25;
        font.draw(batch, "Message: " + message, 30, y); y -= 25;
        font.draw(batch, "Report saved at:", 30, y); y -= 25;
        font.draw(batch, reportPath, 30, y); y -= 40;

        font.draw(batch, "Press R to restart (safe) or ESC to quit.", 30, y);

        batch.end();
    }

    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (font != null) font.dispose();
    }
}
