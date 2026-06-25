package com.aengine.graphics;

import com.aengine.graphics.opengl.OpenGLEBO;
import com.aengine.graphics.opengl.OpenGLRenderer;
import com.aengine.graphics.opengl.OpenGLShader;
import com.aengine.graphics.opengl.OpenGLVAO;
import com.aengine.graphics.opengl.OpenGLVBO;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;

import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;

public class Renderer2D {

    private static OpenGLRenderer renderer;
    private static OpenGLShader   flatShader;
    private static OpenGLShader   textureShader;
    private static OpenGLVAO      quadVAO;
    private static OpenGLVBO      quadVBO;
    private static OpenGLEBO      quadEBO;

    // Unit quad: position(x,y) + uv(u,v), 4 floats per vertex
    private static final float[] QUAD_VERTICES = {
        -0.5f, -0.5f,  0.0f, 0.0f,
         0.5f, -0.5f,  1.0f, 0.0f,
         0.5f,  0.5f,  1.0f, 1.0f,
        -0.5f,  0.5f,  0.0f, 1.0f
    };
    private static final int[] QUAD_INDICES = { 0, 1, 2, 2, 3, 0 };

    public static void init() {
        renderer      = new OpenGLRenderer();
        flatShader    = new OpenGLShader("/shaders/opengl/flat.vert",    "/shaders/opengl/flat.frag");
        textureShader = new OpenGLShader("/shaders/opengl/texture.vert", "/shaders/opengl/texture.frag");
        renderer.init();

        quadVAO = new OpenGLVAO();
        quadVBO = new OpenGLVBO();
        quadEBO = new OpenGLEBO();

        quadVAO.bind();
        quadVBO.uploadData(QUAD_VERTICES, GL_STATIC_DRAW);
        quadEBO.uploadData(QUAD_INDICES,  GL_STATIC_DRAW);
        quadVAO.setVertexAttrib(0, 2, 4, 0); // position
        quadVAO.setVertexAttrib(1, 2, 4, 2); // uv
        quadVAO.unbind();
    }

    public static void setClearColor(float r, float g, float b, float a) {
        renderer.setClearColor(new Vector4f(r, g, b, a));
    }

    public static void beginScene(Camera camera) {
        flatShader.bind();
        flatShader.setMat4("u_ViewProjection", camera.getViewProjection());
        flatShader.unbind();

        textureShader.bind();
        textureShader.setMat4("u_ViewProjection", camera.getViewProjection());
        textureShader.setInt("u_Texture", 0);
        textureShader.unbind();
    }

    public static void drawQuad(Vector2f position, Vector2f size, Vector4f color) {
        Matrix4f transform = new Matrix4f()
            .translate(position.x, position.y, 0.0f)
            .scale(size.x, size.y, 1.0f);

        flatShader.bind();
        flatShader.setMat4("u_Transform", transform);
        flatShader.setVec4("u_Color", color);
        quadVAO.bind();
        renderer.drawIndexed(quadEBO.getIndexCount());
        quadVAO.unbind();
        flatShader.unbind();
    }

    public static void drawQuad(Vector2f position, Vector2f size, TextureAPI texture) {
        drawQuad(position, size, texture, new Vector4f(1.0f));
    }

    public static void drawQuad(Vector2f position, Vector2f size, TextureAPI texture, Vector4f tint) {
        Matrix4f transform = new Matrix4f()
            .translate(position.x, position.y, 0.0f)
            .scale(size.x, size.y, 1.0f);

        texture.bind(0);
        textureShader.bind();
        textureShader.setMat4("u_Transform", transform);
        textureShader.setVec4("u_Color", tint);
        quadVAO.bind();
        renderer.drawIndexed(quadEBO.getIndexCount());
        quadVAO.unbind();
        textureShader.unbind();
        texture.unbind();
    }

    public static void cleanup() {
        flatShader.cleanup();
        textureShader.cleanup();
        quadVAO.cleanup();
        quadVBO.cleanup();
        quadEBO.cleanup();
    }
}
