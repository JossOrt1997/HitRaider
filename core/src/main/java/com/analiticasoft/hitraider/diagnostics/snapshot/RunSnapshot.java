package com.analiticasoft.hitraider.diagnostics.snapshot;

public class RunSnapshot {
    public long seed;
    public int index;
    public int total;
    public String roomType;
    public String templateId;
    public int budget;
    public int planMelee;
    public int planRanged;

    @Override public String toString() {
        return "RunSnapshot{" +
            "seed=" + seed +
            ", index=" + index +
            ", total=" + total +
            ", roomType='" + roomType + '\'' +
            ", templateId='" + templateId + '\'' +
            ", budget=" + budget +
            ", planMelee=" + planMelee +
            ", planRanged=" + planRanged +
            '}';
    }
}
