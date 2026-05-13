package de.paul.voxelgame.renderer;

import de.paul.voxelgame.GameConfig;
import de.paul.voxelgame.assets.ResourcePackLoader;
import de.paul.voxelgame.audio.MusicManager;
import de.paul.voxelgame.core.ChatSystem;
import de.paul.voxelgame.audio.SoundEffectManager;
import de.paul.voxelgame.core.DisplayManager;
import de.paul.voxelgame.core.InventorySystem;
import de.paul.voxelgame.core.InventoryStack;
import de.paul.voxelgame.core.JsonParser;
import de.paul.voxelgame.core.LocalizationManager;
import de.paul.voxelgame.core.MenuAction;
import de.paul.voxelgame.core.MenuSystem;
import de.paul.voxelgame.core.TextureLoader;
import de.paul.voxelgame.mob.Player;
import de.paul.voxelgame.objects.BlockItemComponent;
import de.paul.voxelgame.objects.GameObject;
import de.paul.voxelgame.objects.ModelComponent;
import de.paul.voxelgame.objects.RegistryManager;
import de.paul.voxelgame.objects.ResourceId;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_FUNC;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_LEQUAL;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glDepthFunc;
import static org.lwjgl.opengl.GL11.glGetInteger;
import static org.lwjgl.opengl.GL11.glIsEnabled;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glOrtho;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glRotatef;
import static org.lwjgl.opengl.GL11.glScalef;
import static org.lwjgl.opengl.GL11.glTexCoord2f;
import static org.lwjgl.opengl.GL11.glTranslatef;
import static org.lwjgl.opengl.GL11.glVertex2f;
import static org.lwjgl.opengl.GL11.glVertex3f;

public class HudRenderer {
    private static final ResourceId DIRT_ID = ResourceId.of("game:dirt");
    private static final int HUD_SLOT_COUNT = 9;
    private static final int STAT_PIP_COUNT = 10;
    private static final int DEFAULT_HUD_SCALE = 2;
    private static final int MIN_HUD_SCALE = 1;
    private static final int MAX_HUD_SCALE = 8;
    private static final double SELECTED_ITEM_LABEL_HOLD_SECONDS = 2.2;
    private static final double SELECTED_ITEM_LABEL_FADE_SECONDS = 1.0;
    private static final float MIN_UI_SCALE = 1.0f;
    private static final float MAX_UI_SCALE = 2.0f;

    private static final int HOTBAR_U = 0;
    private static final int HOTBAR_V = 0;
    private static final int HOTBAR_W = 182;
    private static final int HOTBAR_H = 22;

    private static final int SLOT_SELECTOR_U = 0;
    private static final int SLOT_SELECTOR_V = 22;
    private static final int SLOT_SELECTOR_W = 24;
    private static final int SLOT_SELECTOR_H = 24;

    private static final int HEART_EMPTY_U = 16;
    private static final int HEART_HALF_U = 61;
    private static final int HEART_FULL_U = 52;
    private static final int HEART_V = 0;
    private static final int HUNGER_EMPTY_U = 16;
    private static final int HUNGER_HALF_U = 61;
    private static final int HUNGER_FULL_U = 52;
    private static final int HUNGER_V = 27;
    private static final int ICON_W = 9;
    private static final int ICON_H = 9;
    private static final int INVENTORY_COLUMNS = 9;
    private static final int CREATIVE_COLUMNS = 9;
    private static final int CREATIVE_ROWS = 5;
    private static final float CREATIVE_TEXTURE_SIZE = 512.0f;
    private static final float CREATIVE_SLOT_X = 16.0f;
    private static final float CREATIVE_SLOT_Y = 38.0f;
    private static final float CREATIVE_SLOT_SIZE = 35.0f;
    private static final float CREATIVE_SLOT_STEP = 36.0f;
    private static final float CREATIVE_HOTBAR_X = 14.0f;
    private static final float CREATIVE_HOTBAR_Y = 216.0f;
    private static final float CREATIVE_ICON_SIZE = 24.0f;
    private static final float CREATIVE_SEARCH_FIELD_X = 8.0f;
    private static final float CREATIVE_SEARCH_FIELD_Y = 5.0f;
    private static final float CREATIVE_SEARCH_FIELD_W = 246.0f;
    private static final float CREATIVE_SEARCH_FIELD_H = 28.0f;
    private static final float CREATIVE_SEARCH_TEXT_X = 24.0f;
    private static final float CREATIVE_SEARCH_TEXT_Y = 10.0f;
    private static final float CREATIVE_VISIBLE_CENTER_X = 188.5f;
    private static final float CREATIVE_VISIBLE_CENTER_Y = 134.5f;
    private static final float SURVIVAL_TEXTURE_SIZE = 512.0f;
    private static final float SURVIVAL_SLOT_X = 15.0f;
    private static final float SURVIVAL_STORAGE_Y = 168.0f;
    private static final float SURVIVAL_HOTBAR_Y = 275.0f;
    private static final float SURVIVAL_SLOT_SIZE = 34.0f;
    private static final float SURVIVAL_SLOT_STEP = 36.0f;
    private static final float SURVIVAL_ICON_SIZE = 24.0f;
    private static final float SURVIVAL_VISIBLE_CENTER_X = 190.5f;
    private static final float SURVIVAL_VISIBLE_CENTER_Y = 193.0f;
    private static final float SURVIVAL_LAYOUT_WIDTH = 454.0f;
    private static final float SURVIVAL_LAYOUT_HEIGHT = 420.0f;
    private static final float CREATIVE_LAYOUT_WIDTH = 506.0f;
    private static final float CREATIVE_LAYOUT_HEIGHT = 512.0f;
    private static final float INVENTORY_SLOT_SIZE = 42.0f;
    private static final float INVENTORY_SLOT_GAP = 6.0f;
    private static final Color TEXT_COLOR = new Color(242, 244, 246, 255);
    private static final Color MUTED_TEXT_COLOR = new Color(190, 197, 205, 255);

    private static final Color DEFAULT_GRASS_TINT = new Color(0x7F, 0xB2, 0x38);

    private final Player player;
    private final RegistryManager registries;
    private final InventorySystem inventorySystem;
    private final MenuSystem menuSystem;
    private final ChatSystem chatSystem;
    private final LocalizationManager localization;
    private final DisplayManager displayManager;
    private final MusicManager musicManager;
    private final SoundEffectManager soundEffectManager;
    private final ResourcePackLoader resourcePackLoader = new ResourcePackLoader();
    private final TextureLoader textureLoader = new TextureLoader();
    private final TextRenderer textRenderer = new TextRenderer();
    private final Map<ResourceId, Integer> objectIcons = new LinkedHashMap<>();
    private final Map<ResourceId, GuiItemModel> objectGuiModels = new LinkedHashMap<>();
    private final Map<String, ResolvedModelTextures> resolvedModelTextureCache = new LinkedHashMap<>();
    private final Map<String, ResolvedGuiModel> resolvedGuiModelCache = new LinkedHashMap<>();
    private final Map<String, Integer> guiModelTextureCache = new LinkedHashMap<>();
    private final List<Integer> guiPatchTextures = new ArrayList<>();
    private int fallbackIcon;
    private int viewportWidth;
    private int viewportHeight;

    private int hotbarBackgroundTexture;
    private int hotbarSelectorTexture;
    private int heartEmptyTexture;
    private int heartHalfTexture;
    private int heartFullTexture;
    private int hungerEmptyTexture;
    private int hungerHalfTexture;
    private int hungerFullTexture;
    private int inventoryPanelTexture;
    private int creativeInventoryItemsTexture;
    private int creativeInventorySearchTexture;
    private int optionsBackgroundTexture;
    private int menuButtonTexture;
    private GuiTexture survivalPlayerPanelPatch;
    private GuiTexture survivalCraftingPanelPatch;
    private GuiTexture inventorySlotPatch;
    private GuiTexture craftingSlotPatch;
    private GuiTexture selectedSlotPatch;
    private GuiTexture hotbarFramePatch;
    private GuiTexture creativeHeaderPatch;
    private GuiTexture creativeSearchHeaderPatch;
    private GuiTexture searchFieldPatch;
    private GuiTexture tabNormalPatch;
    private GuiTexture tabSelectedPatch;
    private GuiTexture scrollbarKnobPatch;
    private GuiTexture recipeButtonPatch;
    private int hudScaleSetting = DEFAULT_HUD_SCALE;
    private int selectedLabelSlot = -1;
    private ResourceId selectedLabelItemId;
    private double selectedLabelSeconds;

    public HudRenderer(
            final Player player,
            final RegistryManager registries,
            final InventorySystem inventorySystem,
            final MenuSystem menuSystem,
            final ChatSystem chatSystem,
            final LocalizationManager localization,
            final DisplayManager displayManager,
            final MusicManager musicManager,
            final SoundEffectManager soundEffectManager
    ) {
        this.player = player;
        this.registries = registries;
        this.inventorySystem = inventorySystem;
        this.menuSystem = menuSystem;
        this.chatSystem = chatSystem;
        this.localization = localization;
        this.displayManager = displayManager;
        this.musicManager = musicManager;
        this.soundEffectManager = soundEffectManager;
        loadHudTextures();
        loadObjectIcons();
    }

    public void update(final double deltaSeconds) {
        final int selectedSlot = player.getSelectedHotbarSlot();
        final GameObject selectedItem = player.getHotbarItem(selectedSlot);
        final ResourceId selectedItemId = selectedItem == null ? null : selectedItem.id();
        final boolean selectionChanged = selectedSlot != selectedLabelSlot || !sameSelectedItem(selectedItemId);

        if (selectionChanged) {
            selectedLabelSlot = selectedSlot;
            selectedLabelItemId = selectedItemId;
            selectedLabelSeconds = selectedItemId == null
                    ? 0.0
                    : SELECTED_ITEM_LABEL_HOLD_SECONDS + SELECTED_ITEM_LABEL_FADE_SECONDS;
            return;
        }

        if (selectedLabelSeconds > 0.0) {
            selectedLabelSeconds = Math.max(0.0, selectedLabelSeconds - Math.max(0.0, deltaSeconds));
        }
    }

    public int hudScaleSetting() {
        return hudScaleSetting;
    }

    public void adjustHudScale(final int delta) {
        hudScaleSetting = clampInt(hudScaleSetting + delta, MIN_HUD_SCALE, MAX_HUD_SCALE);
    }

    public void render(final int width, final int height, final double mouseX, final double mouseY) {
        if (width <= 0 || height <= 0) {
            return;
        }
        viewportWidth = width;
        viewportHeight = height;

        final boolean cullWasEnabled = glIsEnabled(GL_CULL_FACE);

        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, width, height, 0, -1, 1);

        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        final float scale = uiScale(width, height);
        final float hudScale = hudScale(width, height);
        final float hudTextScale = hudTextScale(scale);
        final float hotbarWidth = HOTBAR_W * hudScale;
        final float hotbarHeight = HOTBAR_H * hudScale;
        final float hotbarX = (width - hotbarWidth) * 0.5f;
        final float hotbarY = height - hotbarHeight - (12.0f * hudScale);

        drawTexturedQuad(hotbarBackgroundTexture, hotbarX, hotbarY, hotbarWidth, hotbarHeight);

        final int selected = player.getSelectedHotbarSlot();
        final float selectorX = hotbarX + ((selected * 20.0f) - 1.0f) * hudScale;
        final float selectorY = hotbarY - hudScale;
        drawTexturedQuad(hotbarSelectorTexture, selectorX, selectorY, SLOT_SELECTOR_W * hudScale, SLOT_SELECTOR_H * hudScale);

        final float iconSize = 16.0f * hudScale;
        for (int i = 0; i < HUD_SLOT_COUNT; i++) {
            final InventoryStack stack = player.getHotbarStack(i);
            if (stack == null) {
                continue;
            }
            final float iconX = hotbarX + (3 + i * 20) * hudScale;
            final float iconY = hotbarY + 3.0f * hudScale;
            drawStackIcon(stack, iconX, iconY, iconSize, font(11, hudTextScale));
        }

        final GameObject selectedItem = player.getHotbarItem(selected);
        if (selectedItem != null && selectedLabelSeconds > 0.0) {
            final String selectedName = localization.translate("ui.hotbar.selected") + ": " + localization.objectName(selectedItem);
            textRenderer.drawCenteredText(selectedName, width * 0.5f, hotbarY - s(42.0f, hudTextScale), font(14, hudTextScale), TEXT_COLOR, selectedItemLabelAlpha());
        }

        final float pipSize = ICON_W * hudScale;
        final float pipStep = 8.0f * hudScale;
        final float statsY = hotbarY - pipSize - (4.0f * hudScale);
        final float heartsX = hotbarX;
        final float hungerX = hotbarX + hotbarWidth - (pipSize + pipStep * (STAT_PIP_COUNT - 1));

        if (GameConfig.isSurvival()) {
            drawStatsRow(player.getHealthPoints(), heartsX, statsY, pipStep, pipSize, heartEmptyTexture, heartHalfTexture, heartFullTexture);
            drawStatsRow(player.getHungerPoints(), hungerX, statsY, pipStep, pipSize, hungerEmptyTexture, hungerHalfTexture, hungerFullTexture);
        }

