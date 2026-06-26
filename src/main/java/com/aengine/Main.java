package com.aengine;

import com.aengine.graphics.Camera;
import com.aengine.graphics.Renderer2D;
import org.joml.Vector2f;
import org.joml.Vector4f;

public class Main extends Engine {

    private Camera camera;

    public Main() {
        super("AEngine", 1280, 720);
    }

    @Override
    protected void onInit() {
        Renderer2D.init();
        camera = Camera.orthographic(0.0f, 1280.0f, 0.0f, 720.0f);
        Renderer2D.setClearColor(0.1f, 0.1f, 0.15f, 1.0f);
    }

    @Override
    protected void onUpdate(float deltaTime) {
        if (Input.isKeyPressed(Keys.ESCAPE)) {
            stop();
        }
    }

    @Override
    protected void onRender() {
        Renderer2D.beginScene(camera);
        Renderer2D.drawQuad(
            new Vector2f(640, 360),
            new Vector2f(200, 200),
            new Vector4f(0.2f, 0.5f, 1.0f, 1.0f)
        );
    }

    @Override
    protected void onCleanup() {
        Renderer2D.cleanup();
    }

    public static void main(String[] args) {
        new Main().run();
    }
}
