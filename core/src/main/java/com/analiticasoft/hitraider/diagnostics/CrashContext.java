package com.analiticasoft.hitraider.diagnostics;

import java.util.LinkedHashMap;
import java.util.Map;

public class CrashContext {
    private final Map<String, String> kv = new LinkedHashMap<>();

    public CrashContext put(String key, Object value) {
        kv.put(key, String.valueOf(value));
        return this;
    }

    public Map<String, String> data() {
        return kv;
    }
}
