package com.aengine;

import com.aengine.graphics.Renderer2D;
import com.aengine.graphics.Renderer3D;
import com.aengine.utils.FileSystem;
import com.aengine.utils.Logger;
import com.aengine.utils.ProjectWizard;
import com.aengine.ecs.components.TransformComponent;
import com.aengine.ecs.components.CameraComponent;
import com.aengine.ecs.components.SpriteComponent;
import com.aengine.ecs.systems.CameraSystem;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class Main extends Engine {

    private CameraSystem cameraSystem;
    private int cameraEntity;

    // Allocation-free temporary structural containers for environmental layout
    private static final Vector2f GROUND_POSITION = new Vector2f(0.0f, -1.5f); 
    private static final Vector2f GROUND_SIZE     = new Vector2f(5000.0f, 5000.0f);    
    private static final Vector4f GROUND_COLOR    = new Vector4f(0.35f, 0.35f, 0.36f, 1.0f); // Slate Light Gray Floor

    // Shared execution state capturing target path sent from external process host
    private static String activeProjectPath;

    public Main() {
        super("AEngine - ECS Fly-Camera Runtime", 1920, 1080);
    }

    @Override
    protected void onInit() {
        Logger.info(Logger.System.CORE, "Initializing core pipeline execution context...");

        // Mount Virtual File System using the path provided by Tauri handshake
        try {
            Logger.info(Logger.System.CORE, "Target asset workspace resolution: " + activeProjectPath);
            FileSystem.mountProject(activeProjectPath);
        } catch (Exception e) {
            Logger.error(Logger.System.CORE, "VFS Handshake critical failure. Falling back to default root storage mapping.");
            FileSystem.mountProject(System.getProperty("user.home") + "/AeternumSandbox");
        }

        Renderer2D.init();
        Renderer3D.init();
        
        // Atmospheric sky blue background clear color registration
        Renderer2D.setClearColor(0.45f, 0.65f, 0.85f, 1.0f);
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);

        cameraSystem = new CameraSystem();

        // 1. Spawn the dynamic flight camera entity into the shared engine ecosystem
        cameraEntity = registry.createEntity();
        registry.addComponent(cameraEntity, new TransformComponent(new Vector3f(0.0f, 0.0f, 5.0f)));
        registry.addComponent(cameraEntity, new CameraComponent(45.0f, 1920.0f, 1080.0f, 0.1f, 100.0f, true));

        // 2. Spawn static world assets for spatial evaluation mapping
        for (int i = 0; i < 3; i++) {
            int entity = registry.createEntity();
            registry.addComponent(entity, new TransformComponent(new Vector3f(i * 2.5f - 2.5f, 0.0f, 0.0f)));
            registry.addComponent(entity, new SpriteComponent(new Vector4f(0.2f, 0.5f, i * 0.3f + 0.3f, 1.0f)));
        }
    }

    @Override
    protected void onUpdate(float deltaTime) {
        if (Input.isKeyPressed(Keys.ESCAPE)) {
            stop();
        }

        // Tick hardware camera updates before dispatching geometry rendering frames
        cameraSystem.update(registry, deltaTime);
        Input.update();
    }

    @Override
    protected void onRender() {
        var cameraPool = registry.getPool(com.aengine.ecs.components.CameraComponent.class);
        com.aengine.graphics.Camera activeCamera = null;

        if (cameraPool != null) {
            com.aengine.ecs.components.CameraComponent[] cameras = cameraPool.getRawComponents();
            int totalCameras = cameraPool.size();
            
            for (int i = 0; i < totalCameras; i++) {
                if (cameras[i] != null && cameras[i].primary) {
                    activeCamera = cameras[i].camera;
                    break;
                }
            }
        }

        if (activeCamera == null) {
            return;
        }

        com.aengine.graphics.Renderer2D.beginScene(activeCamera);

        // --- RENDER ENVIRONMENT ENVIRONMENT (THE GROUND) ---
        com.aengine.graphics.Renderer2D.drawQuad(GROUND_POSITION, GROUND_SIZE, GROUND_COLOR);

        // --- RENDER ECS DYNAMIC ENTITIES SET ---
        var entities = registry.getEntitiesWith(
            com.aengine.ecs.components.TransformComponent.class, 
            com.aengine.ecs.components.SpriteComponent.class
        );

        for (int i = 0; i < entities.size(); i++) {
            int entityID = entities.get(i);
            
            var transform = registry.getComponent(entityID, com.aengine.ecs.components.TransformComponent.class);
            var sprite = registry.getComponent(entityID, com.aengine.ecs.components.SpriteComponent.class);
            
            com.aengine.graphics.Renderer2D.drawEntityQuad(transform, sprite);
        }

        com.aengine.graphics.Renderer2D.endScene();
    }

    @Override
    protected void onCleanup() {
        Logger.info(Logger.System.CORE, "Terminating active workspace runtime contexts. Executing hardware cleanup...");
        Renderer3D.cleanup();
        Renderer2D.cleanup();
    }

    public static void main(String[] args) {
        // Evaluate input arguments dispatched by the Rust process wrapper
        if (args.length > 0 && args[0] != null && !args[0].trim().isEmpty()) {
            activeProjectPath = args[0];
        } else {
            // Default decoupled fallback to prevent JVM execution crash during standalone testing
            activeProjectPath = System.getProperty("user.home") + "/AeternumSandbox";
            Logger.warn(Logger.System.CORE, "No host initialization parameters detected. Binding fallback workspace: " + activeProjectPath);
        }

        new Main().run();
    }
}
