package com.aengine;

import static org.lwjgl.glfw.GLFW.*;

public class Input {

    private static final boolean[] keys         = new boolean[GLFW_KEY_LAST + 1];
    private static final boolean[] mouseButtons = new boolean[GLFW_MOUSE_BUTTON_LAST + 1];
    private static double mouseX, mouseY;

    private Input() {}

    public static void init(long windowHandle) {
        glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
            if (key >= 0 && key <= GLFW_KEY_LAST)
                keys[key] = (action != GLFW_RELEASE);
        });

        glfwSetMouseButtonCallback(windowHandle, (window, button, action, mods) -> {
            if (button >= 0 && button <= GLFW_MOUSE_BUTTON_LAST)
                mouseButtons[button] = (action != GLFW_RELEASE);
        });

        glfwSetCursorPosCallback(windowHandle, (window, xpos, ypos) -> {
            mouseX = xpos;
            mouseY = ypos;
        });
    }

    public static void    poll()    { glfwPollEvents(); }
    public static double  getMouseX() { return mouseX; }
    public static double  getMouseY() { return mouseY; }

    public static boolean isKeyPressed(int keyCode) {
        return keyCode >= 0 && keyCode <= GLFW_KEY_LAST && keys[keyCode];
    }

    public static boolean isMouseButtonPressed(int button) {
        return button >= 0 && button <= GLFW_MOUSE_BUTTON_LAST && mouseButtons[button];
    }
}
