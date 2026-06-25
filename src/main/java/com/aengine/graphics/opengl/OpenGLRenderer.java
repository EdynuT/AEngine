package com.aengine.graphics.opengl;

import com.aengine.graphics.RendererAPI;
import org.joml.Vector4f;

import static org.lwjgl.opengl.GL11.*;

public class OpenGLRenderer implements RendererAPI {

    @Override
    public void init() {
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_DEPTH_TEST);
    }

    @Override
    public void setClearColor(Vector4f color) {
        glClearColor(color.x, color.y, color.z, color.w);
    }

    @Override
    public void clear() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    @Override
    public void drawIndexed(int indexCount) {
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
    }

    @Override
    public void cleanup() {}
}
