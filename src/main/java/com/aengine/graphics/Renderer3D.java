package com.aengine.graphics;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import com.aengine.utils.Logger;

public class Renderer3D {

    private static final int VERTICES_PER_QUAD = 4;
    private static final int INDICES_PER_QUAD = 6;
    private static final int VERTEX_SIZE_FLOATS = 10;

    // Unit quad local position definitions in 3D Space (Facing the Z axis)
    private static final Vector4f[] LOCAL_VERTICES = {
        new Vector4f(-0.5f, -0.5f, 0.0f, 1.0f),
        new Vector4f( 0.5f, -0.5f, 0.0f, 1.0f),
        new Vector4f( 0.5f,  0.5f, 0.0f, 1.0f),
        new Vector4f(-0.5f,  0.5f, 0.0f, 1.0f)
    };

    private static final Vector2f[] LOCAL_UV = {
        new Vector2f(0.0f, 0.0f),
        new Vector2f(1.0f, 0.0f),
        new Vector2f(1.0f, 1.0f),
        new Vector2f(0.0f, 1.0f)
    };

    public static void init() {
        Logger.info(Logger.System.RENDERER, "Initializing core 3D Projection Subsystem...");
    }

    public static void beginScene(Camera camera) {
        // Diverts active hardware thread state compilation to unifed dynamic shader pipelines
        Renderer2D.beginScene(camera);
    }

    public static void drawCubeFace(Vector3f position, Vector3f rotation, Vector3f scale, TextureAPI texture, Vector4f tint) {
        if (Renderer2D.getIndexCount() >= 6000) {
            Renderer2D.flush();
        }

        float textureIndex = Renderer2D.getOrCreateTextureIndex(texture);

        // Build 3D transformation matrix (Translation, Rotation, Scale)
        Matrix4f transform = new Matrix4f()
            .translate(position)
            .rotateX((float) Math.toRadians(rotation.x))
            .rotateY((float) Math.toRadians(rotation.y))
            .rotateZ((float) Math.toRadians(rotation.z))
            .scale(scale);

        float[] vBuffer = Renderer2D.getVertexBuffer();

        for (int i = 0; i < VERTICES_PER_QUAD; i++) {
            Vector4f worldPos = new Vector4f(LOCAL_VERTICES[i]).mul(transform);

            int baseIndex = Renderer2D.getVertexCount();
            vBuffer[baseIndex + 0] = worldPos.x;
            vBuffer[baseIndex + 1] = worldPos.y;
            vBuffer[baseIndex + 2] = worldPos.z; // Hardware Z Depth value preserved
            vBuffer[baseIndex + 3] = LOCAL_UV[i].x;
            vBuffer[baseIndex + 4] = LOCAL_UV[i].y;
            vBuffer[baseIndex + 5] = tint.x;
            vBuffer[baseIndex + 6] = tint.y;
            vBuffer[baseIndex + 7] = tint.z;
            vBuffer[baseIndex + 8] = tint.w;
            vBuffer[baseIndex + 9] = textureIndex;

            Renderer2D.setVertexCount(baseIndex + VERTEX_SIZE_FLOATS);
        }

        Renderer2D.setIndexCount(Renderer2D.getIndexCount() + INDICES_PER_QUAD);
    }

    public static void endScene() {
        Renderer2D.endScene();
    }

    public static void cleanup() {
        Logger.info(Logger.System.RENDERER, "3D Context terminated.");
    }
}
