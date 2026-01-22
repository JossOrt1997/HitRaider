package com.analiticasoft.hitraider.diagnostics;

import com.analiticasoft.hitraider.game.HitRaiderGame;
import com.analiticasoft.hitraider.screens.CrashScreen;

import java.io.File;

public class ErrorGuard {

    private final HitRaiderGame game;
    private final CrashReporter reporter;
    private CrashContextProvider contextProvider;

    public ErrorGuard(HitRaiderGame game, CrashReporter reporter) {
        this.game = game;
        this.reporter = reporter;
    }

    public void setContextProvider(CrashContextProvider provider) {
        this.contextProvider = provider;
    }

    public void installGlobal() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            handle("UncaughtException in thread " + thread.getName(), throwable);
        });
    }

    public void guardFrame(Runnable frameLogic) {
        try {
            frameLogic.run();
        } catch (Throwable t) {
            handle("Exception in main loop", t);
        }
    }

    private void handle(String title, Throwable t) {
        CrashContext ctx = (contextProvider != null) ? contextProvider.get() : new CrashContext().put("phase", title);
        File out = reporter.writeReport(t, ctx);

        String msg = (t.getMessage() == null) ? "(no message)" : t.getMessage();

        // Cambia pantalla a CrashScreen (thread-safe best effort)
        game.postToMainThread(() -> game.setScreen(new CrashScreen(
            t.getClass().getSimpleName(),
            msg,
            out.getAbsolutePath(),
            () -> game.restartToGameplay()
        )));
    }
}
