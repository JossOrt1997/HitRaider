package com.analiticasoft.hitraider.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;

/**
 * Safe loader:
 * - returns null if file missing
 * - never crashes game due to missing PNG
 */
public final class SafeAssets {
    private SafeAssets() {}

    public static boolean exists(String path) {
        try {
            FileHandle fh = Gdx.files.internal(path);
            return fh.exists();
        } catch (Throwable t) {
            return false;
        }
    }

    public static Texture textureOrNull(String path) {
        try {
            FileHandle fh = Gdx.files.internal(path);
            if (!fh.exists()) {
                if (Gdx.app != null) Gdx.app.log("ASSET", "Missing: " + path);
                return null;
            }
            return new Texture(fh);
        } catch (Throwable t) {
            return null;
        }
    }
}
