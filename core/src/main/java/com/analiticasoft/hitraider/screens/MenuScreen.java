package com.analiticasoft.hitraider.screens;

import com.analiticasoft.hitraider.assets.SafeAssets;
import com.analiticasoft.hitraider.assets.SpritePaths;
import com.analiticasoft.hitraider.game.HitRaiderGame;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class MenuScreen implements Screen {

    private final HitRaiderGame game;
    private Stage stage;
    private Skin skin;
    private SpriteBatch batch;
    private Texture background;

    public MenuScreen(HitRaiderGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        background = SafeAssets.textureOrNull(SpritePaths.SCREEN_MENU);
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        createSkin();

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        // Title
        Label titleLabel = new Label("HIT RAIDER", skin, "title");
        table.add(titleLabel).padBottom(50).row();

        // Iniciar button
        TextButton startButton = new TextButton("Iniciar", skin);
        startButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.restartToGameplay();
            }
        });
        table.add(startButton).fillX().uniformX().padBottom(10).row();

        // Music Mute button
        final TextButton musicButton = new TextButton(getMusicText(), skin);
        musicButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.toggleMusic();
                musicButton.setText(getMusicText());
            }
        });
        table.add(musicButton).fillX().uniformX().padBottom(10).row();

        // Salir button
        TextButton exitButton = new TextButton("Salir", skin);
        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });
        table.add(exitButton).fillX().uniformX();
    }

    private String getMusicText() {
        return game.isMusicMuted() ? "Musica: OFF" : "Musica: ON";
    }

    private void createSkin() {
        skin = new Skin();
        BitmapFont font = new BitmapFont(); // Default Arial font
        skin.add("default", font);

        // Generate a 1x1 white texture and store it in the skin
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        skin.add("white", new Texture(pixmap));

        // Configure a TextButtonStyle
        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.up = skin.newDrawable("white", Color.DARK_GRAY);
        textButtonStyle.down = skin.newDrawable("white", Color.LIGHT_GRAY);
        textButtonStyle.over = skin.newDrawable("white", Color.GRAY);
        textButtonStyle.font = skin.getFont("default");
        skin.add("default", textButtonStyle);

        // Label style
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = skin.getFont("default");
        labelStyle.fontColor = Color.WHITE;
        skin.add("default", labelStyle);

        // Title style
        Label.LabelStyle titleStyle = new Label.LabelStyle();
        titleStyle.font = skin.getFont("default");
        titleStyle.fontColor = Color.YELLOW;
        skin.add("title", titleStyle);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (background != null) {
            batch.begin();
            batch.draw(background, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            batch.end();
        }

        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
        batch.dispose();
        if (background != null) background.dispose();
    }
}
