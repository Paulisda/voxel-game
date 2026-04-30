package de.paul.voxelgame;

import de.paul.voxelgame.debug.GameDebug;
import de.paul.voxelgame.engine.InputState;
import de.paul.voxelgame.map.World;
import de.paul.voxelgame.math.Vector3f;
import de.paul.voxelgame.mob.Player;
import de.paul.voxelgame.renderer.HudRenderer;
import de.paul.voxelgame.renderer.WorldRenderer;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_ANY_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glLineWidth;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glOrtho;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glVertex2f;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Main {
    public static void main(String[] args) {
        GameDebug.init();

        if (!glfwInit()) {
            throw new IllegalStateException("GLFW konnte nicht initialisiert werden");
        }

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 2);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_ANY_PROFILE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);

        long window = glfwCreateWindow(GameConfig.WIDTH, GameConfig.HEIGHT, "Voxel Game", NULL, NULL);
        if (window == NULL) {
            glfwTerminate();
            throw new RuntimeException("Fenster konnte nicht erstellt werden");
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);
        GL.createCapabilities();

        glEnable(GL_DEPTH_TEST);

        World world = new World();
        world.generateWorld(false);

        InputState inputState = new InputState(window);
        Player player = new Player(window, world);
        Vector3f spawnPoint = world.getSpawnPoint();
        player.teleport(spawnPoint.getX(), spawnPoint.getY(), spawnPoint.getZ(), -8.0, 225.0);
        player.captureMouse();

        WorldRenderer worldRenderer = new WorldRenderer(world);
        HudRenderer hudRenderer = new HudRenderer(player);

        double lastFrameTime = glfwGetTime();
        int[] frameWidth = new int[1];
        int[] frameHeight = new int[1];

        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();
            inputState.update();

            if (inputState.isKeyPressed(GLFW_KEY_ESCAPE) && !player.isMouseCaptured()) {
                glfwSetWindowShouldClose(window, true);
            }

            double now = glfwGetTime();
            double deltaSeconds = now - lastFrameTime;
            lastFrameTime = now;
            deltaSeconds = Math.max(1.0 / 240.0, Math.min(0.05, deltaSeconds));

            player.update(inputState, deltaSeconds);

            glfwGetFramebufferSize(window, frameWidth, frameHeight);
            int width = Math.max(1, frameWidth[0]);
            int height = Math.max(1, frameHeight[0]);

            glViewport(0, 0, width, height);
            glClearColor(0.53f, 0.77f, 1.0f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            worldRenderer.render(player, width, height);
            hudRenderer.render(width, height);
            drawCrosshair(width, height);

            glfwSwapBuffers(window);
        }

        worldRenderer.destroy();
        hudRenderer.destroy();
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    private static void drawCrosshair(int width, int height) {
        float centerX = width * 0.5f;
        float centerY = height * 0.5f;
        float size = 8.0f;

        glDisable(GL_DEPTH_TEST);

        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, width, height, 0, -1, 1);

        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        glLineWidth(2.0f);
        glColor3f(1.0f, 1.0f, 1.0f);
        glBegin(GL_LINES);
        glVertex2f(centerX - size, centerY);
        glVertex2f(centerX + size, centerY);
        glVertex2f(centerX, centerY - size);
        glVertex2f(centerX, centerY + size);
        glEnd();

        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);

        glEnable(GL_DEPTH_TEST);
    }
}
