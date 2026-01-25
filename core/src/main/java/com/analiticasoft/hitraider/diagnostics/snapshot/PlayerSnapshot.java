package com.analiticasoft.hitraider.diagnostics.snapshot;

public class PlayerSnapshot {
    public String state;
    public int hp;
    public int maxHp;
    public float xPx;
    public float yPx;
    public int facingDir;

    @Override public String toString() {
        return "PlayerSnapshot{" +
            "state='" + state + '\'' +
            ", hp=" + hp +
            ", maxHp=" + maxHp +
            ", xPx=" + xPx +
            ", yPx=" + yPx +
            ", facingDir=" + facingDir +
            '}';
    }
}
