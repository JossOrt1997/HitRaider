package com.analiticasoft.hitraider.game;

import com.analiticasoft.hitraider.assets.SpritePaths;
import com.analiticasoft.hitraider.diagnostics.CrashReporter;
import com.analiticasoft.hitraider.diagnostics.CrashContext;
import com.analiticasoft.hitraider.diagnostics.ErrorGuard;
import com.analiticasoft.hitraider.screens.GameplayScreen;
import com.analiticasoft.hitraider.screens.MenuScreen;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;

public class HitRaiderGame extends Game {

    private ErrorGuard errorGuard;
    private boolean musicMuted = false;
    private Music mainTheme;

    // Simple main-thread task queue
    private final Deque<Runnable> mainThreadQueue = new ArrayDeque<>();

    @Override
    public void create() {
        CrashReporter reporter = new CrashReporter(new File("crash-reports"));
        errorGuard = new ErrorGuard(this, reporter);

        // Optional context provider (basic — GameplayScreen can set richer one later)
        errorGuard.setContextProvider(() -> new CrashContext()
            .put("phase", "unknown")
            .put("screen", String.valueOf(getScreen() != null ? getScreen().getClass().getName() : "null"))
            .put("fps", safeFps())
        );

        errorGuard.installGlobal();

        // ✅ Music setup
        try {
            mainTheme = Gdx.audio.newMusic(Gdx.files.internal(SpritePaths.MUSIC_OST));
            mainTheme.setLooping(true);
            mainTheme.setVolume(0.5f);
            if (!musicMuted) mainTheme.play();
        } catch (Exception e) {
            Gdx.app.error("AUDIO", "Could not load music: " + e.getMessage());
        }

        setScreen(new MenuScreen(this));
    }

    @Override
    public void render() {
        // Execute queued tasks first (safe screen switches)
        while (!mainThreadQueue.isEmpty()) {
            Runnable r = mainThreadQueue.pollFirst();
            if (r != null) r.run();
        }

        // Guard the whole frame
        errorGuard.guardFrame(super::render);
    }

    public void restartToGameplay() {
        setScreen(new GameplayScreen());
    }

    public boolean isMusicMuted() {
        return musicMuted;
    }

    public void setMusicMuted(boolean muted) {
        this.musicMuted = muted;
        if (mainTheme != null) {
            if (muted) mainTheme.pause();
            else mainTheme.play();
        }
    }

    public void toggleMusic() {
        setMusicMuted(!musicMuted);
    }

    public void postToMainThread(Runnable r) {
        mainThreadQueue.addLast(r);
    }

    private static int safeFps() {
        try { return Gdx.graphics.getFramesPerSecond(); }
        catch (Throwable ignored) { return -1; }
    }

    @Override
    public void dispose() {
        if (getScreen() != null) getScreen().dispose();
        if (mainTheme != null) mainTheme.dispose();
    }
}
