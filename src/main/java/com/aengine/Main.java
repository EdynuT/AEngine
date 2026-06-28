package com.aengine;

import com.aengine.graphics.Renderer2D;
import com.aengine.graphics.Renderer3D;
import com.aengine.utils.FileSystem;
import com.aengine.utils.ProjectWizard;
import com.aengine.ecs.Registry;
import com.aengine.ecs.components.TransformComponent;
import com.aengine.ecs.components.CameraComponent;
import com.aengine.ecs.components.SpriteComponent;
import com.aengine.ecs.systems.CameraSystem;
import com.aengine.ecs.systems.RenderSystem;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class Main extends Engine {

    private Registry registry;
    private CameraSystem cameraSystem;
    private RenderSystem renderSystem;
    
    private int cameraEntity;

    public Main() {
        super("AEngine - ECS Fly-Camera Runtime", 1280, 720);
    }

    @Override
    protected void onInit() {
        String parentPath = System.getProperty("user.home"); 
        String projectName = "AeternumSandbox";

        try {
            String activeProjectPath = ProjectWizard.createProject(parentPath, projectName);
            FileSystem.mountProject(activeProjectPath);
        } catch (Exception e) {
            FileSystem.mountProject(parentPath + "/" + projectName);
        }

        Renderer2D.init();
        Renderer3D.init();
        Renderer2D.setClearColor(0.02f, 0.02f, 0.04f, 1.0f);
        
        // Lock cursor lines to handle continuous mouse delta look arrays correctly
        Input.setCursorMode(false); 

        registry = new Registry();
        cameraSystem = new CameraSystem();
        renderSystem = new RenderSystem();

        // 1. Spawn the dynamic flight camera entity
        cameraEntity = registry.createEntity();
        registry.addComponent(cameraEntity, new TransformComponent(new Vector3f(0.0f, 0.0f, 5.0f)));
        registry.addComponent(cameraEntity, new CameraComponent(45.0f, 1280.0f, 720.0f, 0.1f, 100.0f, true));

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
        CameraComponent primaryCam = registry.getComponent(cameraEntity, CameraComponent.class);
        
        if (primaryCam != null) {
            // Open batch buffer slice with current matrix states
            Renderer2D.beginScene(primaryCam.camera);
            
            // Dispatch target component primitives down the rendering pipelines
            renderSystem.update(registry, 0.0f);
            
            // Flush commands down to GPU driver memory lines
            Renderer2D.endScene();
        }
    }

    @Override
    protected void onCleanup() {
        Renderer3D.cleanup();
        Renderer2D.cleanup();
    }

    public static void main(String[] args) {
        new Main().run();
    }
}
