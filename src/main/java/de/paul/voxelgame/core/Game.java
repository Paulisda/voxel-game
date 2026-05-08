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
import static org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_E;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F11;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SLASH;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_TAB;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_T;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_UP;
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
    private static final String[] COMMAND_EXAMPLES = {
            "/gamemode creative",
            "/gamemode survival",
            "/time set day",
            "/time set night",
            "/time set noon",
            "/time set midnight",
            "/time set 6000"
    };

    private final RegistryManager registries;
    private final ResourceManager resources;
    private final ContentLoader contentLoader;
    private final InventorySystem inventorySystem;
    private final LocalizationManager localization;
    private final MenuSystem menuSystem;
    private final ChatSystem chatSystem;
    private final DisplayManager displayManager;
    private final EnvironmentSystem environmentSystem;
    private final MusicManager musicManager;
    private final SoundEffectManager soundEffectManager;
    private final WorldSystem worldSystem;

    private long window;
    private InputState inputState;
    private HudRenderer hudRenderer;
    private WorldRenderer worldRenderer;
    private Player player;
    private InventoryCarrySource inventoryCarrySource = InventoryCarrySource.NONE;
    private int inventoryCarrySourceSlot = -1;
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
        this.chatSystem = new ChatSystem();
        this.displayManager = new DisplayManager();
        this.environmentSystem = new EnvironmentSystem();
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

            final boolean consumedChatInput = handleChatInput();
            final boolean consumedEscapeInput = consumedChatInput ? false : handleEscapeInput();
            final boolean consumedInventorySearchInput = consumedChatInput ? false : handleInventorySearchInput();
            final boolean consumedInventoryInput = (consumedChatInput || consumedInventorySearchInput) ? false : handleInventoryInput();
            final boolean consumedDisplayInput = consumedChatInput ? false : handleDisplayInput();
            final boolean consumedMenuClick = consumedChatInput ? false : handleMenuClick(width, height);
            final boolean consumedInventoryClick = consumedChatInput ? false : handleInventoryClick(width, height);
            if (!consumedChatInput) {
                handleInventoryScroll(width, height);
                handleHotbarScroll();
            }

            final double now = glfwGetTime();
            double deltaSeconds = now - lastFrameTime;
            lastFrameTime = now;
            deltaSeconds = Math.max(1.0 / 240.0, Math.min(0.05, deltaSeconds));
            environmentSystem.update(deltaSeconds);

            if (!menuSystem.isOpen()
                    && !inventorySystem.isOpen()
                    && !chatSystem.isOpen()
                    && !consumedChatInput
                    && !consumedEscapeInput
                    && !consumedInventorySearchInput
                    && !consumedInventoryInput
                    && !consumedDisplayInput
                    && !consumedMenuClick
                    && !consumedInventoryClick) {
                player.update(inputState, deltaSeconds);
            }

            glViewport(0, 0, width, height);
            glClearColor(environmentSystem.skyRed(), environmentSystem.skyGreen(), environmentSystem.skyBlue(), 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            worldRenderer.render(player, width, height);
            hudRenderer.render(width, height, inputState.getMouseX(), inputState.getMouseY());
            if (!menuSystem.isOpen() && !inventorySystem.isOpen() && !chatSystem.isOpen()) {
                drawCrosshair(width, height);
            }

            glfwSwapBuffers(window);
        }
    }

    private boolean handleEscapeInput() {
        if (chatSystem.isOpen()) {
            return false;
        }
        if (!inputState.isKeyPressed(GLFW_KEY_ESCAPE)) {
            return false;
        }

        if (inventorySystem.isOpen()) {
            cancelInventoryDrag();
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

    private boolean handleChatInput() {
        if (!chatSystem.isOpen()) {
            if (menuSystem.isOpen() || inventorySystem.isOpen()) {
                return false;
            }
            if (inputState.isKeyPressed(GLFW_KEY_T)) {
                chatSystem.openChat();
                refreshChatSuggestions();
                inputState.consumeTypedText();
                player.releaseMouse();
                return true;
            }
            if (inputState.isKeyPressed(GLFW_KEY_SLASH) || inputState.typedText().contains("/")) {
                chatSystem.openCommand();
                refreshChatSuggestions();
                inputState.consumeTypedText();
                player.releaseMouse();
                return true;
            }
            return false;
        }

        boolean consumed = false;
        final String typedText = inputState.consumeTypedText();
        if (!typedText.isEmpty()) {
            chatSystem.appendText(typedText);
            consumed = true;
        }
        if (inputState.isKeyPressed(GLFW_KEY_BACKSPACE)) {
            chatSystem.removeLastCharacter();
            consumed = true;
        }
        if (inputState.isKeyPressed(GLFW_KEY_UP)) {
            chatSystem.previousHistory();
            consumed = true;
        }
        if (inputState.isKeyPressed(GLFW_KEY_DOWN)) {
            chatSystem.nextHistory();
            consumed = true;
        }
        if (inputState.isKeyPressed(GLFW_KEY_TAB)) {
            completeChatInput();
            consumed = true;
        }
        if (inputState.isKeyPressed(GLFW_KEY_ESCAPE)) {
            chatSystem.close();
            chatSystem.setSuggestions(java.util.List.of());
            player.captureMouse();
            return true;
        }
        if (inputState.isKeyPressed(GLFW_KEY_ENTER) || inputState.isKeyPressed(GLFW_KEY_KP_ENTER)) {
            handleSubmittedChat(chatSystem.submit());
            chatSystem.setSuggestions(java.util.List.of());
            player.captureMouse();
            return true;
        }
        refreshChatSuggestions();
        return consumed;
    }

    private void completeChatInput() {
        final java.util.List<String> suggestions = commandSuggestions(chatSystem.input());
        if (suggestions.isEmpty()) {
            return;
        }
        final String commonPrefix = commonPrefix(suggestions);
        if (commonPrefix.length() > chatSystem.input().length()) {
            chatSystem.setInput(commonPrefix);
        } else {
            chatSystem.setInput(suggestions.get(0));
        }
    }

    private void refreshChatSuggestions() {
        chatSystem.setSuggestions(commandSuggestions(chatSystem.input()));
    }

    private java.util.List<String> commandSuggestions(final String rawInput) {
        final String input = rawInput == null ? "" : rawInput.trim().toLowerCase(java.util.Locale.ROOT);
        if (input.isEmpty()) {
            return java.util.List.of("/gamemode creative", "/time set day", "/time set night");
        }
        if (!input.startsWith("/")) {
            return java.util.List.of();
        }

        final java.util.List<String> result = new java.util.ArrayList<>();
        for (final String example : COMMAND_EXAMPLES) {
            if (example.startsWith(input)) {
                result.add(example);
            }
        }
        return result;
    }

    private String commonPrefix(final java.util.List<String> values) {
        if (values.isEmpty()) {
            return "";
        }
        String prefix = values.get(0);
        for (int i = 1; i < values.size(); i++) {
            final String value = values.get(i);
            int length = Math.min(prefix.length(), value.length());
            int j = 0;
            while (j < length && prefix.charAt(j) == value.charAt(j)) {
                j++;
            }
            prefix = prefix.substring(0, j);
        }
        return prefix;
    }

    private void handleSubmittedChat(final String submittedText) {
        if (submittedText == null || submittedText.isBlank()) {
            return;
        }
        if (!submittedText.startsWith("/")) {
            chatSystem.addMessage("<Player> " + submittedText);
            return;
        }

        final String command = submittedText.substring(1).trim();
        if (command.isEmpty()) {
            return;
        }
        final String[] parts = command.toLowerCase(java.util.Locale.ROOT).split("\\s+");
        if ("gamemode".equals(parts[0]) || "gm".equals(parts[0])) {
            if (parts.length < 2) {
                chatSystem.addMessage("Gamemode: " + (GameConfig.isCreative() ? "creative" : "survival"));
                return;
            }
            switch (parts[1]) {
                case "1", "creative", "creativ", "c" -> {
                    GameConfig.setGameMode(GameConfig.GAMEMODE_CREATIVE);
                    chatSystem.addMessage("Gamemode auf Creative gesetzt.");
                }
                case "0", "survival", "s" -> {
                    GameConfig.setGameMode(GameConfig.GAMEMODE_SURVIVAL);
                    chatSystem.addMessage("Gamemode auf Survival gesetzt.");
                }
                default -> chatSystem.addMessage("Unbekannter Gamemode: " + parts[1]);
            }
            return;
        }
        if ("time".equals(parts[0])) {
            handleTimeCommand(parts);
            return;
        }

        chatSystem.addMessage("Unbekannter Befehl: /" + command);
    }

    private void handleTimeCommand(final String[] parts) {
        if (parts.length == 1) {
            chatSystem.addMessage("Zeit: " + environmentSystem.dayTime());
            return;
        }
        if (parts.length < 3 || !"set".equals(parts[1])) {
            chatSystem.addMessage("Benutzung: /time set day|night|<ticks>");
            return;
        }

        final Integer dayTime = parseDayTime(parts[2]);
        if (dayTime == null) {
            chatSystem.addMessage("Unbekannte Zeit: " + parts[2]);
            return;
        }
        environmentSystem.setDayTime(dayTime);
        chatSystem.addMessage("Zeit gesetzt: " + parts[2] + " (" + environmentSystem.dayTime() + ")");
    }

    private Integer parseDayTime(final String value) {
        return switch (value) {
            case "day" -> 1_000;
            case "noon", "mittag" -> 6_000;
            case "night", "nacht" -> 13_000;
            case "midnight", "mitternacht" -> 18_000;
            default -> parseTickTime(value);
        };
    }

    private Integer parseTickTime(final String value) {
        try {
            return Math.floorMod(Integer.parseInt(value), EnvironmentSystem.DAY_LENGTH_TICKS);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean handleInventoryInput() {
        if (chatSystem.isOpen()) {
            return false;
        }
        if (menuSystem.isOpen() || !inputState.isKeyPressed(GLFW_KEY_E)) {
            return false;
        }

        inputState.consumeTypedText();
        if (inventorySystem.isOpen()) {
            cancelInventoryDrag();
            inventorySystem.close();
            player.captureMouse();
        } else {
            inventorySystem.open();
            player.releaseMouse();
        }
        return true;
    }

    private boolean handleInventorySearchInput() {
        if (chatSystem.isOpen()) {
            return false;
        }
        if (!inventorySystem.isOpen()) {
            inputState.consumeTypedText();
            return false;
        }
        if (!GameConfig.isCreative() || !inventorySystem.isSearchFocused()) {
            inputState.consumeTypedText();
            return false;
        }

        boolean consumed = false;
        final String typedText = inputState.consumeTypedText();
        if (!typedText.isEmpty()) {
            inventorySystem.appendSearchText(typedText);
            consumed = true;
        }
        if (inputState.isKeyPressed(GLFW_KEY_BACKSPACE)) {
            inventorySystem.removeLastSearchCharacter();
            consumed = true;
        }
        if (inputState.isKeyPressed(GLFW_KEY_DELETE)) {
            inventorySystem.clearSearch();
            consumed = true;
        }
        return consumed;
    }

    private boolean handleDisplayInput() {
        if (chatSystem.isOpen()) {
            return false;
        }
        if (!inputState.isKeyPressed(GLFW_KEY_F11)) {
            return false;
        }

        displayManager.toggleFullscreen(window);
        if (!menuSystem.isOpen() && !inventorySystem.isOpen() && !chatSystem.isOpen()) {
            player.captureMouse();
        }
        return true;
    }

    private boolean handleMenuClick(final int width, final int height) {
        if (chatSystem.isOpen()) {
            return false;
        }
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
        if (chatSystem.isOpen()) {
            return false;
        }
        if (!inventorySystem.isOpen()) {
            return false;
        }

        if (!inputState.isMousePressed(GLFW_MOUSE_BUTTON_LEFT)) {
            return false;
        }
        return inventorySystem.carriedStack() == null
                ? pickUpInventoryStack(width, height)
                : placeCarriedInventoryStack(width, height);
    }

    private boolean pickUpInventoryStack(final int width, final int height) {
        if (inventorySystem.carriedStack() != null) {
            return false;
        }

        final double mouseX = inputState.getMouseX();
        final double mouseY = inputState.getMouseY();
        if (GameConfig.isCreative() && hudRenderer.isCreativeSearchField(mouseX, mouseY, width, height)) {
            inventorySystem.setSearchFocused(true);
            return true;
        }
        inventorySystem.setSearchFocused(false);

        int slot = hudRenderer.pickInventoryHotbarSlot(mouseX, mouseY, width, height);
        if (slot < 0) {
            slot = hudRenderer.pickHudHotbarSlot(mouseX, mouseY, width, height);
        }
        if (slot >= 0) {
            final InventoryStack stack = player.getHotbarStack(slot);
            if (stack == null) {
                return false;
            }
            inventoryCarrySource = InventoryCarrySource.HOTBAR;
            inventoryCarrySourceSlot = slot;
            inventorySystem.setCarriedStack(stack);
            player.setHotbarStack(slot, null);
            return true;
        }

        slot = hudRenderer.pickInventoryStorageSlot(mouseX, mouseY, width, height);
        if (slot >= 0) {
            final InventoryStack stack = inventorySystem.inventorySlot(slot);
            if (stack == null) {
                return false;
            }
            inventoryCarrySource = InventoryCarrySource.INVENTORY;
            inventoryCarrySourceSlot = slot;
            inventorySystem.setCarriedStack(stack);
            inventorySystem.setInventorySlot(slot, null);
            return true;
        }

        final var creativeItem = hudRenderer.pickInventoryItem(mouseX, mouseY, width, height);
        if (creativeItem == null) {
            return false;
        }
        inventoryCarrySource = InventoryCarrySource.CREATIVE;
        inventoryCarrySourceSlot = -1;
        inventorySystem.setCarriedStack(InventoryStack.fullStack(creativeItem));
        return true;
    }

    private boolean placeCarriedInventoryStack(final int width, final int height) {
        final double mouseX = inputState.getMouseX();
        final double mouseY = inputState.getMouseY();
        if (GameConfig.isCreative() && hudRenderer.isCreativeSearchField(mouseX, mouseY, width, height)) {
            inventorySystem.setSearchFocused(true);
            return true;
        }
        inventorySystem.setSearchFocused(false);

        int targetSlot = hudRenderer.pickInventoryHotbarSlot(mouseX, mouseY, width, height);
        if (targetSlot < 0) {
            targetSlot = hudRenderer.pickHudHotbarSlot(mouseX, mouseY, width, height);
        }
        if (targetSlot >= 0) {
            placeCarriedIntoHotbar(targetSlot);
            soundEffectManager.play(UI_TAP_EFFECT);
            return true;
        }

        targetSlot = hudRenderer.pickInventoryStorageSlot(mouseX, mouseY, width, height);
        if (targetSlot >= 0) {
            placeCarriedIntoInventory(targetSlot);
            soundEffectManager.play(UI_TAP_EFFECT);
            return true;
        }

        if (GameConfig.isCreative() && hudRenderer.isCreativeInventoryPanel(mouseX, mouseY, width, height)) {
            clearInventoryCarry();
            soundEffectManager.play(UI_TAP_EFFECT);
            return true;
        }

        return true;
    }

    private void placeCarriedIntoHotbar(final int slot) {
        final InventoryStack carried = inventorySystem.carriedStack();
        if (carried == null) {
            return;
        }
        final InventoryStack target = player.getHotbarStack(slot);
        if (target == null) {
            player.setHotbarStack(slot, carried);
            inventorySystem.setCarriedStack(null);
        } else if (target.canMerge(carried)) {
            target.mergeFrom(carried);
            player.setHotbarStack(slot, target);
            inventorySystem.setCarriedStack(carried.isEmpty() ? null : carried);
        } else {
            player.setHotbarStack(slot, carried);
            inventorySystem.setCarriedStack(target);
        }
        resetCarrySourceAfterPlace();
    }

    private void placeCarriedIntoInventory(final int slot) {
        final InventoryStack carried = inventorySystem.carriedStack();
        if (carried == null) {
            return;
        }
        final InventoryStack target = inventorySystem.inventorySlot(slot);
        if (target == null) {
            inventorySystem.setInventorySlot(slot, carried);
            inventorySystem.setCarriedStack(null);
        } else if (target.canMerge(carried)) {
            target.mergeFrom(carried);
            inventorySystem.setInventorySlot(slot, target);
            inventorySystem.setCarriedStack(carried.isEmpty() ? null : carried);
        } else {
            inventorySystem.setInventorySlot(slot, carried);
            inventorySystem.setCarriedStack(target);
        }
        resetCarrySourceAfterPlace();
    }

    private void resetCarrySourceAfterPlace() {
        inventoryCarrySource = InventoryCarrySource.NONE;
        inventoryCarrySourceSlot = -1;
    }

    private void cancelInventoryDrag() {
        final InventoryStack carriedStack = inventorySystem.carriedStack();
        if (carriedStack == null) {
            clearInventoryCarry();
            return;
        }
        if (inventoryCarrySource == InventoryCarrySource.HOTBAR) {
            player.setHotbarStack(inventoryCarrySourceSlot, carriedStack);
        } else if (inventoryCarrySource == InventoryCarrySource.INVENTORY) {
            inventorySystem.setInventorySlot(inventoryCarrySourceSlot, carriedStack);
        }
        clearInventoryCarry();
    }

    private void clearInventoryCarry() {
        inventorySystem.setCarriedStack(null);
        inventoryCarrySource = InventoryCarrySource.NONE;
        inventoryCarrySourceSlot = -1;
    }

    private void handleHotbarScroll() {
        if (menuSystem.isOpen() || inventorySystem.isOpen() || chatSystem.isOpen()) {
            return;
        }
        player.scrollHotbar(inputState.getScrollY());
    }

    private void handleInventoryScroll(final int width, final int height) {
        if (!inventorySystem.isOpen() || chatSystem.isOpen() || inventorySystem.carriedStack() != null) {
            return;
        }

        final double scrollY = inputState.getScrollY();
        if (scrollY > 0.0) {
            inventorySystem.previousPage(hudRenderer.inventoryPageCount(width, height));
        } else if (scrollY < 0.0) {
            inventorySystem.nextPage(hudRenderer.inventoryPageCount(width, height));
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
        player = new Player(window, world, inventorySystem.createDefaultHotbar(), soundEffectManager);
        final Vector3f spawnPoint = world.getSpawnPoint();
        player.teleport(spawnPoint.getX(), spawnPoint.getY(), spawnPoint.getZ(), -8.0, 225.0);
        player.captureMouse();

        worldRenderer = new WorldRenderer(world, registries, environmentSystem);
        hudRenderer = new HudRenderer(player, registries, inventorySystem, menuSystem, chatSystem, localization, displayManager, musicManager, soundEffectManager);

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

    private enum InventoryCarrySource {
        NONE,
        CREATIVE,
        HOTBAR,
        INVENTORY
    }
}
