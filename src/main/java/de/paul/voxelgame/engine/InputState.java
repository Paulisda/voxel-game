package de.paul.voxelgame.engine;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_LAST;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LAST;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.glfwGetKey;
import static org.lwjgl.glfw.GLFW.glfwGetCursorPos;
import static org.lwjgl.glfw.GLFW.glfwGetMouseButton;
import static org.lwjgl.glfw.GLFW.glfwSetCharCallback;
import static org.lwjgl.glfw.GLFW.glfwSetScrollCallback;

public class InputState {
    private static final int KEY_COUNT = GLFW_KEY_LAST + 1;
    private static final int MOUSE_BUTTON_COUNT = GLFW_MOUSE_BUTTON_LAST + 1;

    private final long window;
    private final boolean[] currentKeys = new boolean[KEY_COUNT];
    private final boolean[] previousKeys = new boolean[KEY_COUNT];
    private final boolean[] currentMouseButtons = new boolean[MOUSE_BUTTON_COUNT];
    private final boolean[] previousMouseButtons = new boolean[MOUSE_BUTTON_COUNT];
    private double pendingScrollY;
    private double scrollY;
    private double mouseX;
    private double mouseY;
    private final StringBuilder pendingTypedText = new StringBuilder();
    private String typedText = "";

    public InputState(final long window) {
        this.window = window;
        glfwSetScrollCallback(window, (callbackWindow, xOffset, yOffset) -> pendingScrollY += yOffset);
        glfwSetCharCallback(window, (callbackWindow, codepoint) -> {
            if (Character.isValidCodePoint(codepoint) && !Character.isISOControl(codepoint)) {
                pendingTypedText.appendCodePoint(codepoint);
            }
        });
        update();
    }

    public void update() {
        System.arraycopy(currentKeys, 0, previousKeys, 0, KEY_COUNT);
        System.arraycopy(currentMouseButtons, 0, previousMouseButtons, 0, MOUSE_BUTTON_COUNT);
        scrollY = pendingScrollY;
        pendingScrollY = 0.0;
        typedText = pendingTypedText.toString();
        pendingTypedText.setLength(0);

        for (int key = 0; key < KEY_COUNT; key++) {
            currentKeys[key] = glfwGetKey(window, key) == GLFW_PRESS;
        }

        for (int button = 0; button < MOUSE_BUTTON_COUNT; button++) {
            currentMouseButtons[button] = glfwGetMouseButton(window, button) == GLFW_PRESS;
        }

        final double[] cursorX = new double[1];
        final double[] cursorY = new double[1];
        glfwGetCursorPos(window, cursorX, cursorY);
        mouseX = cursorX[0];
        mouseY = cursorY[0];
    }

    public boolean isKeyDown(final int key) {
        if (key < 0 || key >= KEY_COUNT) {
            return false;
        }
        return currentKeys[key];
    }

    public boolean isKeyPressed(final int key) {
        if (key < 0 || key >= KEY_COUNT) {
            return false;
        }
        return currentKeys[key] && !previousKeys[key];
    }

    public boolean isMouseDown(final int button) {
        if (button < 0 || button >= MOUSE_BUTTON_COUNT) {
            return false;
        }
        return currentMouseButtons[button];
    }

    public boolean isMousePressed(final int button) {
        if (button < 0 || button >= MOUSE_BUTTON_COUNT) {
            return false;
        }
        return currentMouseButtons[button] && !previousMouseButtons[button];
    }

    public boolean isMouseReleased(final int button) {
        if (button < 0 || button >= MOUSE_BUTTON_COUNT) {
            return false;
        }
        return !currentMouseButtons[button] && previousMouseButtons[button];
    }

    public double getMouseX() {
        return mouseX;
    }

    public double getMouseY() {
        return mouseY;
    }

    public double getScrollY() {
        return scrollY;
    }

    public String consumeTypedText() {
        final String result = typedText;
        typedText = "";
        return result;
    }

    public String typedText() {
        return typedText;
    }
}
