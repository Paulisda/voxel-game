package de.paul.voxelgame.core;

import de.paul.voxelgame.GameConfig;
import de.paul.voxelgame.audio.MusicManager;
import de.paul.voxelgame.audio.SoundEffectManager;
import de.paul.voxelgame.assets.ResourceManager;
import de.paul.voxelgame.debug.GameDebug;
import de.paul.voxelgame.engine.InputState;
import de.paul.voxelgame.map.World;
import de.paul.voxelgame.math.Vector3f;
import de.paul.voxelgame.mob.Player;
import de.paul.voxelgame.objects.RegistryManager;
import de.paul.voxelgame.objects.ResourceId;
import de.paul.voxelgame.renderer.HudRenderer;
import de.paul.voxelgame.renderer.WorldRenderer;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_E;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F11;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
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
    private static final ResourceId UI_TAP_EFFECT = ResourceId.of("game:tap");

    private final RegistryManager registries;
    private final ResourceManager resources;
    private final ContentLoader contentLoader;
    private final InventorySystem inventorySystem;
    private final LocalizationManager localization;
    private final MenuSystem menuSystem;
    private final DisplayManager displayManager;
    private final MusicManager musicManager;
    private final SoundEffectManager soundEffectManager;
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
        this.localization = new LocalizationManager(resources);
        this.menuSystem = new MenuSystem();
        this.displayManager = new DisplayManager();
        this.musicManager = new MusicManager(resources);
        this.soundEffectManager = new SoundEffectManager(resources);
        this.worldSystem = new WorldSystem(registries);
    }

    public void run() {
        GameDebug.init();
        try {
            initWindow();
            initOpenGL();
            contentLoader.loadAll();
            initScene(worldSystem.createWorld(false));
            musicManager.playFirstAvailableLooping();
            gameLoop();
        } finally {
            destroy();
        }
    }

    private void gameLoop() {
        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();
            inputState.update();

            glfwGetFramebufferSize(window, frameWidth, frameHeight);
            final int width = Math.max(1, frameWidth[0]);
            final int height = Math.max(1, frameHeight[0]);

            final boolean consumedEscapeInput = handleEscapeInput();
            final boolean consumedInventoryInput = handleInventoryInput();
            final boolean consumedDisplayInput = handleDisplayInput();
            final boolean consumedMenuClick = handleMenuClick(width, height);
            final boolean consumedInventoryClick = handleInventoryClick(width, height);
            handleHotbarScroll();

            final double now = glfwGetTime();
            double deltaSeconds = now - lastFrameTime;
            lastFrameTime = now;
            deltaSeconds = Math.max(1.0 / 240.0, Math.min(0.05, deltaSeconds));

            if (!menuSystem.isOpen()
                    && !inventorySystem.isOpen()
                    && !consumedEscapeInput
                    && !consumedInventoryInput
                    && !consumedDisplayInput
                    && !consumedMenuClick
                    && !consumedInventoryClick) {
                player.update(inputState, deltaSeconds);
            }

            glViewport(0, 0, width, height);
            glClearColor(0.53f, 0.77f, 1.0f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            worldRenderer.render(player, width, height);
            hudRenderer.render(width, height);
            if (!menuSystem.isOpen() && !inventorySystem.isOpen()) {
                drawCrosshair(width, height);
            }

            glfwSwapBuffers(window);
        }
    }

    private boolean handleEscapeInput() {
        if (!inputState.isKeyPressed(GLFW_KEY_ESCAPE)) {
            return false;
        }

        if (inventorySystem.isOpen()) {
            inventorySystem.close();
            player.captureMouse();
            return true;
        }

        if (menuSystem.isOpen()) {
            if (menuSystem.isOptions()) {
                menuSystem.openPause();
            } else {
                menuSystem.close();
                player.captureMouse();
            }
            return true;
        }

        menuSystem.openPause();
        player.releaseMouse();
        return true;
    }

    private boolean handleInventoryInput() {
        if (menuSystem.isOpen() || !inputState.isKeyPressed(GLFW_KEY_E)) {
            return false;
        }

        inventorySystem.toggle();
        if (inventorySystem.isOpen()) {
            player.releaseMouse();
        } else {
            player.captureMouse();
        }
        return true;
    }

    private boolean handleDisplayInput() {
        if (!inputState.isKeyPressed(GLFW_KEY_F11)) {
            return false;
        }

        displayManager.toggleFullscreen(window);
        if (!menuSystem.isOpen() && !inventorySystem.isOpen()) {
            player.captureMouse();
        }
        return true;
    }

    private boolean handleMenuClick(final int width, final int height) {
        if (!menuSystem.isOpen() || !inputState.isMousePressed(GLFW_MOUSE_BUTTON_LEFT)) {
            return false;
        }

        final MenuAction action = hudRenderer.pickMenuAction(inputState.getMouseX(), inputState.getMouseY(), width, height);
        switch (action) {
            case RESUME -> {
                soundEffectManager.play(UI_TAP_EFFECT);
                menuSystem.close();
                player.captureMouse();
                return true;
            }
            case OPTIONS -> {
                soundEffectManager.play(UI_TAP_EFFECT);
                menuSystem.openOptions();
                return true;
            }
            case EXIT -> {
                soundEffectManager.play(UI_TAP_EFFECT);
                glfwSetWindowShouldClose(window, true);
                return true;
            }
            case BACK -> {
                soundEffectManager.play(UI_TAP_EFFECT);
                menuSystem.openPause();
                return true;
            }
            case SENSITIVITY_DECREASE -> {
                soundEffectManager.play(UI_TAP_EFFECT);
                player.adjustMouseSensitivity(-0.01);
                return true;
            }
            case SENSITIVITY_INCREASE -> {
                soundEffectManager.play(UI_TAP_EFFECT);
                player.adjustMouseSensitivity(0.01);
                return true;
            }
            case MUSIC_VOLUME_DECREASE -> {
                soundEffectManager.play(UI_TAP_EFFECT);
                musicManager.adjustVolume(-0.05f);
                return true;
            }
            case MUSIC_VOLUME_INCREASE -> {
                soundEffectManager.play(UI_TAP_EFFECT);
                musicManager.adjustVolume(0.05f);
                return true;
            }
            case EFFECTS_VOLUME_DECREASE -> {
                soundEffectManager.play(UI_TAP_EFFECT);
                soundEffectManager.adjustVolume(-0.05f);
                return true;
            }
            case EFFECTS_VOLUME_INCREASE -> {
                soundEffectManager.play(UI_TAP_EFFECT);
                soundEffectManager.adjustVolume(0.05f);
                return true;
            }
            case FULLSCREEN_TOGGLE -> {
                soundEffectManager.play(UI_TAP_EFFECT);
                displayManager.toggleFullscreen(window);
                return true;
            }
            case RESOLUTION_PREVIOUS -> {
                soundEffectManager.play(UI_TAP_EFFECT);
                displayManager.previousResolution(window);
                return true;
            }
            case RESOLUTION_NEXT -> {
                soundEffectManager.play(UI_TAP_EFFECT);
                displayManager.nextResolution(window);
                return true;
            }
            case LANGUAGE_TOGGLE -> {
                soundEffectManager.play(UI_TAP_EFFECT);
                localization.toggleLanguage();
                return true;
            }
            case NONE -> {
                return false;
            }
        }
        return false;
    }

    private boolean handleInventoryClick(final int width, final int height) {
        if (!inventorySystem.isOpen() || !inputState.isMousePressed(GLFW_MOUSE_BUTTON_LEFT)) {
            return false;
        }

        final var pickedItem = hudRenderer.pickInventoryItem(inputState.getMouseX(), inputState.getMouseY(), width, height);
        if (pickedItem == null) {
            return false;
        }

        player.setHotbarItem(player.getSelectedHotbarSlot(), pickedItem);
        soundEffectManager.play(UI_TAP_EFFECT);
        inventorySystem.close();
        player.captureMouse();
        return true;
    }

    private void handleHotbarScroll() {
        if (menuSystem.isOpen() || inventorySystem.isOpen()) {
            return;
        }
        player.scrollHotbar(inputState.getScrollY());
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
        player = new Player(window, world, inventorySystem.createDefaultHotbar(), soundEffectManager);
        final Vector3f spawnPoint = world.getSpawnPoint();
        player.teleport(spawnPoint.getX(), spawnPoint.getY(), spawnPoint.getZ(), -8.0, 225.0);
        player.captureMouse();

        worldRenderer = new WorldRenderer(world, registries);
        hudRenderer = new HudRenderer(player, registries, inventorySystem, menuSystem, localization, displayManager, musicManager, soundEffectManager);

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
        displayManager.centerWindow(window);
    }

    private void destroy() {
        musicManager.destroy();
        soundEffectManager.destroy();
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
        final float scale = overlayScale(width, height);
        final float centerX = width * 0.5f;
        final float centerY = height * 0.5f;
        final float size = 8.0f * scale;

        glDisable(GL_DEPTH_TEST);

        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, width, height, 0, -1, 1);

        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        glLineWidth(2.0f * scale);
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

    private static float overlayScale(final int width, final int height) {
        final float scaleByWidth = width / 1920.0f;
        final float scaleByHeight = height / 1080.0f;
        return Math.max(1.0f, Math.min(2.0f, Math.min(scaleByWidth, scaleByHeight)));
    }
}
