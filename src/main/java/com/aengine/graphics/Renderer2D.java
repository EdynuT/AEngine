package com.aengine.graphics;

import com.aengine.graphics.opengl.*;
import com.aengine.utils.FileUtils;
import com.aengine.utils.Logger;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.GL_MAX_TEXTURE_IMAGE_UNITS;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.glBufferSubData;

public class Renderer2D {

    // Batch constraints
    private static final int MAX_QUADS = 1000;
    private static final int VERTICES_PER_QUAD = 4;
    private static final int INDICES_PER_QUAD = 6;
    private static final int VERTEX_SIZE_FLOATS = 10; // pos(3), uv(2), color(4), texIndex(1)
    
    private static final int MAX_VERTICES = MAX_QUADS * VERTICES_PER_QUAD;
    private static final int MAX_INDICES = MAX_QUADS * INDICES_PER_QUAD;
    
    // Made dynamic to support multi-hardware constraints (Intel iGPU vs AMD Dedicated)
    private static int maxTextureSlots = 32;

    private static RendererAPI renderer;
    private static ShaderAPI batchShader;
    private static OpenGLVAO quadVAO;
    private static OpenGLVBO quadVBO;
    private static OpenGLEBO quadEBO;

    // RAM Cache allocation
    private static float[] vertexBuffer;
    private static int     vertexCount = 0;
    private static int     indexCount  = 0;

    private static TextureAPI[] textureSlots;
    private static int          textureSlotIndex = 0;

    private static final Vector4f[] LOCAL_VERTEX_POSITIONS = {
        new Vector4f(-0.5f, -0.5f, 0.0f, 1.0f),
        new Vector4f( 0.5f, -0.5f, 0.0f, 1.0f),
        new Vector4f( 0.5f,  0.5f, 0.0f, 1.0f),
        new Vector4f(-0.5f,  0.5f, 0.0f, 1.0f)
    };

    private static final Vector2f[] LOCAL_UV_COORDS = {
        new Vector2f(0.0f, 0.0f),
        new Vector2f(1.0f, 0.0f),
        new Vector2f(1.0f, 1.0f),
        new Vector2f(0.0f, 1.0f)
    };

    public static void init() {
        Logger.info(Logger.System.RENDERER, "Querying hardware capabilities...");
        
        // Interrogate active graphics context driver for texture units constraints
        // 1. Query hardware slots limits
        int[] queryBuffer = new int[1];
        glGetIntegerv(GL_MAX_TEXTURE_IMAGE_UNITS, queryBuffer);
        
        // Auto-detect: sets 32 for your AMD RX 7900 XT and safe 8 for the Intel iGPU
        maxTextureSlots = queryBuffer[0] >= 32 ? 32 : 8; 
        Logger.info(Logger.System.RENDERER, "Hardware reports %d units. Dynamic batch ceiling set to: %d slots.", queryBuffer[0], maxTextureSlots);

        renderer = RenderContext.createRenderer();
        renderer.init();
        
        // 2. Read, inject slots count, and compile dynamic sources
        String vertSrc = FileUtils.readResource("/shaders/opengl/texture.vert");
        String fragSrc = FileUtils.readAndInjectResource("/shaders/opengl/texture.frag", maxTextureSlots);
        
        // Invoke the source code constructor
        // BEFORE: batchShader = new OpenGLShader(vertSrc, fragSrc, true);
        batchShader = RenderContext.createShader(vertSrc, fragSrc, true);

        // Rest of the initialization code remains unchanged...
        vertexBuffer = new float[MAX_VERTICES * VERTEX_SIZE_FLOATS];
        textureSlots = new TextureAPI[maxTextureSlots];

        quadVAO = new OpenGLVAO();
        quadVBO = new OpenGLVBO();
        quadEBO = new OpenGLEBO();

        quadVAO.bind();
        quadVBO.bind();
        quadVBO.uploadData(new float[MAX_VERTICES * VERTEX_SIZE_FLOATS], GL_DYNAMIC_DRAW);

        int[] indices = new int[MAX_INDICES];
        int offset = 0;
        for (int i = 0; i < MAX_INDICES; i += INDICES_PER_QUAD) {
            indices[i + 0] = offset + 0;
            indices[i + 1] = offset + 1;
            indices[i + 2] = offset + 2;
            indices[i + 3] = offset + 2;
            indices[i + 4] = offset + 3;
            indices[i + 5] = offset + 0;
            offset += VERTICES_PER_QUAD;
        }
        quadEBO.uploadData(indices, GL_STATIC_DRAW);

        quadVAO.setVertexAttrib(0, 3, VERTEX_SIZE_FLOATS, 0); // pos (x, y, z) - NOW 3 FLOATS
        quadVAO.setVertexAttrib(1, 2, VERTEX_SIZE_FLOATS, 3); // uv (u, v) - Offset shifts to 3
        quadVAO.setVertexAttrib(2, 4, VERTEX_SIZE_FLOATS, 5); // color (r, g, b, a) - Offset shifts to 5
        quadVAO.setVertexAttrib(3, 1, VERTEX_SIZE_FLOATS, 9); // texIndex - Offset shifts to 9

        quadVAO.unbind();

        Logger.info(Logger.System.RENDERER, "Batch storage allocations verified. Capable of %d quads per draw call.", MAX_QUADS);
    }

