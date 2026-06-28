package com.aengine.ecs.systems;

import com.aengine.ecs.ComponentPool;
import com.aengine.ecs.Registry;
import com.aengine.ecs.System;
import com.aengine.ecs.components.SpriteComponent;
import com.aengine.ecs.components.TransformComponent;
import com.aengine.graphics.Renderer2D;
import com.aengine.utils.Logger;
import org.joml.Vector2f;

public final class RenderSystem extends System {

    // Thread-local temporary buffers to eliminate allocation spikes inside the render loop
    private final Vector2f tempPosition = new Vector2f();
    private final Vector2f tempScale = new Vector2f();

    @Override
    public void update(Registry registry, float deltaTime) {
        ComponentPool<SpriteComponent> spritePool = registry.getPool(SpriteComponent.class);
        ComponentPool<TransformComponent> transformPool = registry.getPool(TransformComponent.class);

        if (spritePool == null || transformPool == null || spritePool.size() == 0) {
            return;
        }

        SpriteComponent[] sprites = spritePool.getRawComponents();
        int[] denseToEntity = spritePool.getRawDenseToEntity();
        int totalElements = spritePool.size();

        for (int i = 0; i < totalElements; i++) {
            int entityID = denseToEntity[i];
            SpriteComponent sprite = sprites[i];
            TransformComponent transform = transformPool.get(entityID);
            
            if (transform == null) {
                Logger.warn(Logger.System.RENDERER, "Entity ID: %d possesses a SpriteComponent but lacks a TransformComponent. Skipping draw.", entityID);
                continue;
            }

            // Downsample Vector3f coordinates to match Renderer2D expectations
            tempPosition.set(transform.position.x, transform.position.y);
            tempScale.set(transform.scale.x, transform.scale.y);

            if (sprite.texture != null) {
                Renderer2D.drawQuad(tempPosition, tempScale, sprite.texture);
                Logger.trace(Logger.System.RENDERER, "RenderSystem submitted textured quad for Entity ID: %d", entityID);
            } else {
                Renderer2D.drawQuad(tempPosition, tempScale, sprite.color);
                Logger.trace(Logger.System.RENDERER, "RenderSystem submitted colored quad for Entity ID: %d", entityID);
            }
        }
    }
}
