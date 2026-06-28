package com.aengine.ecs.components;

import com.aengine.graphics.Camera;

public final class CameraComponent {

    public final Camera camera;
    public boolean primary = true;

    public CameraComponent(float fov, float width, float height, float near, float far, boolean isPerspective) {
        if (isPerspective) {
            float aspectRatio = width / height;
            this.camera = Camera.perspective(fov, aspectRatio, near, far);
        } else {
            // Orthographic implementation uses standard aspect box dimensions
            this.camera = Camera.orthographic(-width / 2.0f, width / 2.0f, -height / 2.0f, height / 2.0f);
        }
    }
}