        if (inventorySystem.isOpen()) {
            renderInventory(width, height);
        }
        if (menuSystem.isOpen()) {
            renderMenu(width, height, mouseX, mouseY);
        }
        renderChat(width, height, hotbarY, hudTextScale);
        final float guiScale = guiScale(width, height);
        renderInventoryTooltip(width, height, mouseX, mouseY, guiScale);
        renderCarriedItem(mouseX, mouseY, guiScale);

        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);

        glDisable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);
        if (cullWasEnabled) {
            glEnable(GL_CULL_FACE);
        }
        glEnable(GL_DEPTH_TEST);
    }

    public void destroy() {
        final Set<Integer> textureIds = new HashSet<>();
        textureIds.add(hotbarBackgroundTexture);
        textureIds.add(hotbarSelectorTexture);
        textureIds.add(heartEmptyTexture);
        textureIds.add(heartHalfTexture);
        textureIds.add(heartFullTexture);
        textureIds.add(hungerEmptyTexture);
        textureIds.add(hungerHalfTexture);
        textureIds.add(hungerFullTexture);
        textureIds.add(inventoryPanelTexture);
        textureIds.add(creativeInventoryItemsTexture);
        textureIds.add(creativeInventorySearchTexture);
        textureIds.add(optionsBackgroundTexture);
        textureIds.add(menuButtonTexture);
        textureIds.addAll(objectIcons.values());
        textureIds.addAll(guiModelTextureCache.values());
        textureIds.addAll(guiPatchTextures);

        for (final Integer textureId : textureIds) {
            textureLoader.deleteTexture(textureId == null ? 0 : textureId);
        }
        objectIcons.clear();
        objectGuiModels.clear();
        resolvedModelTextureCache.clear();
        resolvedGuiModelCache.clear();
        guiModelTextureCache.clear();
        guiPatchTextures.clear();
        textRenderer.destroy();
    }

    public GameObject pickInventoryItem(final double mouseX, final double mouseY, final int width, final int height) {
        if (!inventorySystem.isOpen() || !GameConfig.isCreative()) {
            return null;
        }

        final List<GameObject> entries = inventorySystem.filteredInventoryEntries(localization);
        final CreativeInventoryLayout layout = creativeInventoryLayout(entries.size(), width, height);
        inventorySystem.clampToPageCount(layout.pageCount());

        final int start = layout.page() * layout.pageSize();
        final int end = Math.min(entries.size(), start + layout.pageSize());
        for (int i = start; i < end; i++) {
            final int visibleIndex = i - start;
            final int column = visibleIndex % layout.columns();
            final int row = visibleIndex / layout.columns();
            final float slotX = layout.slotStartX() + column * (layout.slotSize() + layout.slotGap());
            final float slotY = layout.slotStartY() + row * (layout.slotSize() + layout.slotGap());
            if (mouseX >= slotX && mouseX <= slotX + layout.slotSize()
                    && mouseY >= slotY && mouseY <= slotY + layout.slotSize()) {
                return entries.get(i);
            }
        }
        return null;
    }

    public int pickInventoryStorageSlot(final double mouseX, final double mouseY, final int width, final int height) {
        if (!inventorySystem.isOpen() || GameConfig.isCreative()) {
            return -1;
        }
        final SurvivalInventoryLayout layout = survivalInventoryLayout(width, height);
        for (int i = 0; i < inventorySystem.inventorySlotCount(); i++) {
            final int column = i % INVENTORY_COLUMNS;
            final int row = i / INVENTORY_COLUMNS;
            final float slotX = layout.storageStartX() + column * (layout.slotSize() + layout.slotGap());
            final float slotY = layout.storageStartY() + row * (layout.slotSize() + layout.slotGap());
            if (containsSlot(mouseX, mouseY, slotX, slotY, layout.slotSize())) {
                return i;
            }
        }
        return -1;
    }

    public int pickInventoryHotbarSlot(final double mouseX, final double mouseY, final int width, final int height) {
        if (!inventorySystem.isOpen()) {
            return -1;
        }
        final SlotRowLayout layout = GameConfig.isCreative()
                ? creativeHotbarLayout(inventorySystem.filteredInventoryEntries(localization).size(), width, height)
                : survivalHotbarLayout(width, height);
        return pickSlotInRow(mouseX, mouseY, layout);
    }

    public boolean isCreativeSearchField(final double mouseX, final double mouseY, final int width, final int height) {
        if (!inventorySystem.isOpen() || !GameConfig.isCreative() || !inventorySystem.isSearchFocused()) {
            return false;
        }
        final CreativeInventoryLayout layout = creativeInventoryLayout(inventorySystem.filteredInventoryEntries(localization).size(), width, height);
        return mouseX >= layout.searchFieldX() && mouseX <= layout.searchFieldX() + layout.searchFieldWidth()
                && mouseY >= layout.searchFieldY() && mouseY <= layout.searchFieldY() + layout.searchFieldHeight();
    }

    public boolean isCreativeSearchTab(final double mouseX, final double mouseY, final int width, final int height) {
        return isCreativeTab(mouseX, mouseY, width, height, CREATIVE_COLUMNS - 3);
    }

    public boolean isCreativePrimaryTab(final double mouseX, final double mouseY, final int width, final int height) {
        return isCreativeTab(mouseX, mouseY, width, height, 0);
    }

    private boolean isCreativeTab(final double mouseX, final double mouseY, final int width, final int height, final int tabIndex) {
        if (!inventorySystem.isOpen() || !GameConfig.isCreative()) {
            return false;
        }
        final CreativeInventoryLayout layout = creativeInventoryLayout(inventorySystem.filteredInventoryEntries(localization).size(), width, height);
        final float tabSize = s(56.0f, layout.scale());
        final float gap = s(10.0f, layout.scale());
        final float tabX = layout.panelX() + s(2.0f, layout.scale()) + tabIndex * (tabSize + gap);
        final float tabY = layout.panelY();
        return mouseX >= tabX && mouseX <= tabX + tabSize
                && mouseY >= tabY && mouseY <= tabY + tabSize;
    }

    public boolean isCreativeInventoryPanel(final double mouseX, final double mouseY, final int width, final int height) {
        if (!inventorySystem.isOpen() || !GameConfig.isCreative()) {
            return false;
        }
        final CreativeInventoryLayout layout = creativeInventoryLayout(inventorySystem.filteredInventoryEntries(localization).size(), width, height);
        return mouseX >= layout.panelX() && mouseX <= layout.panelX() + layout.panelWidth()
                && mouseY >= layout.panelY() && mouseY <= layout.panelY() + layout.panelHeight();
    }

    public int pickHudHotbarSlot(final double mouseX, final double mouseY, final int width, final int height) {
        return pickSlotInRow(mouseX, mouseY, hudHotbarLayout(width, height));
    }

    public MenuAction pickMenuAction(final double mouseX, final double mouseY, final int width, final int height) {
        if (!menuSystem.isOpen()) {
            return MenuAction.NONE;
        }
        for (final MenuButton button : menuButtons(width, height)) {
            if (mouseX >= button.x() && mouseX <= button.x() + button.width()
                    && mouseY >= button.y() && mouseY <= button.y() + button.height()) {
                return button.action();
            }
        }
        return MenuAction.NONE;
    }

    public boolean applyMenuSliderClick(final double mouseX, final double mouseY, final int width, final int height) {
        if (!menuSystem.isOptions()) {
            return false;
        }

        final MenuButton[] buttons = menuButtons(width, height);
        float value = clickedSliderValue(buttons, MenuAction.FIELD_OF_VIEW_DECREASE, MenuAction.FIELD_OF_VIEW_INCREASE, mouseX, mouseY);
        if (!Float.isNaN(value)) {
            player.setFieldOfView(30.0f + value * 80.0f);
            return true;
        }

        value = clickedSliderValue(buttons, MenuAction.SENSITIVITY_DECREASE, MenuAction.SENSITIVITY_INCREASE, mouseX, mouseY);
        if (!Float.isNaN(value)) {
            player.setMouseSensitivity(0.02f + value * 0.28f);
            return true;
        }

        value = clickedSliderValue(buttons, MenuAction.HUD_SCALE_DECREASE, MenuAction.HUD_SCALE_INCREASE, mouseX, mouseY);
        if (!Float.isNaN(value)) {
            hudScaleSetting = clampInt(Math.round(MIN_HUD_SCALE + value * (MAX_HUD_SCALE - MIN_HUD_SCALE)), MIN_HUD_SCALE, MAX_HUD_SCALE);
            return true;
        }

        value = clickedSliderValue(buttons, MenuAction.VIEW_DISTANCE_DECREASE, MenuAction.VIEW_DISTANCE_INCREASE, mouseX, mouseY);
        if (!Float.isNaN(value)) {
            GameConfig.setViewDistanceChunks(Math.round(GameConfig.MIN_VIEW_DISTANCE_CHUNKS
                    + value * (GameConfig.MAX_VIEW_DISTANCE_CHUNKS - GameConfig.MIN_VIEW_DISTANCE_CHUNKS)));
            return true;
        }

        value = clickedSliderValue(buttons, MenuAction.MUSIC_VOLUME_DECREASE, MenuAction.MUSIC_VOLUME_INCREASE, mouseX, mouseY);
        if (!Float.isNaN(value)) {
            musicManager.setVolume(value);
            return true;
        }

        value = clickedSliderValue(buttons, MenuAction.EFFECTS_VOLUME_DECREASE, MenuAction.EFFECTS_VOLUME_INCREASE, mouseX, mouseY);
        if (!Float.isNaN(value)) {
            soundEffectManager.setVolume(value);
            return true;
        }

        return false;
    }

    public int inventoryPageCount(final int width, final int height) {
        if (!GameConfig.isCreative()) {
            return 1;
        }
        return creativeInventoryLayout(inventorySystem.filteredInventoryEntries(localization).size(), width, height).pageCount();
    }

    private SlotRowLayout hudHotbarLayout(final int width, final int height) {
        final float hudScale = hudScale(width, height);
        final float hotbarWidth = HOTBAR_W * hudScale;
        final float hotbarHeight = HOTBAR_H * hudScale;
        final float hotbarX = (width - hotbarWidth) * 0.5f;
        final float hotbarY = height - hotbarHeight - (12.0f * hudScale);
        return new SlotRowLayout(hotbarX, hotbarY, 20.0f * hudScale, 0.0f, player.getHotbarSize());
    }

    private SlotRowLayout creativeHotbarLayout(final int entryCount, final int width, final int height) {
        final CreativeInventoryLayout layout = creativeInventoryLayout(entryCount, width, height);
        return new SlotRowLayout(layout.hotbarStartX(), layout.hotbarStartY(), layout.slotSize(), layout.slotGap(), player.getHotbarSize());
    }

    private SlotRowLayout survivalHotbarLayout(final int width, final int height) {
        final SurvivalInventoryLayout layout = survivalInventoryLayout(width, height);
        return new SlotRowLayout(layout.hotbarStartX(), layout.hotbarStartY(), layout.slotSize(), layout.slotGap(), player.getHotbarSize());
    }

    private int pickSlotInRow(final double mouseX, final double mouseY, final SlotRowLayout layout) {
        for (int i = 0; i < layout.slotCount(); i++) {
            final float slotX = layout.slotStartX() + i * (layout.slotSize() + layout.slotGap());
            if (containsSlot(mouseX, mouseY, slotX, layout.slotStartY(), layout.slotSize())) {
                return i;
            }
        }
        return -1;
    }

    private boolean containsSlot(final double mouseX, final double mouseY, final float slotX, final float slotY, final float slotSize) {
        return mouseX >= slotX && mouseX <= slotX + slotSize
                && mouseY >= slotY && mouseY <= slotY + slotSize;
    }

    private void renderInventory(final int width, final int height) {
        if (GameConfig.isCreative()) {
            renderCreativeInventory(width, height);
        } else {
            renderSurvivalInventory(width, height);
        }
    }

    private void renderSurvivalInventory(final int width, final int height) {
        final SurvivalInventoryLayout layout = survivalInventoryLayout(width, height);

        drawInventoryBackdrop(width, height, layout.scale());
        drawSurvivalInventoryChrome(layout);
        renderStorageItems(layout);
        renderHotbarItems(layout.hotbarStartX(), layout.hotbarStartY(), layout.slotSize(), layout.slotGap(), layout.iconSize());
    }

    private void renderCreativeInventory(final int width, final int height) {
        final List<GameObject> entries = inventorySystem.filteredInventoryEntries(localization);
        final CreativeInventoryLayout layout = creativeInventoryLayout(entries.size(), width, height);
        inventorySystem.clampToPageCount(layout.pageCount());

        drawInventoryBackdrop(width, height, layout.scale());
        drawCreativeInventoryChrome(layout, entries);
        if (layout.pageCount() > 1) {
            final String pageLabel = (layout.page() + 1) + "/" + layout.pageCount();
            textRenderer.drawCenteredText(pageLabel, layout.panelX() + layout.panelWidth() - s(42.0f, layout.scale()),
                    layout.slotStartY() - s(28.0f, layout.scale()), font(12, layout.scale()), MUTED_TEXT_COLOR);
        }
        drawCreativeSearchText(layout);

        final int start = layout.page() * layout.pageSize();
        final int end = Math.min(entries.size(), start + layout.pageSize());
        if (start == end) {
            textRenderer.drawCenteredText(localization.translate("ui.inventory.no_results"), layout.panelX() + layout.panelWidth() * 0.5f, layout.slotStartY() + layout.slotSize(), font(13, layout.scale()), MUTED_TEXT_COLOR);
            renderHotbarItems(layout.hotbarStartX(), layout.hotbarStartY(), layout.slotSize(), layout.slotGap(), layout.iconSize());
            return;
        }
        for (int i = start; i < end; i++) {
            final int visibleIndex = i - start;
            final int column = visibleIndex % layout.columns();
            final int row = visibleIndex / layout.columns();
            final float slotX = layout.slotStartX() + column * (layout.slotSize() + layout.slotGap());
            final float slotY = layout.slotStartY() + row * (layout.slotSize() + layout.slotGap());
            final GameObject entry = entries.get(i);

            final float iconX = slotX + (layout.slotSize() - layout.iconSize()) * 0.5f;
            final float iconY = slotY + (layout.slotSize() - layout.iconSize()) * 0.5f;
            drawItemIcon(entry, iconX, iconY, layout.iconSize());
        }
        renderHotbarItems(layout.hotbarStartX(), layout.hotbarStartY(), layout.slotSize(), layout.slotGap(), layout.iconSize());
    }

    private void drawCreativeSearchText(final CreativeInventoryLayout layout) {
        final String query = inventorySystem.searchQuery();
        if (!inventorySystem.isSearchFocused()) {
            textRenderer.drawBoldText(localized("Baubl\u00f6cke", "Building Blocks"),
                    layout.searchX(), layout.searchY(), font(17, layout.scale()), TEXT_COLOR);
            return;
        }

        final String searchText = query.isBlank() ? localization.translate("ui.inventory.search") : shortName(query, 36);
        final Color color = query.isBlank() ? MUTED_TEXT_COLOR : TEXT_COLOR;
        textRenderer.drawBoldText(searchText, layout.searchX(), layout.searchY(), font(15, layout.scale()), color);
    }

    private void drawInventoryBackdrop(final int width, final int height, final float scale) {
        drawColoredQuad(0, 0, width, height, 0.03f, 0.035f, 0.04f, 0.40f);
        drawTiledTexture(optionsBackgroundTexture, 0.0f, 0.0f, width, height, s(32.0f, scale), 0.22f);
        drawColoredQuad(0, 0, width, height, 0.02f, 0.015f, 0.04f, 0.18f);
    }

    private void drawSurvivalInventoryChrome(final SurvivalInventoryLayout layout) {
        final float scale = layout.scale();
        final float playerPanelX = layout.panelX();
        final float playerPanelY = layout.panelY();
        final float playerPanelW = s(210.0f, scale);
        final float playerPanelH = s(210.0f, scale);
        final float craftX = layout.panelX() + s(234.0f, scale);
        final float craftY = layout.panelY() + s(4.0f, scale);
        final float craftW = s(220.0f, scale);
        final float craftH = s(150.0f, scale);

        drawGuiPatch(survivalPlayerPanelPatch, playerPanelX, playerPanelY, playerPanelW, playerPanelH);
        drawInventoryPreviewBox(playerPanelX + s(62.0f, scale), playerPanelY + s(12.0f, scale), s(96.0f, scale), s(178.0f, scale), scale);

        drawGuiPatch(survivalCraftingPanelPatch, craftX, craftY, craftW, craftH);
        drawRecipeIcon(craftX + s(36.0f, scale), craftY + craftH + s(12.0f, scale), scale);

        drawInventorySlotGrid(layout.storageStartX(), layout.storageStartY(), INVENTORY_COLUMNS, 3,
                layout.slotSize(), layout.slotGap(), scale, -1);

        final float hotbarW = player.getHotbarSize() * layout.slotSize() + (player.getHotbarSize() - 1) * layout.slotGap();
        drawGuiPatch(hotbarFramePatch, layout.hotbarStartX() - s(8.0f, scale), layout.hotbarStartY() - s(8.0f, scale),
                hotbarW + s(16.0f, scale), layout.slotSize() + s(16.0f, scale));
        drawInventorySlotGrid(layout.hotbarStartX(), layout.hotbarStartY(), player.getHotbarSize(), 1,
                layout.slotSize(), layout.slotGap(), scale, player.getSelectedHotbarSlot());
    }

    private void drawCreativeInventoryChrome(final CreativeInventoryLayout layout, final List<GameObject> entries) {
        final float scale = layout.scale();
        drawCreativeTabs(layout, entries);
        if (inventorySystem.isSearchFocused()) {
            drawGuiPatch(creativeSearchHeaderPatch, layout.searchFieldX(), layout.searchFieldY(), s(334.0f, scale), layout.searchFieldHeight());
            drawNineSlicePatch(searchFieldPatch, layout.searchFieldX() + s(146.0f, scale), layout.searchFieldY() + s(5.0f, scale),
                    layout.searchFieldWidth() - s(152.0f, scale), layout.searchFieldHeight() - s(10.0f, scale), s(7.0f, scale));
        } else {
            drawGuiPatch(creativeHeaderPatch, layout.searchFieldX(), layout.searchFieldY(), layout.searchFieldWidth(), layout.searchFieldHeight());
        }
        drawInventorySlotGrid(layout.slotStartX(), layout.slotStartY(), layout.columns(), layout.rows(),
                layout.slotSize(), layout.slotGap(), scale, -1);
        drawCreativeScrollbar(layout);

        final float hotbarW = player.getHotbarSize() * layout.slotSize() + (player.getHotbarSize() - 1) * layout.slotGap();
        drawGuiPatch(hotbarFramePatch, layout.hotbarStartX() - s(8.0f, scale), layout.hotbarStartY() - s(8.0f, scale),
                hotbarW + s(16.0f, scale), layout.slotSize() + s(16.0f, scale));
        drawInventorySlotGrid(layout.hotbarStartX(), layout.hotbarStartY(), player.getHotbarSize(), 1,
                layout.slotSize(), layout.slotGap(), scale, player.getSelectedHotbarSlot());
        drawCreativeBottomTabs(layout, entries);
    }

    private void drawArmorSlots(final float x, final float y, final float scale) {
        final String[] labels = {"H", "C", "L", "B"};
        final float slotSize = s(INVENTORY_SLOT_SIZE, scale);
        final float step = slotSize + s(6.0f, scale);
        for (int i = 0; i < labels.length; i++) {
            drawEquipmentSlot(x, y + i * step, slotSize, scale, labels[i]);
        }
    }

    private void drawEquipmentSlot(final float x, final float y, final float size, final float scale, final String label) {
        drawInventorySlot(x, y, size, scale, false);
        textRenderer.drawCenteredBoldText(label, x + size * 0.5f, y + size * 0.25f, font(18, scale), new Color(28, 220, 255, 210));
    }

    private void drawInventoryPreviewBox(final float x, final float y, final float width, final float height, final float scale) {
        drawPlayerSilhouette(x + width * 0.5f, y + height * 0.57f, scale);
    }

    private void drawPlayerSilhouette(final float centerX, final float centerY, final float scale) {
        final float pixel = s(5.0f, scale);
        final float head = pixel * 7.0f;
        final float bodyW = pixel * 8.0f;
        final float bodyH = pixel * 14.0f;
        final float headX = centerX - head * 0.5f;
        final float headY = centerY - bodyH * 0.92f - head;
        drawColoredQuad(headX, headY, head, head, 0.94f, 0.65f, 0.48f, 1.0f);
        drawColoredQuad(headX, headY, head, pixel * 2.0f, 0.26f, 0.10f, 0.08f, 1.0f);
        drawColoredQuad(centerX - bodyW * 0.5f, centerY - bodyH * 0.9f, bodyW, bodyH, 0.08f, 0.17f, 0.30f, 1.0f);
        drawColoredQuad(centerX - pixel * 1.0f, centerY - bodyH * 0.9f, pixel * 2.0f, bodyH, 0.86f, 0.88f, 0.90f, 1.0f);
        drawColoredQuad(centerX - bodyW * 0.55f, centerY + pixel * 4.0f, pixel * 3.0f, pixel * 11.0f, 0.05f, 0.07f, 0.10f, 1.0f);
        drawColoredQuad(centerX + bodyW * 0.20f, centerY + pixel * 4.0f, pixel * 3.0f, pixel * 11.0f, 0.05f, 0.07f, 0.10f, 1.0f);
        drawColoredQuad(centerX - bodyW * 0.96f, centerY - bodyH * 0.75f, pixel * 3.0f, pixel * 13.0f, 0.94f, 0.65f, 0.48f, 1.0f);
        drawColoredQuad(centerX + bodyW * 0.58f, centerY - bodyH * 0.75f, pixel * 3.0f, pixel * 13.0f, 0.94f, 0.65f, 0.48f, 1.0f);
    }

    private void drawCraftingHeader(final float x, final float y, final float width, final float height, final float scale) {
        drawColoredQuad(x, y, width, height, 0.035f, 0.016f, 0.065f, 0.98f);
        drawColoredQuad(x + s(3.0f, scale), y + s(3.0f, scale), width - s(6.0f, scale), height - s(6.0f, scale),
                1.0f, 0.75f, 0.06f, 1.0f);
        drawCenteredMenuText(localized("Handwerk", "Crafting"), x + width * 0.5f, y + height * 0.19f, font(17, scale), scale, new Color(50, 22, 8, 255));
    }

    private void drawCraftingGrid(final float craftX, final float craftY, final float scale) {
        final float slotSize = s(INVENTORY_SLOT_SIZE, scale);
        final float gap = s(6.0f, scale);
        final float startX = craftX + s(16.0f, scale);
        final float startY = craftY + s(48.0f, scale);
        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 2; column++) {
                drawCraftingSlot(startX + column * (slotSize + gap), startY + row * (slotSize + gap), slotSize, scale);
            }
        }

        final float arrowX = craftX + s(112.0f, scale);
        final float arrowY = craftY + s(80.0f, scale);
        drawColoredQuad(arrowX, arrowY, s(36.0f, scale), s(6.0f, scale), 1.0f, 0.78f, 0.02f, 1.0f);
        drawColoredQuad(arrowX + s(28.0f, scale), arrowY - s(7.0f, scale), s(8.0f, scale), s(20.0f, scale),
                1.0f, 0.78f, 0.02f, 1.0f);
        drawCraftingSlot(craftX + s(164.0f, scale), craftY + s(70.0f, scale), slotSize, scale);
    }

    private void drawRecipeIcon(final float x, final float y, final float scale) {
        drawGuiPatch(recipeButtonPatch, x, y, s(42.0f, scale), s(38.0f, scale));
    }

    private void drawCreativeTabs(final CreativeInventoryLayout layout, final List<GameObject> entries) {
        final float scale = layout.scale();
        final float tabSize = s(56.0f, scale);
        final float gap = s(10.0f, scale);
        final float startX = layout.panelX() + s(2.0f, scale);
        final float y = layout.panelY();
        for (int i = 0; i < 7; i++) {
            final float x = startX + i * (tabSize + gap);
            final boolean selected = inventorySystem.isSearchFocused() ? i == 6 : i == 0;
            drawInventoryTab(x, y, tabSize, scale, selected);
            if (i == 6) {
                drawCompassIcon(x + s(13.0f, scale), y + s(13.0f, scale), s(30.0f, scale));
            } else {
                drawEntryIcon(entries, i * 3, x + s(13.0f, scale), y + s(13.0f, scale), s(30.0f, scale));
            }
        }
    }

    private void drawCreativeBottomTabs(final CreativeInventoryLayout layout, final List<GameObject> entries) {
        final float scale = layout.scale();
        final float tabSize = s(56.0f, scale);
        final float gap = s(12.0f, scale);
        final float y = layout.panelY() + s(438.0f, scale);
        final float x = layout.panelX() + s(2.0f, scale);
        for (int i = 0; i < 5; i++) {
            drawInventoryTab(x + i * (tabSize + gap), y, tabSize, scale, false);
            drawEntryIcon(entries, i * 5 + 1, x + i * (tabSize + gap) + s(13.0f, scale), y + s(13.0f, scale), s(30.0f, scale));
        }
        drawInventoryTab(layout.panelX() + layout.panelWidth() - tabSize - s(4.0f, scale), y, tabSize, scale, false);
        drawColoredQuad(layout.panelX() + layout.panelWidth() - tabSize + s(14.0f, scale), y + s(15.0f, scale),
                s(28.0f, scale), s(24.0f, scale), 0.55f, 0.29f, 0.15f, 1.0f);
        drawColoredQuad(layout.panelX() + layout.panelWidth() - tabSize + s(10.0f, scale), y + s(10.0f, scale),
                s(36.0f, scale), s(8.0f, scale), 0.74f, 0.43f, 0.22f, 1.0f);
    }

    private void drawEntryIcon(final List<GameObject> entries, final int index, final float x, final float y, final float size) {
        if (entries.isEmpty()) {
            return;
        }
        drawItemIcon(entries.get(Math.min(index, entries.size() - 1)), x, y, size);
    }

    private void drawCompassIcon(final float x, final float y, final float size) {
        GameObject compass = registries.items().find(ResourceId.of("game:compass")).orElse(null);
        if (compass == null) {
            compass = registries.items().find(ResourceId.of("game:compass_")).orElse(null);
        }
        if (compass == null) {
            drawEntryIcon(inventorySystem.allInventoryEntries(), 0, x, y, size);
            return;
        }
        drawItemIcon(compass, x, y, size);
    }

    private void drawCreativeScrollbar(final CreativeInventoryLayout layout) {
        final float scale = layout.scale();
        final float gridHeight = layout.rows() * layout.slotSize() + (layout.rows() - 1) * layout.slotGap();
        final float barX = layout.slotStartX() + layout.columns() * layout.slotSize()
                + (layout.columns() - 1) * layout.slotGap() + s(16.0f, scale);
        final float barW = s(22.0f, scale);
        drawNineSlicePatch(searchFieldPatch, barX, layout.slotStartY(), barW, gridHeight, s(6.0f, scale));
        final float available = gridHeight - s(42.0f, scale);
        final float knobY = layout.slotStartY() + s(5.0f, scale)
                + (layout.pageCount() <= 1 ? 0.0f : layout.page() / (float) (layout.pageCount() - 1) * Math.max(0.0f, available));
        drawGuiPatch(scrollbarKnobPatch, barX - s(3.0f, scale), knobY, barW + s(6.0f, scale), s(38.0f, scale));
    }

    private void drawInventorySlotGrid(
            final float startX,
            final float startY,
            final int columns,
            final int rows,
            final float slotSize,
            final float slotGap,
            final float scale,
            final int selectedIndex
    ) {
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                final int index = row * columns + column;
                drawInventorySlot(startX + column * (slotSize + slotGap), startY + row * (slotSize + slotGap),
                        slotSize, scale, index == selectedIndex);
            }
        }
    }

    private void drawInventoryBlueFrame(final float x, final float y, final float width, final float height, final float scale) {
        drawNineSlicePatch(hotbarFramePatch, x, y, width, height, s(10.0f, scale));
    }

    private void drawInventoryOrangeFrame(final float x, final float y, final float width, final float height, final float scale) {
        drawNineSlicePatch(survivalCraftingPanelPatch, x, y, width, height, s(10.0f, scale));
    }

    private void drawInventoryCyanBar(final float x, final float y, final float width, final float height, final float scale) {
        drawNineSlicePatch(creativeHeaderPatch, x, y, width, height, s(8.0f, scale));
    }

    private void drawInventorySlot(final float x, final float y, final float size, final float scale, final boolean selected) {
        drawGuiPatch(selected ? selectedSlotPatch : inventorySlotPatch, x, y, size, size);
    }

    private void drawCraftingSlot(final float x, final float y, final float size, final float scale) {
        drawGuiPatch(craftingSlotPatch, x, y, size, size);
    }

    private void drawInventoryTab(final float x, final float y, final float size, final float scale, final boolean selected) {
        drawGuiPatch(selected ? tabSelectedPatch : tabNormalPatch, x, y, size, size);
    }

    private void renderStorageItems(final SurvivalInventoryLayout layout) {
        for (int i = 0; i < inventorySystem.inventorySlotCount(); i++) {
            final InventoryStack stack = inventorySystem.inventorySlot(i);
            if (stack == null) {
                continue;
            }
            final int column = i % INVENTORY_COLUMNS;
            final int row = i / INVENTORY_COLUMNS;
            final float slotX = layout.storageStartX() + column * (layout.slotSize() + layout.slotGap());
            final float slotY = layout.storageStartY() + row * (layout.slotSize() + layout.slotGap());
            drawSlotStack(stack, slotX, slotY, layout.slotSize(), layout.iconSize(), Math.max(8, Math.round(layout.iconSize() * 0.42f)));
        }
    }

    private void renderHotbarItems(final float slotStartX, final float slotStartY, final float slotSize, final float slotGap, final float iconSize) {
        for (int i = 0; i < player.getHotbarSize(); i++) {
            final InventoryStack stack = player.getHotbarStack(i);
            if (stack == null) {
                continue;
            }
            final float slotX = slotStartX + i * (slotSize + slotGap);
            drawSlotStack(stack, slotX, slotStartY, slotSize, iconSize, font(10, 1.0f));
        }
    }

    private void drawSlotStack(final InventoryStack stack, final float slotX, final float slotY, final float slotSize, final float iconSize, final int countFont) {
        final float iconX = slotX + (slotSize - iconSize) * 0.5f;
        final float iconY = slotY + (slotSize - iconSize) * 0.5f;
        drawStackIcon(stack, iconX, iconY, iconSize, countFont);
    }

    private void drawStackIcon(final InventoryStack stack, final float iconX, final float iconY, final float iconSize, final int countFont) {
        drawItemIcon(stack.item(), iconX, iconY, iconSize);
        if (stack.count() <= 1) {
            return;
        }
        final String count = Integer.toString(stack.count());
        final float labelX = iconX + iconSize - Math.max(9.0f, count.length() * countFont * 0.45f);
        final float labelY = iconY + iconSize - countFont * 0.82f;
        drawColoredQuad(labelX - 1.0f, labelY - 1.0f, count.length() * countFont * 0.55f + 2.0f, countFont + 2.0f, 0.0f, 0.0f, 0.0f, 0.48f);
        textRenderer.drawText(count, labelX, labelY, countFont, TEXT_COLOR);
    }

    private void drawItemIcon(final GameObject item, final float iconX, final float iconY, final float iconSize) {
        if (!drawGuiModelIcon(item, iconX, iconY, iconSize)) {
            drawTexturedQuad(iconFor(item), iconX, iconY, iconSize, iconSize);
        }
    }

    private CreativeInventoryLayout creativeInventoryLayout(final int entryCount, final int width, final int height) {
        final float scale = inventoryScale(width, height, CREATIVE_LAYOUT_WIDTH, CREATIVE_LAYOUT_HEIGHT);
        final float panelWidth = s(CREATIVE_LAYOUT_WIDTH, scale);
        final float panelHeight = s(CREATIVE_LAYOUT_HEIGHT, scale);
        final float panelX = (width - panelWidth) * 0.5f;
        final float panelY = (height - panelHeight) * 0.5f;
        final float slotSize = s(INVENTORY_SLOT_SIZE, scale);
        final float slotGap = s(INVENTORY_SLOT_GAP, scale);
        final float iconSize = s(30.0f, scale);
        final int columns = CREATIVE_COLUMNS;
        final int rows = CREATIVE_ROWS;
        final int pageSize = Math.max(1, rows * columns);
        final int pageCount = Math.max(1, (int) Math.ceil(entryCount / (double) pageSize));
        final int page = Math.max(0, Math.min(inventorySystem.page(), pageCount - 1));
        final float slotStartX = panelX + s(20.0f, scale);
        final float slotStartY = panelY + s(110.0f, scale);
        final boolean searchMode = inventorySystem.isSearchFocused();
        return new CreativeInventoryLayout(
                columns,
                panelX,
                panelY,
                panelWidth,
                panelHeight,
                slotStartX,
                slotStartY,
                slotSize,
                slotGap,
                iconSize,
                slotStartX,
                panelY + s(364.0f, scale),
                panelX + s(searchMode ? 170.0f : 34.0f, scale),
                panelY + s(78.0f, scale),
                panelX + s(20.0f, scale),
                panelY + s(68.0f, scale),
                s(searchMode ? 334.0f : 248.0f, scale),
                s(38.0f, scale),
                scale,
                rows,
                pageSize,
                page,
                pageCount
        );
    }

    private SurvivalInventoryLayout survivalInventoryLayout(final int width, final int height) {
        final float scale = inventoryScale(width, height, SURVIVAL_LAYOUT_WIDTH, SURVIVAL_LAYOUT_HEIGHT);
        final float panelWidth = s(SURVIVAL_LAYOUT_WIDTH, scale);
        final float panelHeight = s(SURVIVAL_LAYOUT_HEIGHT, scale);
        final float panelX = (width - panelWidth) * 0.5f;
        final float panelY = (height - panelHeight) * 0.5f;
        final float slotSize = s(INVENTORY_SLOT_SIZE, scale);
        final float slotGap = s(INVENTORY_SLOT_GAP, scale);
        final float iconSize = s(30.0f, scale);
        final float gridWidth = INVENTORY_COLUMNS * slotSize + (INVENTORY_COLUMNS - 1) * slotGap;
        final float storageStartX = panelX + (panelWidth - gridWidth) * 0.5f;
        return new SurvivalInventoryLayout(
                panelX,
                panelY,
                panelWidth,
                panelHeight,
                storageStartX,
                panelY + s(220.0f, scale),
                storageStartX,
                panelY + s(374.0f, scale),
                slotSize,
                slotGap,
                iconSize,
                scale
        );
    }

    private float centeredTextureX(final int width, final float textureSizeOnScreen, final float sourceTextureSize, final float visibleCenterX) {
        return width * 0.5f - textureSizeOnScreen * (visibleCenterX / sourceTextureSize);
    }

    private float centeredTextureY(final int height, final float textureSizeOnScreen, final float sourceTextureSize, final float visibleCenterY) {
        return height * 0.5f - textureSizeOnScreen * (visibleCenterY / sourceTextureSize);
    }

    private void renderCarriedItem(final double mouseX, final double mouseY, final float scale) {
        final InventoryStack stack = inventorySystem.carriedStack();
        if (stack == null) {
            return;
        }
        final float size = s(32.0f, scale);
        drawStackIcon(stack, (float) mouseX - size * 0.5f, (float) mouseY - size * 0.5f, size, font(11, scale));
    }

    private void renderInventoryTooltip(final int width, final int height, final double mouseX, final double mouseY, final float scale) {
        if (!inventorySystem.isOpen() || menuSystem.isOpen() || inventorySystem.carriedStack() != null) {
            return;
        }

        final GameObject hovered = hoveredInventoryItem(mouseX, mouseY, width, height);
        if (hovered == null) {
            return;
        }

        drawItemTooltip(localization.objectName(hovered), mouseX, mouseY, width, height, scale);
    }

    private GameObject hoveredInventoryItem(final double mouseX, final double mouseY, final int width, final int height) {
        if (GameConfig.isCreative()) {
            final GameObject creativeItem = pickInventoryItem(mouseX, mouseY, width, height);
            if (creativeItem != null) {
                return creativeItem;
            }
        } else {
            final int storageSlot = pickInventoryStorageSlot(mouseX, mouseY, width, height);
            if (storageSlot >= 0) {
                final InventoryStack stack = inventorySystem.inventorySlot(storageSlot);
                return stack == null ? null : stack.item();
            }
        }

        final int hotbarSlot = pickInventoryHotbarSlot(mouseX, mouseY, width, height);
        if (hotbarSlot < 0) {
            return null;
        }
        final InventoryStack stack = player.getHotbarStack(hotbarSlot);
        return stack == null ? null : stack.item();
    }

    private void drawItemTooltip(final String itemName, final double mouseX, final double mouseY, final int width, final int height, final float scale) {
        if (itemName == null || itemName.isBlank()) {
            return;
        }

        final int textSize = font(12, scale);
        final float paddingX = s(7.0f, scale);
        final float paddingY = s(5.0f, scale);
        final float textWidth = Math.max(textSize, itemName.codePointCount(0, itemName.length()) * textSize * 0.54f);
        final float boxWidth = textWidth + paddingX * 2.0f;
        final float boxHeight = textSize * 1.35f + paddingY * 2.0f;
        final float gap = s(12.0f, scale);
        float boxX = (float) mouseX + gap;
        float boxY = (float) mouseY + gap;

        if (boxX + boxWidth > width - s(4.0f, scale)) {
            boxX = (float) mouseX - boxWidth - gap;
        }
        if (boxY + boxHeight > height - s(4.0f, scale)) {
            boxY = (float) mouseY - boxHeight - gap;
        }
        boxX = clamp(boxX, s(4.0f, scale), Math.max(s(4.0f, scale), width - boxWidth - s(4.0f, scale)));
        boxY = clamp(boxY, s(4.0f, scale), Math.max(s(4.0f, scale), height - boxHeight - s(4.0f, scale)));

        drawColoredQuad(boxX - 1.0f, boxY - 1.0f, boxWidth + 2.0f, boxHeight + 2.0f, 0.18f, 0.13f, 0.25f, 0.92f);
        drawColoredQuad(boxX, boxY, boxWidth, boxHeight, 0.02f, 0.02f, 0.04f, 0.94f);
        drawColoredQuad(boxX + 1.0f, boxY + 1.0f, boxWidth - 2.0f, 1.0f, 0.36f, 0.28f, 0.50f, 0.78f);
        textRenderer.drawText(itemName, boxX + paddingX, boxY + paddingY, textSize, TEXT_COLOR);
    }

    private void renderChat(final int width, final int height, final float hotbarY, final float scale) {
        final List<ChatSystem.VisibleMessage> messages = chatSystem.visibleMessages();
        final float left = s(10.0f, scale);
        final float lineHeight = s(18.0f, scale);
        float y = hotbarY - s(70.0f, scale) - lineHeight * Math.max(0, messages.size() - 1);
        for (final ChatSystem.VisibleMessage message : messages) {
            final float alpha = message.alpha();
            final String text = shortName(message.text(), Math.max(20, (int) (width / (s(8.0f, scale)))));
            drawColoredQuad(left - s(4.0f, scale), y - s(2.0f, scale), Math.min(width * 0.55f, text.length() * s(8.2f, scale)), lineHeight, 0.0f, 0.0f, 0.0f, 0.38f * alpha);
            textRenderer.drawText(text, left, y, font(12, scale), TEXT_COLOR, alpha);
            y += lineHeight;
        }

        if (!chatSystem.isOpen()) {
            return;
        }

        final float boxX = s(8.0f, scale);
        final float boxH = s(30.0f, scale);
        final float boxY = height - boxH - s(8.0f, scale);
        final float boxW = width - s(16.0f, scale);
        renderChatSuggestions(boxX, boxY, boxW, scale);
        drawColoredQuad(boxX, boxY, boxW, boxH, 0.0f, 0.0f, 0.0f, 0.72f);
        final String input = shortName(chatSystem.input(), Math.max(24, (int) (boxW / s(8.0f, scale))));
        textRenderer.drawText(input, boxX + s(8.0f, scale), boxY + s(8.0f, scale), font(13, scale), TEXT_COLOR);
    }

    private void renderChatSuggestions(final float boxX, final float boxY, final float boxW, final float scale) {
        final List<String> suggestions = chatSystem.suggestions();
        if (suggestions.isEmpty()) {
            return;
        }
        final float lineHeight = s(18.0f, scale);
        final int visibleCount = Math.min(5, suggestions.size());
        final float startY = boxY - s(6.0f, scale) - lineHeight * visibleCount;
        final float panelWidth = Math.min(boxW, s(360.0f, scale));
        drawColoredQuad(boxX, startY - s(3.0f, scale), panelWidth, lineHeight * visibleCount + s(6.0f, scale), 0.0f, 0.0f, 0.0f, 0.58f);
        for (int i = 0; i < visibleCount; i++) {
            final Color color = i == 0 ? TEXT_COLOR : MUTED_TEXT_COLOR;
            textRenderer.drawText(suggestions.get(i), boxX + s(8.0f, scale), startY + i * lineHeight, font(12, scale), color);
        }
    }

    private void renderMenu(final int width, final int height, final double mouseX, final double mouseY) {
        final float scale = guiScale(width, height);
        drawMenuBackdrop(width, height);

        if (menuSystem.isOptions()) {
            renderOptionsMenu(width, height, scale, mouseX, mouseY);
        } else {
            renderPauseMenu(width, height, scale, mouseX, mouseY);
        }
    }

    private void drawMenuBackdrop(final int width, final int height) {
        drawColoredQuad(0, 0, width, height, 0.08f, 0.095f, 0.16f, 0.58f);
        drawColoredQuad(0, 0, width, height, 0.02f, 0.015f, 0.04f, 0.22f);
    }

    private void renderPauseMenu(final int width, final int height, final float scale, final double mouseX, final double mouseY) {
        final MenuPanel panel = menuPanel(width, height, scale);
        final float centerX = panel.x() + panel.width() * 0.5f;

        drawMenuTitle(pauseMenuTitle(), centerX, s(108.0f, scale), font(22, scale), scale);

        for (final MenuButton button : menuButtonsForPause(panel.x(), panel.y(), panel.width(), scale)) {
            final boolean hovered = isMouseOver(button, mouseX, mouseY);
            final boolean accent = button.action() == MenuAction.OPTIONS && hovered;
            drawMenuButton(button, labelFor(button.action()), scale, hovered, accent, true);
        }

        drawInactivePauseButton(panel, scale, 1, 0, localized("Fortschritte", "Advancements"), mouseX, mouseY);
        drawInactivePauseButton(panel, scale, 1, 1, localized("Statistiken", "Statistics"), mouseX, mouseY);
        drawInactivePauseButton(panel, scale, 2, 0, localized("Feedback geben", "Give Feedback"), mouseX, mouseY);
        drawInactivePauseButton(panel, scale, 2, 1, localized("Fehler melden", "Report Bugs"), mouseX, mouseY);
        drawInactivePauseButton(panel, scale, 3, 1, localized("Im LAN \u00f6ffnen", "Open to LAN"), mouseX, mouseY);
    }

    private void renderOptionsMenu(final int width, final int height, final float scale, final double mouseX, final double mouseY) {
        final MenuPanel panel = menuPanel(width, height, scale);
        final float centerX = panel.x() + panel.width() * 0.5f;
        final MenuButton[] buttons = menuButtonsForOptions(panel.x(), panel.y(), panel.width(), scale);

        switch (menuSystem.screen()) {
            case OPTIONS -> renderOptionsRoot(panel, buttons, centerX, scale, mouseX, mouseY);
            case OPTIONS_MUSIC -> renderMusicOptions(panel, buttons, centerX, scale, mouseX, mouseY);
            case OPTIONS_GRAPHICS -> renderGraphicsOptions(panel, buttons, centerX, scale, mouseX, mouseY);
            case OPTIONS_CONTROLS -> renderControlsOptions(panel, buttons, centerX, scale, mouseX, mouseY);
            case OPTIONS_LANGUAGE -> renderLanguageOptions(panel, buttons, centerX, scale, mouseX, mouseY);
            default -> {
            }
        }
    }

    private void renderOptionsRoot(final MenuPanel panel, final MenuButton[] buttons, final float centerX, final float scale, final double mouseX, final double mouseY) {
        drawMenuTitle(optionsMenuTitle(), centerX, s(48.0f, scale), font(22, scale), scale);
        drawMenuSlider(
                buttonForAction(buttons, MenuAction.FIELD_OF_VIEW_DECREASE),
                buttonForAction(buttons, MenuAction.FIELD_OF_VIEW_INCREASE),
                localized("Sichtfeld", "FOV") + ": " + Math.round(player.getFieldOfView()),
                normalized(player.getFieldOfView(), 30.0f, 110.0f),
                scale,
                mouseX,
                mouseY
        );
        drawOptionsTopButton(panel, scale, 1, localized("Schwierigkeit: Normal", "Difficulty: Normal"), mouseX, mouseY);

        drawOptionsSubButton(panel, scale, 0, 0, MenuAction.NONE, localized("Skin-Anpassung ...", "Skin Customization ..."), buttons, mouseX, mouseY, true);
        drawOptionsSubButton(panel, scale, 0, 1, MenuAction.MUSIC_OPTIONS, localized("Musik & Ger\u00e4usche ...", "Music & Sounds ..."), buttons, mouseX, mouseY, true);
        drawOptionsSubButton(panel, scale, 1, 0, MenuAction.GRAPHICS_OPTIONS, localized("Grafikeinstellungen ...", "Video Settings ..."), buttons, mouseX, mouseY, true);
        drawOptionsSubButton(panel, scale, 1, 1, MenuAction.CONTROLS_OPTIONS, localized("Steuerung ...", "Controls ..."), buttons, mouseX, mouseY, true);
        drawOptionsSubButton(panel, scale, 2, 0, MenuAction.LANGUAGE_OPTIONS, localized("Sprache ...", "Language ..."), buttons, mouseX, mouseY, true);
        drawOptionsSubButton(panel, scale, 2, 1, MenuAction.NONE, localized("Chateinstellungen", "Chat Settings"), buttons, mouseX, mouseY, true);
        drawOptionsSubButton(panel, scale, 3, 0, MenuAction.NONE, localized("Ressourcenpakete ...", "Resource Packs ..."), buttons, mouseX, mouseY, true);
        drawOptionsSubButton(panel, scale, 3, 1, MenuAction.NONE, localized("Barrierefreiheitseinstellungen", "Accessibility Settings"), buttons, mouseX, mouseY, true);
        drawOptionsSubButton(panel, scale, 4, 0, MenuAction.NONE, localized("Telemetriedaten ...", "Telemetry Data ..."), buttons, mouseX, mouseY, true);
        drawOptionsSubButton(panel, scale, 4, 1, MenuAction.NONE, localized("Mitwirkende & Namensnennung ...", "Credits & Attribution ..."), buttons, mouseX, mouseY, true);

        drawMenuButton(buttonForAction(buttons, MenuAction.BACK), localized("Fertig", "Done"), scale,
                isMouseOver(buttonForAction(buttons, MenuAction.BACK), mouseX, mouseY), false, true);
    }

    private void renderMusicOptions(final MenuPanel panel, final MenuButton[] buttons, final float centerX, final float scale, final double mouseX, final double mouseY) {
        drawMenuTitle(localized("Musik & Ger\u00e4usche", "Music & Sounds"), centerX, s(48.0f, scale), font(22, scale), scale);
        drawMenuSlider(
                buttonForAction(buttons, MenuAction.MUSIC_VOLUME_DECREASE),
                buttonForAction(buttons, MenuAction.MUSIC_VOLUME_INCREASE),
                localization.translate("ui.options.music_volume") + ": " + percent(musicManager.getVolume()),
                musicManager.getVolume(),
                scale,
                mouseX,
                mouseY
        );
        drawMenuSlider(
                buttonForAction(buttons, MenuAction.EFFECTS_VOLUME_DECREASE),
                buttonForAction(buttons, MenuAction.EFFECTS_VOLUME_INCREASE),
                localization.translate("ui.options.effects_volume") + ": " + percent(soundEffectManager.getVolume()),
                soundEffectManager.getVolume(),
                scale,
                mouseX,
                mouseY
        );
        drawMenuButton(buttonForAction(buttons, MenuAction.BACK), localized("Fertig", "Done"), scale,
                isMouseOver(buttonForAction(buttons, MenuAction.BACK), mouseX, mouseY), false, true);
    }

    private void renderGraphicsOptions(final MenuPanel panel, final MenuButton[] buttons, final float centerX, final float scale, final double mouseX, final double mouseY) {
        drawMenuTitle(localized("Grafikeinstellungen", "Video Settings"), centerX, s(48.0f, scale), font(22, scale), scale);
        drawMenuSlider(
                buttonForAction(buttons, MenuAction.VIEW_DISTANCE_DECREASE),
                buttonForAction(buttons, MenuAction.VIEW_DISTANCE_INCREASE),
                localization.translate("ui.options.view_distance") + ": " + GameConfig.getViewDistanceChunks() + localized(" Chunks", " chunks"),
                normalized(GameConfig.getViewDistanceChunks(), GameConfig.MIN_VIEW_DISTANCE_CHUNKS, GameConfig.MAX_VIEW_DISTANCE_CHUNKS),
                scale,
                mouseX,
                mouseY
        );
        drawMenuSlider(
                buttonForAction(buttons, MenuAction.HUD_SCALE_DECREASE),
                buttonForAction(buttons, MenuAction.HUD_SCALE_INCREASE),
                localization.translate("ui.options.hud_scale") + ": " + hudScaleSetting + "x",
                normalized(hudScaleSetting, MIN_HUD_SCALE, MAX_HUD_SCALE),
                scale,
                mouseX,
                mouseY
        );
        drawMenuButton(buttonForAction(buttons, MenuAction.FULLSCREEN_TOGGLE),
                localization.translate("ui.options.fullscreen") + ": " + labelFor(MenuAction.FULLSCREEN_TOGGLE),
                scale,
                isMouseOver(buttonForAction(buttons, MenuAction.FULLSCREEN_TOGGLE), mouseX, mouseY),
                false,
                true);
        drawMenuStepper(
                buttonForAction(buttons, MenuAction.RESOLUTION_PREVIOUS),
                buttonForAction(buttons, MenuAction.RESOLUTION_NEXT),
                localization.translate("ui.options.resolution") + ": " + displayManager.currentResolutionLabel(),
                scale,
                mouseX,
                mouseY
        );
        drawMenuButton(buttonForAction(buttons, MenuAction.BACK), localized("Fertig", "Done"), scale,
                isMouseOver(buttonForAction(buttons, MenuAction.BACK), mouseX, mouseY), false, true);
    }

    private void renderControlsOptions(final MenuPanel panel, final MenuButton[] buttons, final float centerX, final float scale, final double mouseX, final double mouseY) {
        drawMenuTitle(localized("Steuerung", "Controls"), centerX, s(48.0f, scale), font(22, scale), scale);
        drawMenuSlider(
                buttonForAction(buttons, MenuAction.SENSITIVITY_DECREASE),
                buttonForAction(buttons, MenuAction.SENSITIVITY_INCREASE),
                localization.translate("ui.options.sensitivity") + ": " + String.format(Locale.ROOT, "%.2f", player.getMouseSensitivity()),
                normalized((float) player.getMouseSensitivity(), 0.02f, 0.30f),
                scale,
                mouseX,
                mouseY
        );
        drawMenuButton(buttonForAction(buttons, MenuAction.BACK), localized("Fertig", "Done"), scale,
                isMouseOver(buttonForAction(buttons, MenuAction.BACK), mouseX, mouseY), false, true);
    }

    private void renderLanguageOptions(final MenuPanel panel, final MenuButton[] buttons, final float centerX, final float scale, final double mouseX, final double mouseY) {
        drawMenuTitle(localized("Sprache", "Language"), centerX, s(48.0f, scale), font(22, scale), scale);
        drawMenuButton(buttonForAction(buttons, MenuAction.LANGUAGE_TOGGLE),
                localization.translate("ui.options.language") + ": " + localization.currentLanguageName(),
                scale,
                isMouseOver(buttonForAction(buttons, MenuAction.LANGUAGE_TOGGLE), mouseX, mouseY),
                false,
                true);
        drawMenuButton(buttonForAction(buttons, MenuAction.BACK), localized("Fertig", "Done"), scale,
                isMouseOver(buttonForAction(buttons, MenuAction.BACK), mouseX, mouseY), false, true);
    }

    private void drawInactivePauseButton(final MenuPanel panel, final float scale, final int row, final int column, final String label, final double mouseX, final double mouseY) {
        final float gap = s(28.0f, scale);
        final float rowGap = s(16.0f, scale);
        final float buttonHeight = s(44.0f, scale);
        final float columnWidth = (panel.width() - gap) * 0.5f;
        final float x = panel.x() + column * (columnWidth + gap);
        final float y = panel.y() + row * (buttonHeight + rowGap);
        final MenuButton button = new MenuButton(MenuAction.NONE, x, y, columnWidth, buttonHeight);
        drawMenuButton(button, label, scale, isMouseOver(button, mouseX, mouseY), false, false);
    }

    private void drawOptionsSubButton(
            final MenuPanel panel,
            final float scale,
            final int row,
            final int column,
            final MenuAction action,
            final String label,
            final MenuButton[] buttons,
            final double mouseX,
            final double mouseY,
            final boolean enabled
    ) {
        final MenuButton activeButton = buttonForAction(buttons, action);
        final MenuButton button = activeButton == null
                ? optionsSubButton(panel, scale, row, column, MenuAction.NONE)
                : activeButton;
        drawMenuButton(button, label, scale, isMouseOver(button, mouseX, mouseY), false, enabled);
    }

    private void drawOptionsTopButton(final MenuPanel panel, final float scale, final int column, final String label, final double mouseX, final double mouseY) {
        final float gap = s(32.0f, scale);
        final float buttonHeight = s(44.0f, scale);
        final float columnWidth = (panel.width() - gap) * 0.5f;
        final float x = panel.x() + column * (columnWidth + gap);
        final MenuButton button = new MenuButton(MenuAction.NONE, x, panel.y(), columnWidth, buttonHeight);
        drawMenuButton(button, label, scale, isMouseOver(button, mouseX, mouseY), false, true);
    }

    private void drawMenuButton(
            final MenuButton button,
            final String label,
            final float scale,
            final boolean hovered,
            final boolean accent,
            final boolean enabled
    ) {
        if (button == null) {
            return;
        }
        drawMenuButtonBackground(button.x(), button.y(), button.width(), button.height(), scale, hovered, accent, enabled);
        drawCenteredMenuText(fitMenuLabel(label, button.width(), scale), button.x() + button.width() * 0.5f,
                button.y() + button.height() * 0.23f, font(15, scale), scale, enabled ? TEXT_COLOR : MUTED_TEXT_COLOR);
    }

    private void drawMenuButtonBackground(
            final float x,
            final float y,
            final float width,
            final float height,
            final float scale,
            final boolean hovered,
            final boolean accent,
            final boolean enabled
    ) {
        drawColoredQuad(x, y, width, height, 0.035f, 0.016f, 0.065f, 0.98f);
        drawColoredQuad(x + s(3.0f, scale), y + s(3.0f, scale),
                width - s(6.0f, scale), height - s(6.0f, scale), 0.08f, 0.10f, 0.22f, 1.0f);

        if (!enabled) {
            drawColoredQuad(x + s(7.0f, scale), y + s(7.0f, scale),
                    width - s(14.0f, scale), height - s(14.0f, scale), 0.15f, 0.13f, 0.19f, 0.95f);
            return;
        }

        if (accent) {
            drawColoredQuad(x + s(3.0f, scale), y + s(3.0f, scale),
                    width - s(6.0f, scale), height - s(6.0f, scale), 0.95f, 0.36f, 0.0f, 1.0f);
            final float insetX = Math.min(s(11.0f, scale), width * 0.35f);
            final float insetY = Math.min(s(10.0f, scale), height * 0.35f);
            drawColoredQuad(x + insetX, y + insetY,
                    Math.max(1.0f, width - insetX * 2.0f), Math.max(1.0f, height - insetY * 2.0f), 1.0f, 0.73f, 0.03f, 1.0f);
            return;
        }

        final float outerR = hovered ? 0.02f : 0.018f;
        final float outerG = hovered ? 0.60f : 0.46f;
        final float outerB = hovered ? 0.98f : 0.78f;
        final float innerR = hovered ? 0.12f : 0.05f;
        final float innerG = hovered ? 0.82f : 0.76f;
        final float innerB = hovered ? 1.0f : 0.91f;
        drawColoredQuad(x + s(3.0f, scale), y + s(3.0f, scale),
                width - s(6.0f, scale), height - s(6.0f, scale), outerR, outerG, outerB, 1.0f);
        final float insetX = Math.min(s(11.0f, scale), width * 0.35f);
        final float insetY = Math.min(s(10.0f, scale), height * 0.35f);
        drawColoredQuad(x + insetX, y + insetY,
                Math.max(1.0f, width - insetX * 2.0f), Math.max(1.0f, height - insetY * 2.0f), innerR, innerG, innerB, 1.0f);
    }

    private void drawMenuSlider(
            final MenuButton lowerButton,
            final MenuButton upperButton,
            final String label,
            final float normalizedValue,
            final float scale,
            final double mouseX,
            final double mouseY
    ) {
        if (lowerButton == null || upperButton == null) {
            return;
        }
        final float x = Math.min(lowerButton.x(), upperButton.x());
        final float y = Math.min(lowerButton.y(), upperButton.y());
        final float width = Math.max(lowerButton.x() + lowerButton.width(), upperButton.x() + upperButton.width()) - x;
        final float height = Math.max(lowerButton.y() + lowerButton.height(), upperButton.y() + upperButton.height()) - y;
        final boolean hovered = isMouseOver(lowerButton, mouseX, mouseY) || isMouseOver(upperButton, mouseX, mouseY);

        drawColoredQuad(x, y, width, height, 0.035f, 0.016f, 0.065f, 0.98f);
        drawColoredQuad(x + s(3.0f, scale), y + s(3.0f, scale),
                width - s(6.0f, scale), height - s(6.0f, scale), 0.15f, 0.12f, 0.18f, 0.98f);
        drawColoredQuad(x + s(7.0f, scale), y + s(7.0f, scale),
                width - s(14.0f, scale), height - s(14.0f, scale), 0.10f, 0.09f, 0.13f, 0.92f);

        final float knobWidth = s(18.0f, scale);
        final float knobX = x + s(5.0f, scale) + clamp(normalizedValue, 0.0f, 1.0f) * (width - s(10.0f, scale) - knobWidth);
        final MenuButton knob = new MenuButton(MenuAction.NONE, knobX, y, knobWidth, height);
        drawMenuButtonBackground(knob.x(), knob.y(), knob.width(), knob.height(), scale, hovered, false, true);

        drawCenteredMenuText(fitMenuLabel(label, width, scale), x + width * 0.5f, y + height * 0.23f, font(15, scale), scale, TEXT_COLOR);
    }

    private void drawMenuStepper(
            final MenuButton previousButton,
            final MenuButton nextButton,
            final String label,
            final float scale,
            final double mouseX,
            final double mouseY
    ) {
        if (previousButton == null || nextButton == null) {
            return;
        }
        final float x = Math.min(previousButton.x(), nextButton.x());
        final float y = Math.min(previousButton.y(), nextButton.y());
        final float width = Math.max(previousButton.x() + previousButton.width(), nextButton.x() + nextButton.width()) - x;
        final float height = Math.max(previousButton.y() + previousButton.height(), nextButton.y() + nextButton.height()) - y;
        drawMenuButtonBackground(x, y, width, height, scale, false, false, false);
        drawMenuButton(previousButton, "<", scale, isMouseOver(previousButton, mouseX, mouseY), false, true);
        drawMenuButton(nextButton, ">", scale, isMouseOver(nextButton, mouseX, mouseY), false, true);
        drawCenteredMenuText(fitMenuLabel(label, width - previousButton.width() - nextButton.width(), scale),
                x + width * 0.5f, y + height * 0.23f, font(15, scale), scale, TEXT_COLOR);
    }

    private void drawMenuTitle(final String title, final float centerX, final float y, final int size, final float scale) {
        drawCenteredMenuText(title, centerX, y, size, scale, TEXT_COLOR);
    }

    private void drawCenteredMenuText(final String text, final float centerX, final float y, final int size, final float scale, final Color color) {
        textRenderer.drawCenteredBoldText(text, centerX + s(2.0f, scale), y + s(2.0f, scale), size, new Color(20, 18, 28, 230));
        textRenderer.drawCenteredBoldText(text, centerX, y, size, color);
    }

    private String labelFor(final MenuAction action) {
        return switch (action) {
            case RESUME -> localized("Zur\u00fcck zum Spiel", "Back to Game");
            case OPTIONS -> localized("Optionen ...", "Options ...");
            case MUSIC_OPTIONS -> localized("Musik & Ger\u00e4usche ...", "Music & Sounds ...");
            case GRAPHICS_OPTIONS -> localized("Grafikeinstellungen ...", "Video Settings ...");
            case CONTROLS_OPTIONS -> localized("Steuerung ...", "Controls ...");
            case LANGUAGE_OPTIONS -> localized("Sprache ...", "Language ...");
            case EXIT -> localized("Speichern und beenden", "Save and Quit");
            case BACK -> localized("Fertig", "Done");
            case SENSITIVITY_DECREASE -> "-";
            case SENSITIVITY_INCREASE -> "+";
            case HUD_SCALE_DECREASE -> "-";
            case HUD_SCALE_INCREASE -> "+";
            case VIEW_DISTANCE_DECREASE -> "-";
            case VIEW_DISTANCE_INCREASE -> "+";
            case MUSIC_VOLUME_DECREASE -> "-";
            case MUSIC_VOLUME_INCREASE -> "+";
            case EFFECTS_VOLUME_DECREASE -> "-";
            case EFFECTS_VOLUME_INCREASE -> "+";
            case FIELD_OF_VIEW_DECREASE -> "-";
            case FIELD_OF_VIEW_INCREASE -> "+";
            case FULLSCREEN_TOGGLE -> displayManager.isFullscreen()
                    ? localization.translate("ui.options.fullscreen.on")
                    : localization.translate("ui.options.fullscreen.off");
            case RESOLUTION_PREVIOUS -> "<";
            case RESOLUTION_NEXT -> ">";
            case LANGUAGE_TOGGLE -> localization.currentLanguageName();
            case NONE -> "";
        };
    }

    private MenuButton[] menuButtons(final int width, final int height) {
        final float scale = guiScale(width, height);
        final MenuPanel panel = menuPanel(width, height, scale);
        return menuSystem.isOptions()
                ? menuButtonsForOptions(panel.x(), panel.y(), panel.width(), scale)
                : menuButtonsForPause(panel.x(), panel.y(), panel.width(), scale);
    }

    private MenuButton[] menuButtonsForPause(final float panelX, final float panelY, final float panelWidth, final float scale) {
        final float gap = s(28.0f, scale);
        final float buttonHeight = s(44.0f, scale);
        final float rowStep = buttonHeight + s(16.0f, scale);
        final float columnWidth = (panelWidth - gap) * 0.5f;
        return new MenuButton[]{
                new MenuButton(MenuAction.RESUME, panelX, panelY, panelWidth, buttonHeight),
                new MenuButton(MenuAction.OPTIONS, panelX, panelY + rowStep * 3.0f, columnWidth, buttonHeight),
                new MenuButton(MenuAction.EXIT, panelX, panelY + rowStep * 4.0f, panelWidth, buttonHeight)
        };
    }

    private MenuButton[] menuButtonsForOptions(final float panelX, final float panelY, final float panelWidth, final float scale) {
        return switch (menuSystem.screen()) {
            case OPTIONS -> menuButtonsForOptionsRoot(panelX, panelY, panelWidth, scale);
            case OPTIONS_MUSIC -> menuButtonsForMusicOptions(panelX, panelY, panelWidth, scale);
            case OPTIONS_GRAPHICS -> menuButtonsForGraphicsOptions(panelX, panelY, panelWidth, scale);
            case OPTIONS_CONTROLS -> menuButtonsForControlsOptions(panelX, panelY, panelWidth, scale);
            case OPTIONS_LANGUAGE -> menuButtonsForLanguageOptions(panelX, panelY, panelWidth, scale);
            default -> new MenuButton[0];
        };
    }

    private MenuButton[] menuButtonsForOptionsRoot(final float panelX, final float panelY, final float panelWidth, final float scale) {
        final float centerX = panelX + panelWidth * 0.5f;
        final float gap = s(32.0f, scale);
        final float buttonHeight = s(44.0f, scale);
        final float rowStep = buttonHeight + s(20.0f, scale);
        final float columnWidth = (panelWidth - gap) * 0.5f;
        final float halfColumnWidth = columnWidth * 0.5f;
        final float leftX = panelX;
        final float rightX = panelX + columnWidth + gap;
        final float subOptionsTop = panelY + s(144.0f, scale);
        final float backWidth = Math.min(s(528.0f, scale), panelWidth * 0.68f);
        final float backX = centerX - backWidth * 0.5f;
        return new MenuButton[]{
                new MenuButton(MenuAction.FIELD_OF_VIEW_DECREASE, leftX, panelY, halfColumnWidth, buttonHeight),
                new MenuButton(MenuAction.FIELD_OF_VIEW_INCREASE, leftX + halfColumnWidth, panelY, halfColumnWidth, buttonHeight),
                optionsSubButton(panelX, subOptionsTop, columnWidth, gap, rowStep, buttonHeight, 0, 1, MenuAction.MUSIC_OPTIONS),
                optionsSubButton(panelX, subOptionsTop, columnWidth, gap, rowStep, buttonHeight, 1, 0, MenuAction.GRAPHICS_OPTIONS),
                optionsSubButton(panelX, subOptionsTop, columnWidth, gap, rowStep, buttonHeight, 1, 1, MenuAction.CONTROLS_OPTIONS),
                optionsSubButton(panelX, subOptionsTop, columnWidth, gap, rowStep, buttonHeight, 2, 0, MenuAction.LANGUAGE_OPTIONS),
                new MenuButton(MenuAction.BACK, backX, subOptionsTop + rowStep * 5.0f + s(16.0f, scale), backWidth, buttonHeight)
        };
    }

    private MenuButton[] menuButtonsForMusicOptions(final float panelX, final float panelY, final float panelWidth, final float scale) {
        final float centerX = panelX + panelWidth * 0.5f;
        final float gap = s(32.0f, scale);
        final float buttonHeight = s(44.0f, scale);
        final float rowStep = buttonHeight + s(20.0f, scale);
        final float columnWidth = (panelWidth - gap) * 0.5f;
        final float halfColumnWidth = columnWidth * 0.5f;
        final float leftX = panelX;
        final float rightX = panelX + columnWidth + gap;
        final float backWidth = Math.min(s(528.0f, scale), panelWidth * 0.68f);
        final float backX = centerX - backWidth * 0.5f;
        return new MenuButton[]{
                new MenuButton(MenuAction.MUSIC_VOLUME_DECREASE, leftX, panelY, halfColumnWidth, buttonHeight),
                new MenuButton(MenuAction.MUSIC_VOLUME_INCREASE, leftX + halfColumnWidth, panelY, halfColumnWidth, buttonHeight),
                new MenuButton(MenuAction.EFFECTS_VOLUME_DECREASE, rightX, panelY, halfColumnWidth, buttonHeight),
                new MenuButton(MenuAction.EFFECTS_VOLUME_INCREASE, rightX + halfColumnWidth, panelY, halfColumnWidth, buttonHeight),
                new MenuButton(MenuAction.BACK, backX, panelY + rowStep + s(24.0f, scale), backWidth, buttonHeight)
        };
    }

    private MenuButton[] menuButtonsForGraphicsOptions(final float panelX, final float panelY, final float panelWidth, final float scale) {
        final float centerX = panelX + panelWidth * 0.5f;
        final float gap = s(32.0f, scale);
        final float buttonHeight = s(44.0f, scale);
        final float rowStep = buttonHeight + s(20.0f, scale);
        final float columnWidth = (panelWidth - gap) * 0.5f;
        final float halfColumnWidth = columnWidth * 0.5f;
        final float leftX = panelX;
        final float rightX = panelX + columnWidth + gap;
        final float backWidth = Math.min(s(528.0f, scale), panelWidth * 0.68f);
        final float backX = centerX - backWidth * 0.5f;
        return new MenuButton[]{
                new MenuButton(MenuAction.VIEW_DISTANCE_DECREASE, leftX, panelY, halfColumnWidth, buttonHeight),
                new MenuButton(MenuAction.VIEW_DISTANCE_INCREASE, leftX + halfColumnWidth, panelY, halfColumnWidth, buttonHeight),
                new MenuButton(MenuAction.HUD_SCALE_DECREASE, rightX, panelY, halfColumnWidth, buttonHeight),
                new MenuButton(MenuAction.HUD_SCALE_INCREASE, rightX + halfColumnWidth, panelY, halfColumnWidth, buttonHeight),
                new MenuButton(MenuAction.FULLSCREEN_TOGGLE, leftX, panelY + rowStep, columnWidth, buttonHeight),
                new MenuButton(MenuAction.RESOLUTION_PREVIOUS, rightX, panelY + rowStep, s(54.0f, scale), buttonHeight),
                new MenuButton(MenuAction.RESOLUTION_NEXT, rightX + columnWidth - s(54.0f, scale), panelY + rowStep, s(54.0f, scale), buttonHeight),
                new MenuButton(MenuAction.BACK, backX, panelY + rowStep * 2.0f + s(24.0f, scale), backWidth, buttonHeight)
        };
    }

    private MenuButton[] menuButtonsForControlsOptions(final float panelX, final float panelY, final float panelWidth, final float scale) {
        final float centerX = panelX + panelWidth * 0.5f;
        final float buttonHeight = s(44.0f, scale);
        final float halfWidth = panelWidth * 0.5f;
        final float backWidth = Math.min(s(528.0f, scale), panelWidth * 0.68f);
        final float backX = centerX - backWidth * 0.5f;
        return new MenuButton[]{
                new MenuButton(MenuAction.SENSITIVITY_DECREASE, panelX, panelY, halfWidth, buttonHeight),
                new MenuButton(MenuAction.SENSITIVITY_INCREASE, panelX + halfWidth, panelY, halfWidth, buttonHeight),
                new MenuButton(MenuAction.BACK, backX, panelY + s(84.0f, scale), backWidth, buttonHeight)
        };
    }

    private MenuButton[] menuButtonsForLanguageOptions(final float panelX, final float panelY, final float panelWidth, final float scale) {
        final float centerX = panelX + panelWidth * 0.5f;
        final float buttonHeight = s(44.0f, scale);
        final float buttonWidth = Math.min(s(420.0f, scale), panelWidth);
        final float buttonX = centerX - buttonWidth * 0.5f;
        final float backWidth = Math.min(s(528.0f, scale), panelWidth * 0.68f);
        final float backX = centerX - backWidth * 0.5f;
        return new MenuButton[]{
                new MenuButton(MenuAction.LANGUAGE_TOGGLE, buttonX, panelY, buttonWidth, buttonHeight),
                new MenuButton(MenuAction.BACK, backX, panelY + s(84.0f, scale), backWidth, buttonHeight)
        };
    }

    private MenuButton optionsSubButton(final MenuPanel panel, final float scale, final int row, final int column, final MenuAction action) {
        final float gap = s(32.0f, scale);
        final float buttonHeight = s(44.0f, scale);
        final float rowStep = buttonHeight + s(20.0f, scale);
        final float columnWidth = (panel.width() - gap) * 0.5f;
        final float top = panel.y() + s(144.0f, scale);
        return optionsSubButton(panel.x(), top, columnWidth, gap, rowStep, buttonHeight, row, column, action);
    }

    private MenuButton optionsSubButton(
            final float panelX,
            final float top,
            final float columnWidth,
            final float columnGap,
            final float rowStep,
            final float buttonHeight,
            final int row,
            final int column,
            final MenuAction action
    ) {
        final float x = panelX + column * (columnWidth + columnGap);
        final float y = top + rowStep * row;
        return new MenuButton(action, x, y, columnWidth, buttonHeight);
    }

    private MenuPanel menuPanel(final int width, final int height, final float scale) {
        final float margin = s(48.0f, scale);
        final float requestedWidth = s(menuSystem.isOptions() ? 820.0f : 540.0f, scale);
        final float minWidth = s(menuSystem.isOptions() ? 560.0f : 360.0f, scale);
        final float panelWidth = Math.max(Math.min(minWidth, width - margin), Math.min(requestedWidth, width - margin));
        final float panelHeight = menuPanelHeight(scale);
        final float panelX = (width - panelWidth) * 0.5f;
        final float preferredY = menuSystem.isOptions()
                ? Math.max(s(160.0f, scale), height * 0.15f)
                : Math.max(s(310.0f, scale), height * 0.27f);
        final float minY = s(menuSystem.isOptions() ? 72.0f : 160.0f, scale);
        final float maxY = height - panelHeight - s(24.0f, scale);
        final float panelY = Math.min(preferredY, Math.max(minY, maxY));
        return new MenuPanel(panelX, panelY, panelWidth, panelHeight);
    }

    private float menuPanelHeight(final float scale) {
        if (!menuSystem.isOptions()) {
            return s(284.0f, scale);
        }
        return switch (menuSystem.screen()) {
            case OPTIONS -> s(540.0f, scale);
            case OPTIONS_GRAPHICS -> s(248.0f, scale);
            case OPTIONS_MUSIC, OPTIONS_CONTROLS, OPTIONS_LANGUAGE -> s(176.0f, scale);
            default -> s(284.0f, scale);
        };
    }

    private MenuButton buttonForAction(final MenuButton[] buttons, final MenuAction action) {
        for (final MenuButton button : buttons) {
            if (button.action() == action) {
                return button;
            }
        }
        return null;
    }

    private float clickedSliderValue(
            final MenuButton[] buttons,
            final MenuAction lowerAction,
            final MenuAction upperAction,
            final double mouseX,
            final double mouseY
    ) {
        final MenuButton lowerButton = buttonForAction(buttons, lowerAction);
        final MenuButton upperButton = buttonForAction(buttons, upperAction);
        if (lowerButton == null || upperButton == null) {
            return Float.NaN;
        }

        final float x = Math.min(lowerButton.x(), upperButton.x());
        final float y = Math.min(lowerButton.y(), upperButton.y());
        final float width = Math.max(lowerButton.x() + lowerButton.width(), upperButton.x() + upperButton.width()) - x;
        final float height = Math.max(lowerButton.y() + lowerButton.height(), upperButton.y() + upperButton.height()) - y;
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) {
            return Float.NaN;
        }
        return clamp((float) ((mouseX - x) / Math.max(1.0f, width)), 0.0f, 1.0f);
    }

    private boolean isMouseOver(final MenuButton button, final double mouseX, final double mouseY) {
        return button != null
                && mouseX >= button.x()
                && mouseX <= button.x() + button.width()
                && mouseY >= button.y()
                && mouseY <= button.y() + button.height();
    }

    private String fitMenuLabel(final String label, final float width, final float scale) {
        final int maxCharacters = Math.max(4, (int) (width / Math.max(1.0f, s(8.4f, scale))));
        return shortName(label, maxCharacters);
    }

    private String pauseMenuTitle() {
        return localized("Spielmen\u00fc", "Game Menu");
    }

    private String optionsMenuTitle() {
        return localized("Optionen", "Options");
    }

    private String localized(final String german, final String english) {
        return "de_de".equals(localization.currentLanguage().code()) ? german : english;
    }

    private float normalized(final float value, final float min, final float max) {
        if (max <= min) {
            return 0.0f;
        }
        return clamp((value - min) / (max - min), 0.0f, 1.0f);
    }

    private float normalizedResolution() {
        return switch (displayManager.currentResolutionLabel()) {
            case "1080p" -> 0.0f;
            case "1440p" -> 0.5f;
            case "4K" -> 1.0f;
            default -> 0.5f;
        };
    }

    private String percent(final float value) {
        return Math.round(value * 100.0f) + "%";
    }

    private boolean sameSelectedItem(final ResourceId currentItemId) {
        return currentItemId == null ? selectedLabelItemId == null : currentItemId.equals(selectedLabelItemId);
    }

    private float selectedItemLabelAlpha() {
        if (selectedLabelSeconds <= 0.0) {
            return 0.0f;
        }
        if (selectedLabelSeconds >= SELECTED_ITEM_LABEL_FADE_SECONDS) {
            return 1.0f;
        }
        return clamp((float) (selectedLabelSeconds / SELECTED_ITEM_LABEL_FADE_SECONDS), 0.0f, 1.0f);
    }

    private float hudScale(final int width, final int height) {
        return hudScaleSetting * uiScale(width, height);
    }

    private float hudTextScale(final float scale) {
        return Math.max(0.6f, scale * hudScaleSetting / (float) DEFAULT_HUD_SCALE);
    }

    private float guiScale(final int width, final int height) {
        final float desired = uiScale(width, height) * hudScaleSetting / (float) DEFAULT_HUD_SCALE;
        final float fitToScreen = Math.min(width / 1180.0f, height / 720.0f);
        final float maxScale = Math.min(2.0f, Math.max(1.0f, fitToScreen));
        return clamp(desired, 0.75f, maxScale);
    }

    private float inventoryScale(final int width, final int height, final float layoutWidth, final float layoutHeight) {
        final float margin = 64.0f;
        final float fit = Math.min((width - margin) / layoutWidth, (height - margin) / layoutHeight);
        return clamp(Math.min(guiScale(width, height), fit), 0.65f, 2.0f);
    }

    private float uiScale(final int width, final int height) {
        final float scaleByWidth = width / 1920.0f;
        final float scaleByHeight = height / 1080.0f;
        return clamp(Math.min(scaleByWidth, scaleByHeight), MIN_UI_SCALE, MAX_UI_SCALE);
    }

    private float s(final float value, final float scale) {
        return value * scale;
    }

    private int font(final int baseSize, final float scale) {
        return Math.max(8, Math.round(baseSize * scale));
    }

    private float clamp(final float value, final float min, final float max) {
        return Math.max(min, Math.min(max, value));
    }

    private void drawStatsRow(final int value, final float startX, final float y, final float step, final float size, final int empty, final int half, final int full) {
        for (int i = 0; i < STAT_PIP_COUNT; i++) {
            final int required = (i + 1) * 2;
            final int texture = value >= required ? full : (value == required - 1 ? half : empty);
            drawTexturedQuad(texture, startX + i * step, y, size, size);
        }
    }

    private void drawTexturedQuad(final int textureId, final float x, final float y, final float width, final float height) {
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        glBindTexture(GL_TEXTURE_2D, textureId);
        glBegin(GL_QUADS);
        glTexCoord2f(0.0f, 1.0f);
        glVertex2f(x, y);
        glTexCoord2f(1.0f, 1.0f);
        glVertex2f(x + width, y);
        glTexCoord2f(1.0f, 0.0f);
        glVertex2f(x + width, y + height);
        glTexCoord2f(0.0f, 0.0f);
        glVertex2f(x, y + height);
        glEnd();
    }

    private void drawGuiPatch(final GuiTexture patch, final float x, final float y, final float width, final float height) {
        if (patch == null || patch.textureId() == 0) {
            drawColoredQuad(x, y, width, height, 0.12f, 0.09f, 0.16f, 0.92f);
            return;
        }
        drawTexturedQuad(patch.textureId(), x, y, width, height);
    }

    private void drawNineSlicePatch(final GuiTexture patch, final float x, final float y, final float width, final float height, final float border) {
        if (patch == null || patch.textureId() == 0 || width <= 0.0f || height <= 0.0f) {
            drawColoredQuad(x, y, width, height, 0.12f, 0.09f, 0.16f, 0.92f);
            return;
        }

        final float sourceBorder = Math.max(1.0f, Math.min(Math.min(patch.width(), patch.height()) * 0.45f, border));
        final float destBorder = Math.max(1.0f, Math.min(Math.min(width, height) * 0.45f, border));
        final float centerSourceW = Math.max(1.0f, patch.width() - sourceBorder * 2.0f);
        final float centerSourceH = Math.max(1.0f, patch.height() - sourceBorder * 2.0f);
        final float centerDestW = Math.max(1.0f, width - destBorder * 2.0f);
        final float centerDestH = Math.max(1.0f, height - destBorder * 2.0f);

        drawGuiPatchRegion(patch, 0, 0, sourceBorder, sourceBorder, x, y, destBorder, destBorder);
        drawGuiPatchRegion(patch, sourceBorder, 0, centerSourceW, sourceBorder, x + destBorder, y, centerDestW, destBorder);
        drawGuiPatchRegion(patch, patch.width() - sourceBorder, 0, sourceBorder, sourceBorder, x + width - destBorder, y, destBorder, destBorder);

        drawGuiPatchRegion(patch, 0, sourceBorder, sourceBorder, centerSourceH, x, y + destBorder, destBorder, centerDestH);
        drawGuiPatchRegion(patch, sourceBorder, sourceBorder, centerSourceW, centerSourceH, x + destBorder, y + destBorder, centerDestW, centerDestH);
        drawGuiPatchRegion(patch, patch.width() - sourceBorder, sourceBorder, sourceBorder, centerSourceH, x + width - destBorder, y + destBorder, destBorder, centerDestH);

        drawGuiPatchRegion(patch, 0, patch.height() - sourceBorder, sourceBorder, sourceBorder, x, y + height - destBorder, destBorder, destBorder);
        drawGuiPatchRegion(patch, sourceBorder, patch.height() - sourceBorder, centerSourceW, sourceBorder, x + destBorder, y + height - destBorder, centerDestW, destBorder);
        drawGuiPatchRegion(patch, patch.width() - sourceBorder, patch.height() - sourceBorder, sourceBorder, sourceBorder, x + width - destBorder, y + height - destBorder, destBorder, destBorder);
    }

    private void drawGuiPatchRegion(
            final GuiTexture patch,
            final float sourceX,
            final float sourceY,
            final float sourceWidth,
            final float sourceHeight,
            final float x,
            final float y,
            final float width,
            final float height
    ) {
        final float u1 = sourceX / patch.width();
        final float u2 = (sourceX + sourceWidth) / patch.width();
        final float vTop = 1.0f - sourceY / patch.height();
        final float vBottom = 1.0f - (sourceY + sourceHeight) / patch.height();

        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        glBindTexture(GL_TEXTURE_2D, patch.textureId());
        glBegin(GL_QUADS);
        glTexCoord2f(u1, vTop);
        glVertex2f(x, y);
        glTexCoord2f(u2, vTop);
        glVertex2f(x + width, y);
        glTexCoord2f(u2, vBottom);
        glVertex2f(x + width, y + height);
        glTexCoord2f(u1, vBottom);
        glVertex2f(x, y + height);
        glEnd();
    }

    private void drawTiledTexture(final int textureId, final float x, final float y, final float width, final float height, final float tileSize, final float alpha) {
        final float safeTile = Math.max(1.0f, tileSize);
        final float u2 = width / safeTile;
        final float v2 = height / safeTile;
        glColor4f(1.0f, 1.0f, 1.0f, clamp(alpha, 0.0f, 1.0f));
        glBindTexture(GL_TEXTURE_2D, textureId);
        glBegin(GL_QUADS);
        glTexCoord2f(0.0f, 0.0f);
        glVertex2f(x, y);
        glTexCoord2f(u2, 0.0f);
        glVertex2f(x + width, y);
        glTexCoord2f(u2, v2);
        glVertex2f(x + width, y + height);
        glTexCoord2f(0.0f, v2);
        glVertex2f(x, y + height);
        glEnd();
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void drawColoredQuad(final float x, final float y, final float width, final float height, final float r, final float g, final float b, final float a) {
        glDisable(GL_TEXTURE_2D);
        glColor4f(r, g, b, a);
        glBegin(GL_QUADS);
        glVertex2f(x, y);
        glVertex2f(x + width, y);
        glVertex2f(x + width, y + height);
        glVertex2f(x, y + height);
        glEnd();
        glEnable(GL_TEXTURE_2D);
    }

    private void loadHudTextures() {
        final BufferedImage widgetsAtlas = decodeImage(resourcePackLoader.loadGuiTexture("widgets"));
        final BufferedImage iconsAtlas = decodeImage(resourcePackLoader.loadGuiTexture("icons"));
        final BufferedImage inventoryAtlas = decodeImage(resourcePackLoader.loadGuiTexture("container/inventory"));
        final BufferedImage creativeItemsAtlas = decodeImage(resourcePackLoader.loadGuiTexture("container/creative_inventory/tab_items"));
        final BufferedImage creativeSearchAtlas = decodeImage(resourcePackLoader.loadGuiTexture("container/creative_inventory/tab_item_search"));
        final BufferedImage creativeTabsAtlas = decodeImage(resourcePackLoader.loadGuiTexture("container/creative_inventory/tabs"));
        final BufferedImage recipeButtonAtlas = decodeImage(resourcePackLoader.loadGuiTexture("recipe_button"));

        hotbarBackgroundTexture = loadGuiSprite(
                widgetsAtlas, HOTBAR_U, HOTBAR_V, HOTBAR_W, HOTBAR_H,
                new Color(40, 40, 52, 210)
        );
        hotbarSelectorTexture = loadGuiSprite(
                widgetsAtlas, SLOT_SELECTOR_U, SLOT_SELECTOR_V, SLOT_SELECTOR_W, SLOT_SELECTOR_H,
                new Color(255, 193, 77, 230)
        );

        heartEmptyTexture = loadGuiSprite(iconsAtlas, HEART_EMPTY_U, HEART_V, ICON_W, ICON_H, new Color(48, 28, 28, 210));
        heartHalfTexture = loadGuiSprite(iconsAtlas, HEART_HALF_U, HEART_V, ICON_W, ICON_H, new Color(206, 52, 52, 240));
        heartFullTexture = loadGuiSprite(iconsAtlas, HEART_FULL_U, HEART_V, ICON_W, ICON_H, new Color(235, 59, 59, 245));

        hungerEmptyTexture = loadGuiSprite(iconsAtlas, HUNGER_EMPTY_U, HUNGER_V, ICON_W, ICON_H, new Color(52, 38, 20, 210));
        hungerHalfTexture = loadGuiSprite(iconsAtlas, HUNGER_HALF_U, HUNGER_V, ICON_W, ICON_H, new Color(230, 148, 52, 240));
        hungerFullTexture = loadGuiSprite(iconsAtlas, HUNGER_FULL_U, HUNGER_V, ICON_W, ICON_H, new Color(245, 163, 61, 245));

        inventoryPanelTexture = uploadTexture(inventoryAtlas == null ? createSolidImage(16, 16, new Color(68, 70, 78, 245)) : inventoryAtlas);
        creativeInventoryItemsTexture = uploadTexture(creativeItemsAtlas == null ? createSolidImage(16, 16, new Color(68, 70, 78, 245)) : creativeItemsAtlas);
        creativeInventorySearchTexture = uploadTexture(creativeSearchAtlas == null ? createSolidImage(16, 16, new Color(68, 70, 78, 245)) : creativeSearchAtlas);
        optionsBackgroundTexture = loadGuiTexture("options_background", new Color(42, 42, 48, 255));
        menuButtonTexture = loadGuiSprite(widgetsAtlas, 0, 66, 200, 20, new Color(82, 86, 96, 255));

        survivalPlayerPanelPatch = loadGuiPatch(inventoryAtlas, 4, 4, 188, 160, new Color(16, 66, 120, 245));
        survivalCraftingPanelPatch = loadGuiPatch(inventoryAtlas, 188, 4, 162, 110, new Color(220, 74, 4, 245));
        inventorySlotPatch = loadGuiPatch(inventoryAtlas, 16, 168, 34, 34, new Color(40, 32, 48, 245));
        craftingSlotPatch = loadGuiPatch(inventoryAtlas, 196, 36, 34, 34, new Color(102, 24, 16, 245));
        selectedSlotPatch = loadGuiPatch(inventoryAtlas, 0, 332, 48, 48, new Color(255, 166, 0, 245));
        hotbarFramePatch = loadGuiPatch(creativeItemsAtlas, 8, 216, 334, 44, new Color(0, 121, 191, 245));
        creativeHeaderPatch = loadGuiPatch(creativeItemsAtlas, 8, 8, 248, 30, new Color(20, 194, 230, 245));
        creativeSearchHeaderPatch = loadGuiPatch(creativeSearchAtlas, 8, 8, 334, 30, new Color(20, 194, 230, 245));
        searchFieldPatch = loadGuiPatch(creativeSearchAtlas, 148, 10, 190, 24, new Color(42, 35, 52, 245));
        tabNormalPatch = loadGuiPatch(creativeTabsAtlas, 4, 0, 96, 104, new Color(43, 35, 52, 245));
        tabSelectedPatch = loadGuiPatch(creativeTabsAtlas, 4, 148, 96, 96, new Color(255, 179, 0, 245));
        scrollbarKnobPatch = loadGuiPatch(creativeTabsAtlas, 936, 4, 32, 64, new Color(28, 205, 244, 245));
        recipeButtonPatch = loadGuiPatch(recipeButtonAtlas, 0, 0, 42, 38, new Color(90, 230, 95, 245));
    }

    private int loadGuiTexture(final String name, final Color fallbackColor) {
        final BufferedImage image = decodeImage(resourcePackLoader.loadGuiTexture(name));
        return uploadTexture(image == null ? createSolidImage(16, 16, fallbackColor) : image);
    }

    private int loadGuiSprite(final BufferedImage atlas, final int u, final int v, final int w, final int h, final Color fallbackColor) {
        if (atlas == null) {
            return uploadTexture(createSolidImage(w, h, fallbackColor));
        }

        final double scaleX = atlas.getWidth() / 256.0;
        final double scaleY = atlas.getHeight() / 256.0;

        final int sx = clampInt((int) Math.round(u * scaleX), 0, Math.max(0, atlas.getWidth() - 1));
        final int sy = clampInt((int) Math.round(v * scaleY), 0, Math.max(0, atlas.getHeight() - 1));
        int sw = Math.max(1, (int) Math.round(w * scaleX));
        int sh = Math.max(1, (int) Math.round(h * scaleY));

        if (sx + sw > atlas.getWidth()) {
            sw = Math.max(1, atlas.getWidth() - sx);
        }
        if (sy + sh > atlas.getHeight()) {
            sh = Math.max(1, atlas.getHeight() - sy);
        }

        final BufferedImage cropped = copyRegion(atlas, sx, sy, sw, sh);
        return uploadTexture(cropped);
    }

    private GuiTexture loadGuiPatch(final BufferedImage atlas, final int x, final int y, final int width, final int height, final Color fallbackColor) {
        final BufferedImage patchImage = atlas == null
                ? createSolidImage(width, height, fallbackColor)
                : copyRegionSafe(atlas, x, y, width, height, fallbackColor);
        final int textureId = uploadTexture(patchImage);
        guiPatchTextures.add(textureId);
        return new GuiTexture(textureId, patchImage.getWidth(), patchImage.getHeight());
    }

    private void loadObjectIcons() {
        final Color grassTint = resolveGrassTint();
        for (final GameObject item : registries.items().values()) {
            final ModelComponent model = item.has(ModelComponent.class)
                    ? item.get(ModelComponent.class)
                    : new ModelComponent(item.id().path());

            final GuiItemModel guiModel = loadMinecraftItemGuiModel(item.id().path(), grassTint);
            if (guiModel != null) {
                objectGuiModels.put(item.id(), guiModel);
            }

            BufferedImage icon = loadItemIconImage(item, model);
            if (icon == null) {
                icon = createSolidImage(16, 16, fallbackColor(item, model));
            } else if (model.hasTint("grass")) {
                icon = multiplyTint(icon, grassTint);
            } else if (model.hasTint("water")) {
                icon = multiplyTint(icon, new Color(62, 128, 216, 200));
            }
            final int textureId = uploadTexture(icon);
            objectIcons.put(item.id(), textureId);
            if (fallbackIcon == 0 || DIRT_ID.equals(item.id())) {
                fallbackIcon = textureId;
            }
        }
    }

    private BufferedImage loadItemIconImage(final GameObject item, final ModelComponent model) {
        BufferedImage icon = loadMinecraftItemModelIcon(item.id().path());
            if (icon != null) {
                return icon;
            }
        if (item.has(BlockItemComponent.class)) {
            icon = decodeImage(resourcePackLoader.loadBlockTexture(model.textureCandidates()));
            if (icon == null && model.hasFrontCandidates()) {
                icon = decodeImage(resourcePackLoader.loadBlockTexture(model.frontCandidates()));
            }
            if (icon == null) {
                icon = decodeImage(resourcePackLoader.loadBlockTexture(model.topCandidates()));
            }
            if (icon == null) {
                icon = decodeImage(resourcePackLoader.loadBlockTexture(model.sideCandidates()));
            }
            return icon;
        }

        return decodeImage(resourcePackLoader.loadItemTexture(model.textureCandidates()));
    }

    private boolean drawGuiModelIcon(final GameObject item, final float iconX, final float iconY, final float iconSize) {
        if (item == null || viewportWidth <= 0 || viewportHeight <= 0) {
            return false;
        }

        final GuiItemModel model = objectGuiModels.get(item.id());
        if (model == null || model.quads().isEmpty()) {
            return false;
        }

        final boolean depthWasEnabled = glIsEnabled(GL_DEPTH_TEST);
        final int previousDepthFunc = glGetInteger(GL_DEPTH_FUNC);
        glClear(GL_DEPTH_BUFFER_BIT);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);

        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, viewportWidth, viewportHeight, 0, -1000.0, 1000.0);

        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        final GuiDisplay display = model.display();
        final float baseScale = iconSize / 16.0f;
        glTranslatef(
                iconX + iconSize * 0.5f + display.translation()[0] * baseScale,
                iconY + iconSize * 0.5f - display.translation()[1] * baseScale,
                display.translation()[2] * baseScale
        );
        glScalef(
                baseScale * display.scale()[0],
                -baseScale * display.scale()[1],
                baseScale * display.scale()[2]
        );
        glRotatef(display.rotation()[2], 0.0f, 0.0f, 1.0f);
        glRotatef(display.rotation()[1], 0.0f, 1.0f, 0.0f);
        glRotatef(display.rotation()[0], 1.0f, 0.0f, 0.0f);
        glTranslatef(-8.0f, -8.0f, -8.0f);

        for (final GuiQuad quad : model.quads()) {
            glBindTexture(GL_TEXTURE_2D, quad.textureId());
            final float shade = clamp(quad.shade(), 0.0f, 1.0f);
            glColor3f(shade, shade, shade);
            glBegin(GL_QUADS);
            for (int i = 0; i < quad.vertices().length; i++) {
                glTexCoord2f(quad.uvs()[i][0], quad.uvs()[i][1]);
                glVertex3f(quad.vertices()[i][0], quad.vertices()[i][1], quad.vertices()[i][2]);
            }
            glEnd();
        }

        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glDepthFunc(previousDepthFunc);

        if (!depthWasEnabled) {
            glDisable(GL_DEPTH_TEST);
        }
        return true;
    }

    private GuiItemModel loadMinecraftItemGuiModel(final String itemName, final Color grassTint) {
        final ResolvedGuiModel model = resolveGuiModel("item/" + itemName, new HashSet<>());
        if (model == null || model.elements().isEmpty()) {
            return null;
        }

        final List<GuiQuad> quads = new ArrayList<>();
        try {
            for (final Object rawElement : model.elements()) {
                final Map<String, Object> element = object(rawElement);
                final float[] from = vec3(element.get("from"));
                final float[] to = vec3(element.get("to"));
                final ModelRotation rotation = readRotation(object(element.get("rotation")));
                final boolean shade = bool(element, "shade", true);
                final Map<String, Object> faces = object(element.get("faces"));
                for (final Map.Entry<String, Object> faceEntry : faces.entrySet()) {
                    final String faceName = faceEntry.getKey();
                    final Map<String, Object> face = object(faceEntry.getValue());
                    final String texturePath = resolveTexturePath(string(face, "texture", ""), model.textures());
                    if (texturePath.isBlank()) {
                        continue;
                    }

                    final int textureId = loadGuiModelTexture(texturePath, face.containsKey("tintindex") ? grassTint : null);
                    final float[][] vertices = faceVertices(faceName, from, to);
                    if (vertices.length == 0) {
                        continue;
                    }
                    for (int i = 0; i < vertices.length; i++) {
                        vertices[i] = rotate(vertices[i], rotation);
                    }
                    quads.add(new GuiQuad(
                            textureId,
                            shadeForFace(faceName, shade),
                            faceUvs(face.get("uv"), Math.round(number(face.get("rotation"), 0.0f))),
                            vertices
                    ));
                }
            }
        } catch (RuntimeException ignored) {
            return null;
        }

        return quads.isEmpty() ? null : new GuiItemModel(quads, model.display());
    }

    private ResolvedGuiModel resolveGuiModel(final String rawModelPath, final Set<String> stack) {
        final ModelReference reference = parseMinecraftModelReference(rawModelPath);
        if (reference.path().isBlank()) {
            return null;
        }

        final String cacheKey = reference.family() + "/" + reference.path();
        final ResolvedGuiModel cached = resolvedGuiModelCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        if (!stack.add(cacheKey)) {
            return null;
        }

        try {
            final String modelJson = "block".equals(reference.family())
                    ? resourcePackLoader.loadBlockModel(reference.path())
                    : resourcePackLoader.loadItemModel(reference.path());
            if (modelJson == null || modelJson.isBlank()) {
                return null;
            }

            final Map<String, Object> root = JsonParser.parseObject(modelJson);
            final Map<String, Object> textures = new LinkedHashMap<>();
            List<Object> elements = list(root.get("elements"));
            GuiDisplay display = GuiDisplay.DEFAULT;

            final String parentPath = string(root, "parent", "");
            if (!parentPath.isBlank()) {
                final ResolvedGuiModel parent = resolveGuiModel(parentPath, stack);
                if (parent != null) {
                    textures.putAll(parent.textures());
                    display = parent.display();
                    if (elements.isEmpty()) {
                        elements = parent.elements();
                    }
                }
            }

            textures.putAll(object(root.get("textures")));
            final Map<String, Object> guiDisplay = object(object(root.get("display")).get("gui"));
            if (!guiDisplay.isEmpty()) {
                display = readGuiDisplay(guiDisplay, display);
            }

            final ResolvedGuiModel resolved = new ResolvedGuiModel(elements, textures, display);
            resolvedGuiModelCache.put(cacheKey, resolved);
            return resolved;
        } catch (RuntimeException ignored) {
            return null;
        } finally {
            stack.remove(cacheKey);
        }
    }

    private GuiDisplay readGuiDisplay(final Map<String, Object> guiDisplay, final GuiDisplay fallback) {
        return new GuiDisplay(
                vec3(guiDisplay.getOrDefault("rotation", fallback.rotation())),
                vec3(guiDisplay.getOrDefault("translation", fallback.translation())),
                vec3(guiDisplay.getOrDefault("scale", fallback.scale()))
        );
    }

    private int loadGuiModelTexture(final String texturePath, final Color tint) {
        final String normalizedPath = normalizeMinecraftTexturePath(texturePath);
        final String cacheKey = normalizedPath + "|" + (tint == null ? "" : tint.getRGB());
        final Integer cached = guiModelTextureCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        BufferedImage image = decodeImage(resourcePackLoader.loadMinecraftTexture(normalizedPath));
        if (image == null) {
            image = createSolidImage(16, 16, tint == null ? new Color(185, 82, 124, 255) : tint);
        } else if (tint != null) {
            image = multiplyTint(image, tint);
        }
        final int textureId = uploadTexture(image);
        guiModelTextureCache.put(cacheKey, textureId);
        return textureId;
    }

    private BufferedImage loadMinecraftItemModelIcon(final String itemName) {
        final ResolvedModelTextures model = resolveMinecraftModelTextures("item/" + itemName, new HashSet<>());
        if (model == null || model.textures().isEmpty()) {
            return null;
        }

        final String texturePath = firstIconTexturePath(model.textures());
        if (texturePath.isBlank()) {
            return null;
        }
        return decodeImage(resourcePackLoader.loadMinecraftTexture(normalizeMinecraftTexturePath(texturePath)));
    }

    private ResolvedModelTextures resolveMinecraftModelTextures(final String rawModelPath, final Set<String> stack) {
        final ModelReference reference = parseMinecraftModelReference(rawModelPath);
        if (reference.path().isBlank()) {
            return null;
        }

        final String cacheKey = reference.family() + "/" + reference.path();
        final ResolvedModelTextures cached = resolvedModelTextureCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        if (!stack.add(cacheKey)) {
            return null;
        }

        try {
            final String modelJson = "block".equals(reference.family())
                    ? resourcePackLoader.loadBlockModel(reference.path())
                    : resourcePackLoader.loadItemModel(reference.path());
            if (modelJson == null || modelJson.isBlank()) {
                return null;
            }

            final Map<String, Object> root = JsonParser.parseObject(modelJson);
            final Map<String, Object> textures = new LinkedHashMap<>();

            final String parentPath = string(root, "parent", "");
            if (!parentPath.isBlank()) {
                final ResolvedModelTextures parent = resolveMinecraftModelTextures(parentPath, stack);
                if (parent != null) {
                    textures.putAll(parent.textures());
                }
            }

            textures.putAll(object(root.get("textures")));
            final ResolvedModelTextures resolved = new ResolvedModelTextures(textures);
            resolvedModelTextureCache.put(cacheKey, resolved);
            return resolved;
        } catch (RuntimeException ignored) {
            return null;
        } finally {
            stack.remove(cacheKey);
        }
    }

    private String firstIconTexturePath(final Map<String, Object> textures) {
        final String[] preferredKeys = {
                "layer0", "particle", "all", "front", "side", "north", "south", "east", "west", "top", "up", "bottom", "down"
        };
        for (final String key : preferredKeys) {
            final Object value = textures.get(key);
            final String resolved = resolveTexturePath(value == null ? "" : String.valueOf(value), textures);
            if (!resolved.isBlank()) {
                return resolved;
            }
        }

        for (final Object value : textures.values()) {
            final String resolved = resolveTexturePath(value == null ? "" : String.valueOf(value), textures);
            if (!resolved.isBlank()) {
                return resolved;
            }
        }
        return "";
    }

    private String resolveTexturePath(final String textureReference, final Map<String, Object> textures) {
        String key = textureReference == null ? "" : textureReference.trim();
        final Set<String> seen = new HashSet<>();
        while (key.startsWith("#")) {
            key = key.substring(1);
            if (!seen.add(key)) {
                return "";
            }
            final Object resolved = textures.get(key);
            if (resolved == null) {
                return "";
            }
            key = String.valueOf(resolved).trim();
        }
        return key;
    }

    private ModelReference parseMinecraftModelReference(final String modelPath) {
        String path = modelPath == null ? "" : modelPath.trim();
        final int namespaceIndex = path.indexOf(':');
        if (namespaceIndex >= 0) {
            path = path.substring(namespaceIndex + 1);
        }

        String family = "item";
        if (path.startsWith("block/")) {
            family = "block";
            path = path.substring("block/".length());
        } else if (path.startsWith("item/")) {
            path = path.substring("item/".length());
        }
        return new ModelReference(family, path);
    }

    private String normalizeMinecraftTexturePath(final String texturePath) {
        String path = texturePath == null ? "" : texturePath.trim();
        final int namespaceIndex = path.indexOf(':');
        if (namespaceIndex >= 0) {
            path = path.substring(namespaceIndex + 1);
        }
        return path;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> object(final Object value) {
        if (value == null) {
            return Map.of();
        }
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        return (Map<String, Object>) map;
    }

    private List<Object> list(final Object value) {
        if (value instanceof List<?> rawList) {
            return new ArrayList<>(rawList);
        }
        return List.of();
    }

    private String string(final Map<String, Object> object, final String key, final String defaultValue) {
        final Object value = object.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    private boolean bool(final Map<String, Object> object, final String key, final boolean defaultValue) {
        final Object value = object.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private float number(final Object value, final float defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.floatValue();
        }
        return Float.parseFloat(String.valueOf(value));
    }

    private float[] vec3(final Object value) {
        if (value instanceof float[] values && values.length >= 3) {
            return new float[]{values[0], values[1], values[2]};
        }
        final List<Object> values = list(value);
        return new float[]{
                number(values.size() > 0 ? values.get(0) : null, 0.0f),
                number(values.size() > 1 ? values.get(1) : null, 0.0f),
                number(values.size() > 2 ? values.get(2) : null, 0.0f)
        };
    }

    private float[] uv(final Object value) {
        final List<Object> values = list(value);
        return new float[]{
                number(values.size() > 0 ? values.get(0) : null, 0.0f) / 16.0f,
                number(values.size() > 1 ? values.get(1) : null, 0.0f) / 16.0f,
                number(values.size() > 2 ? values.get(2) : null, 16.0f) / 16.0f,
                number(values.size() > 3 ? values.get(3) : null, 16.0f) / 16.0f
        };
    }

    private float[][] faceUvs(final Object value, final int rotationDegrees) {
        final float[] uv = uv(value);
        final float[][] corners = new float[][]{
                {uv[0], uv[1]},
                {uv[2], uv[1]},
                {uv[2], uv[3]},
                {uv[0], uv[3]}
        };

        final int steps = Math.floorMod(rotationDegrees / 90, 4);
        if (steps == 0) {
            return corners;
        }

        final float[][] rotated = new float[4][2];
        for (int i = 0; i < corners.length; i++) {
            rotated[i] = corners[Math.floorMod(i + steps, corners.length)];
        }
        return rotated;
    }

    private ModelRotation readRotation(final Map<String, Object> rotation) {
        if (rotation.isEmpty()) {
            return ModelRotation.NONE;
        }
        return new ModelRotation(
                string(rotation, "axis", ""),
                number(rotation.get("angle"), 0.0f),
                vec3(rotation.get("origin"))
        );
    }

    private float[][] faceVertices(final String faceName, final float[] from, final float[] to) {
        return switch (faceName) {
            case "north" -> new float[][]{
                    {to[0], from[1], from[2]},
                    {from[0], from[1], from[2]},
                    {from[0], to[1], from[2]},
                    {to[0], to[1], from[2]}
            };
            case "south" -> new float[][]{
                    {from[0], from[1], to[2]},
                    {to[0], from[1], to[2]},
                    {to[0], to[1], to[2]},
                    {from[0], to[1], to[2]}
            };
            case "east" -> new float[][]{
                    {to[0], from[1], to[2]},
                    {to[0], from[1], from[2]},
                    {to[0], to[1], from[2]},
                    {to[0], to[1], to[2]}
            };
            case "west" -> new float[][]{
                    {from[0], from[1], from[2]},
                    {from[0], from[1], to[2]},
                    {from[0], to[1], to[2]},
                    {from[0], to[1], from[2]}
            };
            case "up" -> new float[][]{
                    {from[0], to[1], to[2]},
                    {to[0], to[1], to[2]},
                    {to[0], to[1], from[2]},
                    {from[0], to[1], from[2]}
            };
            case "down" -> new float[][]{
                    {from[0], from[1], from[2]},
                    {to[0], from[1], from[2]},
                    {to[0], from[1], to[2]},
                    {from[0], from[1], to[2]}
            };
            default -> new float[0][0];
        };
    }

    private float[] rotate(final float[] vertex, final ModelRotation rotation) {
        if (rotation == ModelRotation.NONE || rotation.axis().isBlank() || rotation.angle() == 0.0f) {
            return vertex;
        }

        final double radians = Math.toRadians(rotation.angle());
        final double sin = Math.sin(radians);
        final double cos = Math.cos(radians);
        final float[] origin = rotation.origin();
        final float x = vertex[0] - origin[0];
        final float y = vertex[1] - origin[1];
        final float z = vertex[2] - origin[2];

        return switch (rotation.axis()) {
            case "x" -> new float[]{
                    origin[0] + x,
                    origin[1] + (float) (y * cos - z * sin),
                    origin[2] + (float) (y * sin + z * cos)
            };
            case "y" -> new float[]{
                    origin[0] + (float) (x * cos + z * sin),
                    origin[1] + y,
                    origin[2] + (float) (-x * sin + z * cos)
            };
            case "z" -> new float[]{
                    origin[0] + (float) (x * cos - y * sin),
                    origin[1] + (float) (x * sin + y * cos),
                    origin[2] + z
            };
            default -> vertex;
        };
    }

    private float shadeForFace(final String faceName, final boolean shade) {
        if (!shade) {
            return 1.0f;
        }
        return switch (faceName) {
            case "down" -> 0.72f;
            case "east", "west" -> 0.83f;
            case "north", "south" -> 0.88f;
            default -> 1.0f;
        };
    }

    private int iconFor(final GameObject type) {
        if (type == null) {
            return fallbackIcon;
        }
        return objectIcons.getOrDefault(type.id(), fallbackIcon);
    }

    private BufferedImage decodeImage(final byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        try {
            return ImageIO.read(new ByteArrayInputStream(data));
        } catch (IOException e) {
            return null;
        }
    }

    private BufferedImage copyRegion(final BufferedImage image, final int x, final int y, final int width, final int height) {
        final BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int iy = 0; iy < height; iy++) {
            for (int ix = 0; ix < width; ix++) {
                out.setRGB(ix, iy, image.getRGB(x + ix, y + iy));
            }
        }
        return out;
    }

    private BufferedImage copyRegionSafe(final BufferedImage image, final int x, final int y, final int width, final int height, final Color fallbackColor) {
        final BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        final int fallback = fallbackColor.getRGB();
        for (int iy = 0; iy < height; iy++) {
            for (int ix = 0; ix < width; ix++) {
                final int sourceX = x + ix;
                final int sourceY = y + iy;
                if (sourceX < 0 || sourceX >= image.getWidth() || sourceY < 0 || sourceY >= image.getHeight()) {
                    out.setRGB(ix, iy, fallback);
                } else {
                    out.setRGB(ix, iy, image.getRGB(sourceX, sourceY));
                }
            }
        }
        return out;
    }

    private BufferedImage createSolidImage(final int width, final int height, final Color color) {
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        final int argb = color.getRGB();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, argb);
            }
        }
        return image;
    }

    private BufferedImage multiplyTint(final BufferedImage source, final Color tint) {
        final BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        final float tr = tint.getRed() / 255.0f;
        final float tg = tint.getGreen() / 255.0f;
        final float tb = tint.getBlue() / 255.0f;

        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                final int argb = source.getRGB(x, y);
                final int a = (argb >>> 24) & 0xff;
                final int r = (argb >>> 16) & 0xff;
                final int g = (argb >>> 8) & 0xff;
                final int b = argb & 0xff;

                final int rr = clampInt(Math.round(r * tr), 0, 255);
                final int gg = clampInt(Math.round(g * tg), 0, 255);
                final int bb = clampInt(Math.round(b * tb), 0, 255);
                out.setRGB(x, y, (a << 24) | (rr << 16) | (gg << 8) | bb);
            }
        }
        return out;
    }

    private Color resolveGrassTint() {
        final BufferedImage colorMap = decodeImage(resourcePackLoader.loadColorMapTexture("grass"));
        if (colorMap == null) {
            return DEFAULT_GRASS_TINT;
        }

        final double temperature = 0.8;
        double rainfall = 0.4;
        rainfall *= temperature;

        final int colorX = clampInt((int) ((1.0 - temperature) * 255.0), 0, 255);
        final int colorY = clampInt((int) ((1.0 - rainfall) * 255.0), 0, 255);

        final int sampleX = clampInt((int) Math.round((colorX / 255.0) * (colorMap.getWidth() - 1)), 0, colorMap.getWidth() - 1);
        final int sampleY = clampInt((int) Math.round((colorY / 255.0) * (colorMap.getHeight() - 1)), 0, colorMap.getHeight() - 1);
        return new Color(colorMap.getRGB(sampleX, sampleY), true);
    }

    private Color fallbackColor(final GameObject type, final ModelComponent model) {
        final int defaultRgba = switch (type.id().path()) {
            case "grass" -> rgba(118, 190, 74, 255);
            case "dirt" -> rgba(138, 90, 51, 255);
            case "stone" -> rgba(123, 123, 123, 255);
            case "bedrock" -> rgba(65, 65, 65, 255);
            case "water" -> rgba(62, 128, 216, 220);
            case "wood" -> rgba(122, 74, 26, 255);
            default -> rgba(185, 82, 124, 255);
        };
        final int rgba = model.fallbackColor("top", model.fallbackColor("default", defaultRgba));
        return new Color(
                (rgba >>> 24) & 0xff,
                (rgba >>> 16) & 0xff,
                (rgba >>> 8) & 0xff,
                rgba & 0xff
        );
    }

    private int rgba(final int r, final int g, final int b, final int a) {
        return ((r & 0xff) << 24) | ((g & 0xff) << 16) | ((b & 0xff) << 8) | (a & 0xff);
    }

    private int uploadTexture(final BufferedImage image) {
        return textureLoader.uploadTexture(image);
    }

    private int clampInt(final int value, final int min, final int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String shortName(final String value, final int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value == null ? "" : value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private record CreativeInventoryLayout(
            int columns,
            float panelX,
            float panelY,
            float panelWidth,
            float panelHeight,
            float slotStartX,
            float slotStartY,
            float slotSize,
            float slotGap,
            float iconSize,
            float hotbarStartX,
            float hotbarStartY,
            float searchX,
            float searchY,
            float searchFieldX,
            float searchFieldY,
            float searchFieldWidth,
            float searchFieldHeight,
            float scale,
            int rows,
            int pageSize,
            int page,
            int pageCount
    ) {
    }

    private record SurvivalInventoryLayout(
            float panelX,
            float panelY,
            float panelWidth,
            float panelHeight,
            float storageStartX,
            float storageStartY,
            float hotbarStartX,
            float hotbarStartY,
            float slotSize,
            float slotGap,
            float iconSize,
            float scale
    ) {
    }

    private record SlotRowLayout(float slotStartX, float slotStartY, float slotSize, float slotGap, int slotCount) {
    }

    private record MenuPanel(float x, float y, float width, float height) {
    }

    private record MenuButton(MenuAction action, float x, float y, float width, float height) {
    }

    private record GuiTexture(int textureId, int width, int height) {
    }

    private record GuiItemModel(List<GuiQuad> quads, GuiDisplay display) {
    }

    private record GuiQuad(int textureId, float shade, float[][] uvs, float[][] vertices) {
    }

    private record ResolvedGuiModel(List<Object> elements, Map<String, Object> textures, GuiDisplay display) {
    }

    private record GuiDisplay(float[] rotation, float[] translation, float[] scale) {
        private static final GuiDisplay DEFAULT = new GuiDisplay(
                new float[]{30.0f, 225.0f, 0.0f},
                new float[]{0.0f, 0.0f, 0.0f},
                new float[]{0.625f, 0.625f, 0.625f}
        );
    }

    private record ModelRotation(String axis, float angle, float[] origin) {
        private static final ModelRotation NONE = new ModelRotation("", 0.0f, new float[]{0.0f, 0.0f, 0.0f});
    }

    private record ModelReference(String family, String path) {
    }

    private record ResolvedModelTextures(Map<String, Object> textures) {
    }
}
