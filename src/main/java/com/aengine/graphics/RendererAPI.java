package com.aengine.graphics;

import org.joml.Vector4f;

public interface RendererAPI {
    void init();
    void setClearColor(Vector4f color);
    void clear();
    void drawIndexed(int indexCount);
    void cleanup();
}
