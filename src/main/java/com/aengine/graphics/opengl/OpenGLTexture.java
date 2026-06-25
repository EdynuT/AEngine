package com.aengine.graphics.opengl;

import com.aengine.graphics.TextureAPI;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;

public class OpenGLTexture implements TextureAPI {

    private final int id;
    private final int width;
    private final int height;

    public OpenGLTexture(String resourcePath) {
        IntBuffer w        = BufferUtils.createIntBuffer(1);
        IntBuffer h        = BufferUtils.createIntBuffer(1);
        IntBuffer channels = BufferUtils.createIntBuffer(1);

        STBImage.stbi_set_flip_vertically_on_load(true);
        ByteBuffer rawData = loadResourceBytes(resourcePath);
        ByteBuffer pixels  = STBImage.stbi_load_from_memory(rawData, w, h, channels, 4);

        if (pixels == null) {
            throw new RuntimeException(
                "Failed to load texture: " + resourcePath + "\n" + STBImage.stbi_failure_reason());
        }

        width  = w.get(0);
        height = h.get(0);

        id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S,     GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T,     GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
        glGenerateMipmap(GL_TEXTURE_2D);

        STBImage.stbi_image_free(pixels);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    private ByteBuffer loadResourceBytes(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) throw new RuntimeException("Texture not found: " + path);
            byte[]     bytes = is.readAllBytes();
            ByteBuffer buf   = BufferUtils.createByteBuffer(bytes.length);
            buf.put(bytes).flip();
            return buf;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read texture: " + path, e);
        }
    }

    @Override public void bind(int slot) {
        glActiveTexture(GL_TEXTURE0 + slot);
        glBindTexture(GL_TEXTURE_2D, id);
    }
    @Override public void unbind()    { glBindTexture(GL_TEXTURE_2D, 0); }
    @Override public int  getWidth()  { return width; }
    @Override public int  getHeight() { return height; }
    @Override public void cleanup()   { glDeleteTextures(id); }
    public    int  getId()            { return id; }
}
