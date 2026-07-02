package com.aengine.ecs.serialization;

import com.aengine.ecs.Registry;
import com.aengine.ecs.components.TransformComponent;
import com.aengine.ecs.components.SpriteComponent;
import com.aengine.graphics.opengl.OpenGLTexture;
import com.aengine.utils.FileSystem;
import com.aengine.utils.Logger;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.io.File;
import java.io.FileReader;

public class PrefabLoader {

    private static final Gson gson = new Gson();

    // The active Registry instance is now passed to the instantiator
    public static int instantiate(Registry activeRegistry, String virtualPath) {
        File file = FileSystem.resolve(virtualPath);
        
        if (!file.exists()) {
            Logger.error(Logger.System.CORE, "Prefab allocation failed. File not found: %s", file.getAbsolutePath());
            return -1;
        }

        try (FileReader reader = new FileReader(file)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            String entityName = root.has("name") ? root.get("name").getAsString() : "Unnamed_Entity";
            
            // Call createEntity on the passed instance, not the class
            int entityId = activeRegistry.createEntity();
            Logger.debug(Logger.System.CORE, "Instantiating Prefab [%s] to Entity ID: %d", entityName, entityId);

            if (root.has("components")) {
                JsonObject components = root.getAsJsonObject("components");

                // Parse Transform
                if (components.has("TransformComponent")) {
                    JsonObject transformData = components.getAsJsonObject("TransformComponent");
                    TransformComponent transform = new TransformComponent();
                    
                    fillVector3(transformData, "position", transform.position);
                    fillVector3(transformData, "rotation", transform.rotation);
                    fillVector3(transformData, "scale", transform.scale);
                    
                    activeRegistry.addComponent(entityId, transform);
                }

                // Parse Sprite
                if (components.has("SpriteComponent")) {
                    JsonObject spriteData = components.getAsJsonObject("SpriteComponent");
                    SpriteComponent sprite = new SpriteComponent();
                    
                    if (spriteData.has("texture")) {
                        String texturePath = spriteData.get("texture").getAsString();
                        sprite.texture = com.aengine.graphics.AssetManager.getTexture(texturePath); 
                    }
                    
                    if (spriteData.has("color")) {
                        JsonArray colorArray = spriteData.getAsJsonArray("color");
                        sprite.color.x = colorArray.get(0).getAsFloat();
                        sprite.color.y = colorArray.get(1).getAsFloat();
                        sprite.color.z = colorArray.get(2).getAsFloat();
                        sprite.color.w = colorArray.get(3).getAsFloat();
                    }
                    
                    activeRegistry.addComponent(entityId, sprite);
                }
            }

            return entityId;

        } catch (Exception e) {
            Logger.error(Logger.System.CORE, "Failed to parse .entity format: %s. Reason: %s", virtualPath, e.getMessage());
            return -1;
        }
    }

    // Helper updated to use JOML Vector3f instead of float[]
    private static void fillVector3(JsonObject source, String key, Vector3f targetVector) {
        if (source.has(key)) {
            JsonArray array = source.getAsJsonArray(key);
            targetVector.x = array.get(0).getAsFloat();
            targetVector.y = array.get(1).getAsFloat();
            targetVector.z = array.get(2).getAsFloat();
        }
    }
}
