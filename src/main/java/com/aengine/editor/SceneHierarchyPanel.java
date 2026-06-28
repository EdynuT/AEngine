package com.aengine.editor;

import com.aengine.ecs.Registry;
import com.aengine.ecs.components.TransformComponent;
import com.aengine.ecs.components.SpriteComponent;
import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;

public final class SceneHierarchyPanel {

    private final Registry registry;
    private int selectedEntity = -1;

    public SceneHierarchyPanel(Registry registry) {
        this.registry = registry;
    }

    /**
     * Renders the complete editor workspace overlay layout window.
     */
    public void onImGuiRender() {
        // 1. Scene Hierarchy Panel Window
        ImGui.begin("Scene Hierarchy");

        // Temporary dump: Query all entities containing a TransformComponent to list in hierarchy
        // In a full architecture, this would query an absolute baseline Entity relation array
        var transformPool = registry.getPool(TransformComponent.class);
        if (transformPool != null) {
            int[] denseToEntity = transformPool.getRawDenseToEntity();
            int totalEntities = transformPool.size();

            for (int i = 0; i < totalEntities; i++) {
                int entityID = denseToEntity[i];
                
                // Formulate an industry-standard unique node string tag index identifier
                int flags = ((selectedEntity == entityID) ? ImGuiTreeNodeFlags.Selected : 0) 
                            | ImGuiTreeNodeFlags.OpenOnArrow | ImGuiTreeNodeFlags.SpanAvailWidth;
                
                boolean treeNodeOpen = ImGui.treeNodeEx("Entity ID: " + entityID, flags);
                
                if (ImGui.isItemClicked()) {
                    selectedEntity = entityID;
                }

                if (treeNodeOpen) {
                    ImGui.treePop();
                }
            }
        }
        ImGui.end();

        // 2. Component Inspector Panel Window
        ImGui.begin("Inspector");
        if (selectedEntity != -1) {
            renderComponents(selectedEntity);
        } else {
            ImGui.text("Select an entity within the hierarchy to inspect properties.");
        }
        ImGui.end();
    }

    /**
     * Dispatches explicit ImGui property fields for components attached to the target entity.
     */
    private void renderComponents(int entity) {
        TransformComponent transform = registry.getComponent(entity, TransformComponent.class);
        if (transform != null) {
            if (ImGui.collapsingHeader("Transform Component")) {
                float[] pos = { transform.position.x, transform.position.y, transform.position.z };
                float[] rot = { transform.rotation.x, transform.rotation.y, transform.rotation.z };
                float[] scl = { transform.scale.x, transform.scale.y, transform.scale.z };

                if (ImGui.dragFloat3("Position", pos, 0.05f)) {
                    transform.position.set(pos[0], pos[1], pos[2]);
                }
                if (ImGui.dragFloat3("Rotation", rot, 0.2f)) {
                    transform.rotation.set(rot[0], rot[1], rot[2]);
                }
                if (ImGui.dragFloat3("Scale", scl, 0.05f)) {
                    transform.scale.set(scl[0], scl[1], scl[2]);
                }
            }
        }

        SpriteComponent sprite = registry.getComponent(entity, SpriteComponent.class);
        if (sprite != null) {
            if (ImGui.collapsingHeader("Sprite Component")) {
                float[] color = { sprite.color.x, sprite.color.y, sprite.color.z, sprite.color.w };
                if (ImGui.colorEdit4("Albedo Tint", color)) {
                    sprite.color.set(color[0], color[1], color[2], color[3]);
                }
                
                if (sprite.texture != null) {
                    ImGui.text("Texture Handle Link: Bound");
                } else {
                    ImGui.text("Texture Handle Link: Raw Untextured Color Mode");
                }
                
                ImGui.spacing();
                if (ImGui.button("Remove Sprite Component")) {
                    registry.removeComponent(entity, SpriteComponent.class);
                }
            }
        }

        // TWEAK: Contextual Component Injection System
        ImGui.spacing();
        ImGui.separator();
        ImGui.spacing();

        if (ImGui.button("Add Component", ImGui.getContentRegionAvailX(), 0)) {
            ImGui.openPopup("AddComponentPopup");
        }

        if (ImGui.beginPopup("AddComponentPopup")) {
            if (registry.getComponent(entity, SpriteComponent.class) == null) {
                if (ImGui.menuItem("Sprite Component")) {
                    registry.addComponent(entity, new SpriteComponent());
                    ImGui.closeCurrentPopup();
                }
            } else {
                ImGui.textDisabled("Sprite Component (Already Active)");
            }

            // Future placeholders can be safely mapped here (e.g., RigidBody, AudioSource)
            ImGui.endPopup();
        }
    }

    public int getSelectedEntity() { return selectedEntity; }
}
