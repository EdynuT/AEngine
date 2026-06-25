package com.aengine;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {

    private final String title;
    private int    width;
    private int    height;
    private long   handle;

    public Window(String title, int width, int height) {
        this.title  = title;
        this.width  = width;
        this.height = height;
    }

    public void init() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Failed to initialize GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE,        GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_VISIBLE,   GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        handle = glfwCreateWindow(width, height, title, NULL, NULL);
        if (handle == NULL) {
            throw new RuntimeException("Failed to create GLFW window");
        }

        glfwSetFramebufferSizeCallback(handle, (win, w, h) -> {
            width  = w;
            height = h;
            glViewport(0, 0, w, h);
        });

        GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (vidMode != null) {
            glfwSetWindowPos(handle,
                (vidMode.width()  - width)  / 2,
                (vidMode.height() - height) / 2);
        }

        glfwMakeContextCurrent(handle);
        glfwSwapInterval(1);
        GL.createCapabilities();

        glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
        glEnable(GL_DEPTH_TEST);

        glfwShowWindow(handle);
    }

    public void    swapBuffers() { glfwSwapBuffers(handle); }
    public boolean shouldClose() { return glfwWindowShouldClose(handle); }

    public void cleanup() {
        if (handle != NULL) glfwDestroyWindow(handle);
        glfwTerminate();
        GLFWErrorCallback cb = glfwSetErrorCallback(null);
        if (cb != null) cb.free();
    }

    public long   getHandle() { return handle; }
    public int    getWidth()  { return width; }
    public int    getHeight() { return height; }
    public String getTitle()  { return title; }
}
