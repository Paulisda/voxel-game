package de.paul.voxelgame.engine;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_LAST;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LAST;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.glfwGetKey;
import static org.lwjgl.glfw.GLFW.glfwGetMouseButton;

public class InputState {
    private static final int KEY_COUNT = GLFW_KEY_LAST + 1;
    private static final int MOUSE_BUTTON_COUNT = GLFW_MOUSE_BUTTON_LAST + 1;

    private final long window;
    private final boolean[] currentKeys = new boolean[KEY_COUNT];
    private final boolean[] previousKeys = new boolean[KEY_COUNT];
    private final boolean[] currentMouseButtons = new boolean[MOUSE_BUTTON_COUNT];
    private final boolean[] previousMouseButtons = new boolean[MOUSE_BUTTON_COUNT];

    public InputState(long window) {
        this.window = window;
        update();
    }

    public void update() {
        System.arraycopy(currentKeys, 0, previousKeys, 0, KEY_COUNT);
        System.arraycopy(currentMouseButtons, 0, previousMouseButtons, 0, MOUSE_BUTTON_COUNT);

        for (int key = 0; key < KEY_COUNT; key++) {
            currentKeys[key] = glfwGetKey(window, key) == GLFW_PRESS;
        }

        for (int button = 0; button < MOUSE_BUTTON_COUNT; button++) {
            currentMouseButtons[button] = glfwGetMouseButton(window, button) == GLFW_PRESS;
        }
    }

    public boolean isKeyDown(int key) {
        if (key < 0 || key >= KEY_COUNT) {
            return false;
        }
        return currentKeys[key];
    }

    public boolean isKeyPressed(int key) {
        if (key < 0 || key >= KEY_COUNT) {
            return false;
        }
        return currentKeys[key] && !previousKeys[key];
    }

    public boolean isMouseDown(int button) {
        if (button < 0 || button >= MOUSE_BUTTON_COUNT) {
            return false;
        }
        return currentMouseButtons[button];
    }

    public boolean isMousePressed(int button) {
        if (button < 0 || button >= MOUSE_BUTTON_COUNT) {
            return false;
        }
        return currentMouseButtons[button] && !previousMouseButtons[button];
    }
}
