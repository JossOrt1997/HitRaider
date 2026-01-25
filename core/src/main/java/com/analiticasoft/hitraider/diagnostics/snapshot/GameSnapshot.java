package com.analiticasoft.hitraider.diagnostics.snapshot;

public class GameSnapshot {
    public String phase; // "update", "render", etc.
    public RunSnapshot run;
    public PlayerSnapshot player;
    public WorldSnapshot world;

    @Override public String toString() {
        return "GameSnapshot{" +
            "phase='" + phase + '\'' +
            ", run=" + run +
            ", player=" + player +
            ", world=" + world +
            '}';
    }
}
