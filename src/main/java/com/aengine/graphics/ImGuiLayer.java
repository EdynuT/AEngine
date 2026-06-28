package com.aengine.graphics;

import imgui.ImGui;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import com.aengine.utils.Logger;

public final class ImGuiLayer {

    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();

    public void init(long windowHandle) {
        Logger.info(Logger.System.WINDOW, "Initializing Dear ImGui context layer via io.github.spair backend...");
        ImGui.createContext();
        
        ImGui.getIO().addConfigFlags(ImGuiConfigFlags.NavEnableKeyboard);
        
        imGuiGlfw.init(windowHandle, true);
        imGuiGl3.init("#version 330 core");
        
        Logger.info(Logger.System.WINDOW, "Dear ImGui backend drivers bound successfully.");
    }

    public void beginFrame() {
        imGuiGlfw.newFrame();
        ImGui.newFrame();
    }

    public void endFrame() {
        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());
    }

    public void cleanup() {
        Logger.info(Logger.System.WINDOW, "Destroying Dear ImGui lifecycle contexts...");
        imGuiGl3.dispose();
        imGuiGlfw.dispose();
        ImGui.destroyContext();
    }
}
