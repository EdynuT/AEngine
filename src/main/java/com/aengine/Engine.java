package com.aengine;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;

public abstract class Engine {

    private final Window     window;
    private volatile boolean running;

    public Engine(String title, int width, int height) {
        this.window = new Window(title, width, height);
    }

    public final void run() {
        try {
            init();
            loop();
        } finally {
            cleanup();
        }
    }

    private void init() {
        window.init();
        Input.init(window.getHandle());
        onInit();
    }

    private void loop() {
        running = true;
        long lastTime = System.nanoTime();

        while (running && !window.shouldClose()) {
            long  now       = System.nanoTime();
            float deltaTime = (now - lastTime) / 1_000_000_000.0f;
            lastTime = now;

            Input.poll();
            onUpdate(deltaTime);

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            onRender();

            window.swapBuffers();
        }
    }

    private void cleanup() {
        onCleanup();
        window.cleanup();
    }

    public final void stop()     { running = false; }
    public Window     getWindow() { return window; }

    protected abstract void onInit();
    protected abstract void onUpdate(float deltaTime);
    protected abstract void onRender();
    protected abstract void onCleanup();
}
