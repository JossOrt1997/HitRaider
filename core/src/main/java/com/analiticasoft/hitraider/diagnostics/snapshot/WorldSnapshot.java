package com.analiticasoft.hitraider.diagnostics.snapshot;

public class WorldSnapshot {
    public int meleeEnemies;
    public int rangedEnemies;
    public int projectiles;
    public int pickups;
    public boolean doorClosed;

    @Override public String toString() {
        return "WorldSnapshot{" +
            "meleeEnemies=" + meleeEnemies +
            ", rangedEnemies=" + rangedEnemies +
            ", projectiles=" + projectiles +
            ", pickups=" + pickups +
            ", doorClosed=" + doorClosed +
            '}';
    }
}
