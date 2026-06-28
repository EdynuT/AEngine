package com.aengine.ecs.components;

import com.aengine.graphics.TextureAPI;
import org.joml.Vector4f;

public final class SpriteComponent {

    public TextureAPI texture = null;
    public final Vector4f color = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);

    public SpriteComponent() {}

    public SpriteComponent(Vector4f color) {
        this.color.set(color);
    }

    public SpriteComponent(TextureAPI texture) {
        this.texture = texture;
    }
}
