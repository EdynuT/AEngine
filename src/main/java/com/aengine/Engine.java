package com.aengine;

import com.aengine.graphics.ImGuiLayer;
import com.aengine.utils.Logger;
import com.aengine.ecs.Registry; // Bound architecture mapping
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

    // Strict global data store context binding
    protected final Registry registry; 

    // Hot reload infrastructure tracking placeholders
    private Path targetClassPath;
    private String gameClassName;
    private long lastKnownModificationTime = 0;
    private long lastReloadCheckTime = 0;
    private static final long CHECK_INTERVAL_MS = 1000; 

    private com.aengine.editor.SceneHierarchyPanel hierarchyPanel;

    public Engine(String title, int width, int height) {
        this.window = new Window(title, width, height);
        this.registry = new Registry(); // Centralizes baseline ECS lifecycle memory context
    }

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

        // Safe registration tracking passing the correctly initialized engine registry context
        hierarchyPanel = new com.aengine.editor.SceneHierarchyPanel(registry);
        
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

            // ImGui pipeline framing
            imGuiLayer.beginFrame();
            
            // 1. MAIN MENU BAR & DOCKSPACE HOSTER (Corrected Spair package routing)
            int windowFlags = imgui.flag.ImGuiWindowFlags.MenuBar | imgui.flag.ImGuiWindowFlags.NoDocking;
            imgui.ImGuiViewport viewport = imgui.ImGui.getMainViewport(); // Elevated to root package
            
            imgui.ImGui.setNextWindowPos(viewport.getWorkPosX(), viewport.getWorkPosY());
            imgui.ImGui.setNextWindowSize(viewport.getWorkSizeX(), viewport.getWorkSizeY());
            imgui.ImGui.setNextWindowViewport(viewport.getID());
            
            int hostFlags = imgui.flag.ImGuiWindowFlags.NoTitleBar | imgui.flag.ImGuiWindowFlags.NoCollapse 
                          | imgui.flag.ImGuiWindowFlags.NoResize | imgui.flag.ImGuiWindowFlags.NoMove 
                          | imgui.flag.ImGuiWindowFlags.NoBringToFrontOnFocus | imgui.flag.ImGuiWindowFlags.NoNavFocus;

            imgui.ImGui.begin("Editor Workspace Hoster", new imgui.type.ImBoolean(true), windowFlags | hostFlags);
            
            // Render Global Menu Bar
            if (imgui.ImGui.beginMainMenuBar()) {
                if (imgui.ImGui.beginMenu("File")) {
                    if (imgui.ImGui.menuItem("Exit", "Alt+F4")) stop();
                    imgui.ImGui.endMenu();
                }
                if (imgui.ImGui.beginMenu("Entity")) {
                    if (imgui.ImGui.menuItem("Create Empty Entity")) {
                        int newEntity = registry.createEntity();
                        registry.addComponent(newEntity, new com.aengine.ecs.components.TransformComponent());
                    }
                    imgui.ImGui.endMenu();
                }
                imgui.ImGui.endMainMenuBar();
            }

            // Enable central docking node
            imgui.ImGui.dockSpace(imgui.ImGui.getID("WorkspaceDockspace"));
            imgui.ImGui.end(); // Closes Hoster Window
            
            // 2. PANEL RENDERING
            imgui.ImGui.begin("Engine Telemetry Debugger");
            imgui.ImGui.text(String.format("Application Performance: %.2f FPS", 1.0f / deltaTime));
            imgui.ImGui.text(String.format("Frame Time Delta: %.4f ms", deltaTime * 1000.0f));
            imgui.ImGui.separator();
            imgui.ImGui.text(String.format("Hardware Mouse X: %.2f", Input.getMouseX()));
            imgui.ImGui.text(String.format("Hardware Mouse Y: %.2f", Input.getMouseY()));
            imgui.ImGui.end();
            
            hierarchyPanel.onImGuiRender(); 
            
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
