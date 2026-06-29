package com.aengine.graphics.opengl;

import com.aengine.graphics.TextureAPI;
import com.aengine.utils.FileSystem;
import com.aengine.utils.Logger;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class OpenGLTexture implements TextureAPI {

    private final int id;
    private final int width;
    private final int height;

    public OpenGLTexture(String virtualPath) {
        Logger.debug(Logger.System.ASSET, "Decoding image token via STB Image: %s", virtualPath);

        // 1. Route I/O tracking through the sandboxed Virtual File System
        File file = FileSystem.resolve(virtualPath);
        if (!file.exists()) {
            Logger.error(Logger.System.ASSET, "Resource lookup failed for path target: %s", file.getAbsolutePath());
            throw new RuntimeException("Texture asset missing: " + virtualPath);
        }

        ByteBuffer rawData;
        try {
            // Channel block streaming directly into unmanaged memory allocation
            rawData = FileSystem.ioResourceToBuffer(virtualPath, 8 * 1024);
        } catch (IOException e) {
            Logger.error(Logger.System.ASSET, "Disk IO breakdown processing byte stream: %s", virtualPath);
            throw new RuntimeException("Failed to read texture memory block: " + virtualPath, e);
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w        = stack.mallocInt(1);
            IntBuffer h        = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            STBImage.stbi_set_flip_vertically_on_load(true);
            ByteBuffer pixels = STBImage.stbi_load_from_memory(rawData, w, h, channels, 4);

            if (pixels == null) {
                String failureReason = STBImage.stbi_failure_reason();
                MemoryUtil.memFree(rawData);
                Logger.error(Logger.System.ASSET, "Failed to decode image data stream: %s. Reason: %s", virtualPath, failureReason);
                throw new RuntimeException("Failed to load texture: " + virtualPath + "\n" + failureReason);
            }

            this.width  = w.get(0);
            this.height = h.get(0);

            this.id = GL11.glGenTextures();
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
            
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
            
            // NEAREST sampling logic optimized for retro dungeon crawler crisp pixel art
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST_MIPMAP_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);

            // Housekeeping: purge native allocations immediately
            STBImage.stbi_image_free(pixels);
            MemoryUtil.memFree(rawData);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

            Logger.info(Logger.System.ASSET, "Texture successfully bound to VRAM memory slot. ID: %d [%dx%d]", id, width, height);
        }
    }

    @Override 
    public void bind(int slot) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + slot);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
    }
    
    @Override 
    public void unbind() { 
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0); 
    }
    
    @Override public int  getWidth()  { return width; }
    @Override public int  getHeight() { return height; }
    @Override public int  getID()     { return id; }
    
    @Override 
    public void cleanup() {
        Logger.info(Logger.System.ASSET, "Releasing hardware storage resource for texture ID: %d", id);
        GL11.glDeleteTextures(id); 
    }
}
