package com.aengine.ecs.components;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class TransformComponent {

    // Raw, primitive aligned vector structures for direct CPU L1 cache streaming
    public final Vector3f position = new Vector3f(0.0f, 0.0f, 0.0f);
    public final Vector3f rotation = new Vector3f(0.0f, 0.0f, 0.0f);
    public final Vector3f scale    = new Vector3f(1.0f, 1.0f, 1.0f);
    
    // Cached transformation matrix to avoid allocating new instances during runtime loops
    public final Matrix4f transformMatrix = new Matrix4f();

    public TransformComponent() {}

    public TransformComponent(Vector3f position) {
        this.position.set(position);
    }

    public TransformComponent(Vector3f position, Vector3f rotation, Vector3f scale) {
        this.position.set(position);
        this.rotation.set(rotation);
        this.scale.set(scale);
    }
}
