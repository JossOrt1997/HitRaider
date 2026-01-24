package com.analiticasoft.hitraider.assets;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

/** Holds frames + owning textures to dispose safely. */
public class AnimSet {
    public final Array<Texture> textures = new Array<>();
    public final Array<TextureRegion> frames = new Array<>();

    public void add(Texture tex) {
        textures.add(tex);
        frames.add(new TextureRegion(tex));
    }

    public TextureRegion frame(int index) {
        if (frames.size == 0) return null;
        if (index < 0) index = 0;
        if (index >= frames.size) index = frames.size - 1;
        return frames.get(index);
    }

    public void dispose() {
        for (Texture t : textures) t.dispose();
        textures.clear();
        frames.clear();
    }
}