    public static void beginScene(Camera camera) {
        batchShader.bind();
        batchShader.setMat4("u_ViewProjection", camera.getViewProjection());
        
        // Populate samplers array sequentially matching the current dynamic texture layout size
        for (int i = 0; i < maxTextureSlots; i++) {
            batchShader.setInt("u_Textures[" + i + "]", i);
        }
        batchShader.unbind();

        startBatch();
    }

    private static void startBatch() {
        vertexCount = 0;
        indexCount = 0;
        textureSlotIndex = 0;
    }

    public static void endScene() {
        flush();
    }

    public static void flush() {
        if (indexCount == 0) return;

        for (int i = 0; i < textureSlotIndex; i++) {
            textureSlots[i].bind(i);
        }

        quadVBO.bind();
        float[] activeVertices = new float[vertexCount];
        System.arraycopy(vertexBuffer, 0, activeVertices, 0, vertexCount);
        glBufferSubData(GL_ARRAY_BUFFER, 0, activeVertices);

        batchShader.bind();
        quadVAO.bind();
        
        renderer.drawIndexed(indexCount);

        quadVAO.unbind();
        batchShader.unbind();
    }

    private static void nextBatch() {
        flush();
        startBatch();
    }

    public static void drawQuad(Vector2f position, Vector2f size, Vector4f color) {
        drawQuad(position, size, null, color);
    }

    public static void drawQuad(Vector2f position, Vector2f size, TextureAPI texture) {
        drawQuad(position, size, texture, new Vector4f(1.0f));
    }

    public static void drawQuad(Vector2f position, Vector2f size, TextureAPI texture, Vector4f tint) {
        if (indexCount >= MAX_INDICES) {
            nextBatch();
        }

        float textureIndex = -1.0f;
        
        if (texture != null) {
            for (int i = 0; i < textureSlotIndex; i++) {
                if (textureSlots[i].getID() == texture.getID()) {
                    textureIndex = (float) i;
                    break;
                }
            }

            if (textureIndex == -1.0f) {
                if (textureSlotIndex >= maxTextureSlots) {
                    nextBatch();
                }
                textureSlots[textureSlotIndex] = texture;
                textureIndex = (float) textureSlotIndex;
                textureSlotIndex++;
            }
        }

        Matrix4f transform = new Matrix4f()
            .translate(position.x, position.y, 0.0f)
            .scale(size.x, size.y, 1.0f);

        for (int i = 0; i < VERTICES_PER_QUAD; i++) {
        Vector4f transformedPos = new Vector4f(LOCAL_VERTEX_POSITIONS[i]).mul(transform);

        int baseIndex = vertexCount;
        vertexBuffer[baseIndex + 0] = transformedPos.x;
        vertexBuffer[baseIndex + 1] = transformedPos.y;
        vertexBuffer[baseIndex + 2] = 0.0f; // Default Z coordinate layer for 2D elements
        vertexBuffer[baseIndex + 3] = LOCAL_UV_COORDS[i].x;
        vertexBuffer[baseIndex + 4] = LOCAL_UV_COORDS[i].y;
        vertexBuffer[baseIndex + 5] = tint.x;
        vertexBuffer[baseIndex + 6] = tint.y;
        vertexBuffer[baseIndex + 7] = tint.z;
        vertexBuffer[baseIndex + 8] = tint.w;
        vertexBuffer[baseIndex + 9] = textureIndex;

        vertexCount += VERTEX_SIZE_FLOATS;
        }

        indexCount += INDICES_PER_QUAD;
    }

    public static void cleanup() {
        Logger.info(Logger.System.RENDERER, "Deallocating internal Batch Renderer pipeline elements...");
        batchShader.cleanup();
        quadVAO.cleanup();
        quadVBO.cleanup();
        quadEBO.cleanup();
    }

    public static void setClearColor(float r, float g, float b, float a) {
        renderer.setClearColor(new Vector4f(r, g, b, a));
    }

    static float[] getVertexBuffer() { return vertexBuffer; }
    static int getVertexCount() { return vertexCount; }
    static void setVertexCount(int count) { vertexCount = count; }
    static int getIndexCount() { return indexCount; }
    static void setIndexCount(int count) { indexCount = count; }

    static float getOrCreateTextureIndex(TextureAPI texture) {
        if (texture == null) return -1.0f;
        
        for (int i = 0; i < textureSlotIndex; i++) {
            if (textureSlots[i].getID() == texture.getID()) return (float) i;
        }

        if (textureSlotIndex >= maxTextureSlots) {
            flush();
            vertexCount = 0;
            indexCount = 0;
            textureSlotIndex = 0;
        }
        
        textureSlots[textureSlotIndex] = texture;
        float index = (float) textureSlotIndex;
        textureSlotIndex++;
        return index;
    }
}
