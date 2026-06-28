package com.aengine;

import com.aengine.graphics.ImGuiLayer;
import com.aengine.utils.Logger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;

public abstract class Engine {

    private final Window window;
    private volatile boolean running;
    private ImGuiLayer imGuiLayer;

    // Hot reload infrastructure tracking placeholders (Preserved for future System hot-swaps)
    private Path targetClassPath;
    private String gameClassName;
    private long lastKnownModificationTime = 0;
    private long lastReloadCheckTime = 0;
    private static final long CHECK_INTERVAL_MS = 1000; 


    public Engine(String title, int width, int height) {
        this.window = new Window(title, width, height);
    }

    /**
     * Configures the hot reload sub-system targets.
     */
    public final void configureHotReload(String buildDirectory, String fullyQualifiedClassName) {
        this.gameClassName = fullyQualifiedClassName;
        this.targetClassPath = Paths.get(buildDirectory).resolve(fullyQualifiedClassName.replace('.', '/') + ".class");
        
        if (Files.exists(targetClassPath)) {
            try {
                this.lastKnownModificationTime = Files.getLastModifiedTime(targetClassPath).toMillis();
                Logger.info(Logger.System.CORE, "Hot Reload system pointing to: %s", targetClassPath.toAbsolutePath());
            } catch (Exception e) {
                Logger.error(Logger.System.CORE, "Failed to resolve initial file attributes for hot reload target.");
            }
        } else {
            Logger.warn(Logger.System.CORE, "Hot reload target bytecode file not found yet at: %s.", targetClassPath.toAbsolutePath());
        }
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
        Logger.info(Logger.System.CORE, "Initializing core engine components...");
        window.init();
        Input.init(window.getHandle());
        
        imGuiLayer = new ImGuiLayer();
        imGuiLayer.init(window.getHandle());
        
        if (gameClassName != null) {
            reloadGameCode();
        }

        Logger.info(Logger.System.CORE, "Invoking native engine host onInit callback...");
        onInit();
    }

private void loop() {
        running = true;
        long lastTime = System.nanoTime();
        
        Logger.info(Logger.System.CORE, "Engine main loop engaged.");

        while (running && !window.shouldClose()) {
            long now = System.nanoTime();
            float deltaTime = (now - lastTime) / 1_000_000_000.0f;
            lastTime = now;

            long currentMillis = now / 1_000_000;
            if (currentMillis - lastReloadCheckTime > CHECK_INTERVAL_MS) {
                checkAndHandleHotReload();
                lastReloadCheckTime = currentMillis;
            }

            org.lwjgl.glfw.GLFW.glfwPollEvents(); 

            Input.update();
            onUpdate(deltaTime);

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            
            onRender();

            // ImGui overlay rendering block
            imGuiLayer.beginFrame();
            imgui.ImGui.begin("Engine Telemetry Debugger");
            imgui.ImGui.text(String.format("Application Performance: %.2f FPS", 1.0f / deltaTime));
            imgui.ImGui.text(String.format("Frame Time Delta: %.4f ms", deltaTime * 1000.0f));
            imgui.ImGui.separator();
            imgui.ImGui.text(String.format("Hardware Mouse X: %.2f", Input.getMouseX()));
            imgui.ImGui.text(String.format("Hardware Mouse Y: %.2f", Input.getMouseY()));
            imgui.ImGui.end();
            imGuiLayer.endFrame();

            window.swapBuffers();
        }
        
        Logger.info(Logger.System.CORE, "Break condition detected. Terminating main loop...");
    }

    private void checkAndHandleHotReload() {
        if (targetClassPath == null || !Files.exists(targetClassPath)) return;

        try {
            long currentModificationTime = Files.getLastModifiedTime(targetClassPath).toMillis();
            if (currentModificationTime > lastKnownModificationTime) {
                lastKnownModificationTime = currentModificationTime;
                Logger.info(Logger.System.CORE, "Detected bytecode file mutation on disk. Triggering hot reload...");
                reloadGameCode();
            }
        } catch (Exception e) {
            Logger.error(Logger.System.CORE, "Hot reload file attribute lookup failed: %s", e.getMessage());
        }
    }

    private void reloadGameCode() {
        // TODO: Adapt this hook to reconstruct individual ECS Systems bytecode dynamically instead of GameBehavior objects
        Logger.debug(Logger.System.CORE, "Hot reload intercepted. Pipeline pending target conversion to pure ECS Data-Driven systems.");
    }

    private void cleanup() {
        Logger.info(Logger.System.CORE, "Executing engine teardown sequence...");
        onCleanup();
        
        if (imGuiLayer != null) {
            imGuiLayer.cleanup();
        }
        
        window.cleanup();
        Logger.info(Logger.System.CORE, "Engine lifecycle shutdown complete.");
    }

    public final void stop() { 
        running = false; 
    }
    
    public Window getWindow() { 
        return window; 
    }

    protected abstract void onInit();
    protected abstract void onUpdate(float deltaTime);
    protected abstract void onRender();
    protected abstract void onCleanup();
}
