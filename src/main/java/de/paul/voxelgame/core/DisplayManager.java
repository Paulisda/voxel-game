package de.paul.voxelgame.core;

import de.paul.voxelgame.GameConfig;
import org.lwjgl.glfw.GLFWVidMode;

import static org.lwjgl.glfw.GLFW.glfwGetMonitorPos;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwGetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwSetWindowMonitor;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwSetWindowSize;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class DisplayManager {
    private static final DisplayMode[] MODES = {
            new DisplayMode("1080p", 1920, 1080),
            new DisplayMode("1440p", 2560, 1440),
            new DisplayMode("4K", 3840, 2160)
    };

    private int modeIndex;
    private boolean fullscreen;
    private boolean nativeModeResolved;
    private int windowedX = 100;
    private int windowedY = 80;
    private int windowedWidth = GameConfig.WIDTH;
    private int windowedHeight = GameConfig.HEIGHT;

    public void centerWindow(final long window) {
        final long monitor = glfwGetPrimaryMonitor();
        final GLFWVidMode videoMode = monitor == NULL ? null : glfwGetVideoMode(monitor);
        if (window == NULL || monitor == NULL || videoMode == null) {
            return;
        }

        final int[] monitorX = new int[1];
        final int[] monitorY = new int[1];
        final int[] windowWidth = new int[1];
        final int[] windowHeight = new int[1];
        glfwGetMonitorPos(monitor, monitorX, monitorY);
        glfwGetWindowSize(window, windowWidth, windowHeight);
        selectInitialMode(videoMode);

        final int x = monitorX[0] + Math.max(0, (videoMode.width() - windowWidth[0]) / 2);
        final int y = monitorY[0] + Math.max(0, (videoMode.height() - windowHeight[0]) / 2);
        glfwSetWindowPos(window, x, y);
        storeWindowedBounds(window);
    }

    public void toggleFullscreen(final long window) {
        setFullscreen(window, !fullscreen);
    }

    public void setFullscreen(final long window, final boolean enabled) {
        if (window == NULL || fullscreen == enabled) {
            return;
        }

        if (enabled) {
            storeWindowedBounds(window);
            fullscreen = true;
            applyFullscreen(window);
            return;
        }

        fullscreen = false;
        glfwSetWindowMonitor(window, NULL, windowedX, windowedY, windowedWidth, windowedHeight, 0);
    }

    public void previousResolution(final long window) {
        modeIndex = (modeIndex - 1 + MODES.length) % MODES.length;
        applyResolution(window);
    }

    public void nextResolution(final long window) {
        modeIndex = (modeIndex + 1) % MODES.length;
        applyResolution(window);
    }

    public boolean isFullscreen() {
        return fullscreen;
    }

    public String currentResolutionLabel() {
        return currentMode().label();
    }

    public DisplayMode currentMode() {
        return MODES[modeIndex];
    }

    private void applyResolution(final long window) {
        if (window == NULL) {
            return;
        }

        if (fullscreen) {
            applyFullscreen(window);
            return;
        }

        final DisplayMode mode = currentMode();
        final int[] size = fitWindowedSize(mode.width(), mode.height());
        windowedWidth = size[0];
        windowedHeight = size[1];
        glfwSetWindowSize(window, windowedWidth, windowedHeight);
        centerWindow(window);
    }

    private void applyFullscreen(final long window) {
        final long monitor = glfwGetPrimaryMonitor();
        final GLFWVidMode nativeMode = monitor == NULL ? null : glfwGetVideoMode(monitor);
        if (monitor == NULL || nativeMode == null) {
            return;
        }

        final DisplayMode mode = currentMode();
        final int width = Math.min(mode.width(), nativeMode.width());
        final int height = Math.min(mode.height(), nativeMode.height());
        glfwSetWindowMonitor(window, monitor, 0, 0, width, height, nativeMode.refreshRate());
    }

    private int[] fitWindowedSize(final int preferredWidth, final int preferredHeight) {
        final long monitor = glfwGetPrimaryMonitor();
        final GLFWVidMode nativeMode = monitor == NULL ? null : glfwGetVideoMode(monitor);
        if (nativeMode == null) {
            return new int[]{preferredWidth, preferredHeight};
        }

        final int maxWidth = Math.max(640, nativeMode.width() - 80);
        final int maxHeight = Math.max(480, nativeMode.height() - 120);
        final float scale = Math.min(1.0f, Math.min(maxWidth / (float) preferredWidth, maxHeight / (float) preferredHeight));
        return new int[]{
                Math.max(640, Math.round(preferredWidth * scale)),
                Math.max(480, Math.round(preferredHeight * scale))
        };
    }

    private void selectInitialMode(final GLFWVidMode nativeMode) {
        if (nativeModeResolved) {
            return;
        }

        nativeModeResolved = true;
        for (int i = 0; i < MODES.length; i++) {
            final DisplayMode mode = MODES[i];
            if (mode.width() <= nativeMode.width() && mode.height() <= nativeMode.height()) {
                modeIndex = i;
            }
        }
    }

    private void storeWindowedBounds(final long window) {
        if (window == NULL || fullscreen) {
            return;
        }

        final int[] x = new int[1];
        final int[] y = new int[1];
        final int[] width = new int[1];
        final int[] height = new int[1];
        glfwGetWindowPos(window, x, y);
        glfwGetWindowSize(window, width, height);
        windowedX = x[0];
        windowedY = y[0];
        windowedWidth = Math.max(640, width[0]);
        windowedHeight = Math.max(480, height[0]);
    }
}
