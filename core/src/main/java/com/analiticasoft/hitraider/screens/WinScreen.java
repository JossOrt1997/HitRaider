package com.analiticasoft.hitraider.screens;

import com.analiticasoft.hitraider.game.HitRaiderGame;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class WinScreen implements Screen {

    private final HitRaiderGame game;
    private Stage stage;
    private Skin skin;
    private Texture background;

    public WinScreen(HitRaiderGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        background = com.analiticasoft.hitraider.assets.SafeAssets.textureOrNull(com.analiticasoft.hitraider.assets.SpritePaths.SCREEN_WIN);

        createSkin();

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        Label winLabel = new Label("¡FELICIDADES!", skin, "title");
        table.add(winLabel).padBottom(20).row();

        Label subLabel = new Label("Has completado la demo", skin);
        table.add(subLabel).padBottom(50).row();

        TextButton menuButton = new TextButton("Volver al Menu", skin);
        menuButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MenuScreen(game));
            }
        });
        table.add(menuButton).fillX().uniformX().padBottom(10).row();

        TextButton exitButton = new TextButton("Salir", skin);
        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });
        table.add(exitButton).fillX().uniformX();
    }

    private void createSkin() {
        skin = new Skin();
        BitmapFont font = new BitmapFont();
        skin.add("default", font);

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        skin.add("white", new Texture(pixmap));

        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.up = skin.newDrawable("white", Color.DARK_GRAY);
        textButtonStyle.down = skin.newDrawable("white", Color.LIGHT_GRAY);
        textButtonStyle.over = skin.newDrawable("white", Color.GRAY);
        textButtonStyle.font = skin.getFont("default");
        skin.add("default", textButtonStyle);

        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = skin.getFont("default");
        labelStyle.fontColor = Color.WHITE;
        skin.add("default", labelStyle);

        Label.LabelStyle titleStyle = new Label.LabelStyle();
        titleStyle.font = skin.getFont("default");
        titleStyle.fontColor = Color.GOLD;
        skin.add("title", titleStyle);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.2f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (background != null) {
            stage.getBatch().begin();
            stage.getBatch().draw(background, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            stage.getBatch().end();
        }

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
        if (background != null) background.dispose();
    }
}
