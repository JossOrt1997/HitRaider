package com.analiticasoft.hitraider.diagnostics;

public final class DebugValidators {
    private DebugValidators() {}

    public static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
