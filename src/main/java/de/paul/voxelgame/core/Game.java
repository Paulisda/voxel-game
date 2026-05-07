package de.paul.voxelgame.core;

import de.paul.voxelgame.GameConfig;
import de.paul.voxelgame.assets.ResourceManager;
import de.paul.voxelgame.debug.GameDebug;
import de.paul.voxelgame.engine.InputState;
import de.paul.voxelgame.map.World;
import de.paul.voxelgame.math.Vector3f;
import de.paul.voxelgame.mob.Player;
import de.paul.voxelgame.objects.RegistryManager;
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

public class Game {
    private final RegistryManager registries;
    private final ResourceManager resources;
    private final ContentLoader contentLoader;
    private final InventorySystem inventorySystem;
    private final WorldSystem worldSystem;

    private long window;
    private InputState inputState;
    private HudRenderer hudRenderer;
    private WorldRenderer worldRenderer;
    private Player player;
    private double lastFrameTime;
    private int[] frameWidth;
    private int[] frameHeight;

    public Game() {
        this.registries = new RegistryManager();
        this.resources = new ResourceManager();
        this.contentLoader = new ContentLoader(registries, resources);
        this.inventorySystem = new InventorySystem(registries);
        this.worldSystem = new WorldSystem(registries);
    }

    public void run() {
        GameDebug.init();
        try {
            initWindow();
            initOpenGL();
            contentLoader.loadAll();
            initScene(worldSystem.createWorld(false));
            gameLoop();
        } finally {
            destroy();
        }
    }

    private void gameLoop() {
        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();
            inputState.update();

            if (inputState.isKeyPressed(GLFW_KEY_ESCAPE) && !player.isMouseCaptured()) {
                glfwSetWindowShouldClose(window, true);
            }

            final double now = glfwGetTime();
            double deltaSeconds = now - lastFrameTime;
            lastFrameTime = now;
            deltaSeconds = Math.max(1.0 / 240.0, Math.min(0.05, deltaSeconds));

            player.update(inputState, deltaSeconds);

            glfwGetFramebufferSize(window, frameWidth, frameHeight);
            final int width = Math.max(1, frameWidth[0]);
            final int height = Math.max(1, frameHeight[0]);

            glViewport(0, 0, width, height);
            glClearColor(0.53f, 0.77f, 1.0f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            worldRenderer.render(player, width, height);
            hudRenderer.render(width, height);
            drawCrosshair(width, height);

            glfwSwapBuffers(window);
        }
    }

    private void initOpenGL() {
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);
        GL.createCapabilities();

        glEnable(GL_DEPTH_TEST);
    }

    private void initScene(final World world) {
        inputState = new InputState(window);
        player = new Player(window, world, inventorySystem.createDefaultHotbar());
        final Vector3f spawnPoint = world.getSpawnPoint();
        player.teleport(spawnPoint.getX(), spawnPoint.getY(), spawnPoint.getZ(), -8.0, 225.0);
        player.captureMouse();

        worldRenderer = new WorldRenderer(world, registries);
        hudRenderer = new HudRenderer(player, registries);

        lastFrameTime = glfwGetTime();
        frameWidth = new int[1];
        frameHeight = new int[1];
    }

    private void initWindow() {
        if (!glfwInit()) {
            throw new IllegalStateException("GLFW konnte nicht initialisiert werden");
        }

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 2);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_ANY_PROFILE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);

        window = glfwCreateWindow(GameConfig.WIDTH, GameConfig.HEIGHT, "Voxel Game", NULL, NULL);
        if (window == NULL) {
            glfwTerminate();
            throw new RuntimeException("Fenster konnte nicht erstellt werden");
        }
    }

    private void destroy() {
        if (worldRenderer != null) {
            worldRenderer.destroy();
            worldRenderer = null;
        }
        if (hudRenderer != null) {
            hudRenderer.destroy();
            hudRenderer = null;
        }
        if (window != NULL) {
            glfwDestroyWindow(window);
            window = NULL;
        }
        glfwTerminate();
    }

    private static void drawCrosshair(final int width, final int height) {
        final float centerX = width * 0.5f;
        final float centerY = height * 0.5f;
        final float size = 8.0f;

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
