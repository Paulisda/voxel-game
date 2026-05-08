package de.paul.voxelgame.renderer;

import de.paul.voxelgame.GameConfig;
import de.paul.voxelgame.assets.ResourcePackLoader;
import de.paul.voxelgame.audio.MusicManager;
import de.paul.voxelgame.core.ChatSystem;
import de.paul.voxelgame.audio.SoundEffectManager;
import de.paul.voxelgame.core.DisplayManager;
import de.paul.voxelgame.core.InventorySystem;
import de.paul.voxelgame.core.InventoryStack;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glIsEnabled;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glOrtho;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glTexCoord2f;
import static org.lwjgl.opengl.GL11.glVertex2f;

public class HudRenderer {
    private static final ResourceId DIRT_ID = ResourceId.of("game:dirt");
    private static final int HUD_SLOT_COUNT = 9;
    private static final int STAT_PIP_COUNT = 10;
    private static final float HUD_SCALE = 2.0f;
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
    private int fallbackIcon;

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

    public void render(final int width, final int height, final double mouseX, final double mouseY) {
        if (width <= 0 || height <= 0) {
            return;
        }

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
        final float hudScale = HUD_SCALE * scale;
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
            drawStackIcon(stack, iconX, iconY, iconSize, font(11, scale));
        }

        final GameObject selectedItem = player.getHotbarItem(selected);
        if (selectedItem != null) {
            final String selectedName = localization.translate("ui.hotbar.selected") + ": " + localization.objectName(selectedItem);
            textRenderer.drawCenteredText(selectedName, width * 0.5f, hotbarY - s(42.0f, scale), font(14, scale), TEXT_COLOR);
        }

        final float pipSize = ICON_W * hudScale;
        final float pipStep = 8.0f * hudScale;
        final float statsY = hotbarY - pipSize - (4.0f * hudScale);
        final float heartsX = hotbarX;
        final float hungerX = hotbarX + hotbarWidth - (pipSize + pipStep * (STAT_PIP_COUNT - 1));

        drawStatsRow(player.getHealthPoints(), heartsX, statsY, pipStep, pipSize, heartEmptyTexture, heartHalfTexture, heartFullTexture);
        drawStatsRow(player.getHungerPoints(), hungerX, statsY, pipStep, pipSize, hungerEmptyTexture, hungerHalfTexture, hungerFullTexture);

        if (inventorySystem.isOpen()) {
            renderInventory(width, height);
        }
        if (menuSystem.isOpen()) {
            renderMenu(width, height);
        }
        renderChat(width, height, hotbarY, scale);
        renderCarriedItem(mouseX, mouseY, scale);

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

        for (final Integer textureId : textureIds) {
            textureLoader.deleteTexture(textureId == null ? 0 : textureId);
        }
        objectIcons.clear();
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
        if (!inventorySystem.isOpen() || !GameConfig.isCreative()) {
            return false;
        }
        final CreativeInventoryLayout layout = creativeInventoryLayout(inventorySystem.filteredInventoryEntries(localization).size(), width, height);
        return mouseX >= layout.searchFieldX() && mouseX <= layout.searchFieldX() + layout.searchFieldWidth()
                && mouseY >= layout.searchFieldY() && mouseY <= layout.searchFieldY() + layout.searchFieldHeight();
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

    public int inventoryPageCount(final int width, final int height) {
        if (!GameConfig.isCreative()) {
            return 1;
        }
        return creativeInventoryLayout(inventorySystem.filteredInventoryEntries(localization).size(), width, height).pageCount();
    }

    private SlotRowLayout hudHotbarLayout(final int width, final int height) {
        final float scale = uiScale(width, height);
        final float hudScale = HUD_SCALE * scale;
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
        final float scale = uiScale(width, height);
        final SurvivalInventoryLayout layout = survivalInventoryLayout(width, height);

        drawColoredQuad(0, 0, width, height, 0.03f, 0.035f, 0.04f, 0.42f);
        drawTiledTexture(optionsBackgroundTexture, 0.0f, 0.0f, width, height, s(32.0f, scale), 0.36f);
        drawColoredQuad(0, 0, width, height, 0.02f, 0.025f, 0.03f, 0.20f);
        drawTexturedQuad(inventoryPanelTexture, layout.panelX(), layout.panelY(), layout.panelSize(), layout.panelSize());
        renderStorageItems(layout);
        renderHotbarItems(layout.hotbarStartX(), layout.hotbarStartY(), layout.slotSize(), layout.slotGap(), layout.iconSize());
    }

    private void renderCreativeInventory(final int width, final int height) {
        final List<GameObject> entries = inventorySystem.filteredInventoryEntries(localization);
        final CreativeInventoryLayout layout = creativeInventoryLayout(entries.size(), width, height);
        inventorySystem.clampToPageCount(layout.pageCount());

        drawColoredQuad(0, 0, width, height, 0.03f, 0.035f, 0.04f, 0.42f);
        drawTiledTexture(optionsBackgroundTexture, 0.0f, 0.0f, width, height, s(32.0f, layout.scale()), 0.42f);
        drawColoredQuad(0, 0, width, height, 0.02f, 0.025f, 0.03f, 0.22f);
        final int backgroundTexture = inventorySystem.searchQuery().isBlank()
                ? creativeInventoryItemsTexture
                : creativeInventorySearchTexture;
        drawTexturedQuad(backgroundTexture, layout.panelX(), layout.panelY(), layout.panelWidth(), layout.panelHeight());
        if (layout.pageCount() > 1) {
            final String pageLabel = (layout.page() + 1) + "/" + layout.pageCount();
            textRenderer.drawCenteredText(pageLabel, layout.panelX() + layout.panelWidth() * 0.86f, layout.panelY() + layout.panelHeight() * 0.16f, font(12, layout.scale()), MUTED_TEXT_COLOR);
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
            drawTexturedQuad(iconFor(entry), iconX, iconY, layout.iconSize(), layout.iconSize());
        }
        renderHotbarItems(layout.hotbarStartX(), layout.hotbarStartY(), layout.slotSize(), layout.slotGap(), layout.iconSize());
    }

    private void drawCreativeSearchText(final CreativeInventoryLayout layout) {
        final String query = inventorySystem.searchQuery();
        if (query.isBlank() && !inventorySystem.isSearchFocused()) {
            return;
        }
        final String searchText = query.isBlank() ? localization.translate("ui.inventory.search") : shortName(query, 36);
        final Color color = query.isBlank() ? MUTED_TEXT_COLOR : TEXT_COLOR;
        textRenderer.drawText(searchText, layout.searchX(), layout.searchY(), font(12, layout.scale()), color);
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
        drawTexturedQuad(iconFor(stack.item()), iconX, iconY, iconSize, iconSize);
        if (stack.count() <= 1) {
            return;
        }
        final String count = Integer.toString(stack.count());
        final float labelX = iconX + iconSize - Math.max(9.0f, count.length() * countFont * 0.45f);
        final float labelY = iconY + iconSize - countFont * 0.82f;
        drawColoredQuad(labelX - 1.0f, labelY - 1.0f, count.length() * countFont * 0.55f + 2.0f, countFont + 2.0f, 0.0f, 0.0f, 0.0f, 0.48f);
        textRenderer.drawText(count, labelX, labelY, countFont, TEXT_COLOR);
    }

    private CreativeInventoryLayout creativeInventoryLayout(final int entryCount, final int width, final int height) {
        final float scale = uiScale(width, height);
        final float panelSize = clamp(Math.min(width * 0.52f, height * 0.70f), s(360.0f, scale), s(620.0f, scale));
        final float panelX = centeredTextureX(width, panelSize, CREATIVE_TEXTURE_SIZE, CREATIVE_VISIBLE_CENTER_X);
        final float panelY = centeredTextureY(height, panelSize, CREATIVE_TEXTURE_SIZE, CREATIVE_VISIBLE_CENTER_Y);
        final float slotSize = panelSize * (CREATIVE_SLOT_SIZE / CREATIVE_TEXTURE_SIZE);
        final float slotGap = panelSize * ((CREATIVE_SLOT_STEP - CREATIVE_SLOT_SIZE) / CREATIVE_TEXTURE_SIZE);
        final float iconSize = panelSize * (CREATIVE_ICON_SIZE / CREATIVE_TEXTURE_SIZE);
        final int columns = CREATIVE_COLUMNS;
        final int rows = CREATIVE_ROWS;
        final int pageSize = Math.max(1, rows * columns);
        final int pageCount = Math.max(1, (int) Math.ceil(entryCount / (double) pageSize));
        final int page = Math.max(0, Math.min(inventorySystem.page(), pageCount - 1));
        return new CreativeInventoryLayout(
                columns,
                panelX,
                panelY,
                panelSize,
                panelSize,
                panelX + panelSize * (CREATIVE_SLOT_X / CREATIVE_TEXTURE_SIZE),
                panelY + panelSize * (CREATIVE_SLOT_Y / CREATIVE_TEXTURE_SIZE),
                slotSize,
                slotGap,
                iconSize,
                panelX + panelSize * (CREATIVE_HOTBAR_X / CREATIVE_TEXTURE_SIZE),
                panelY + panelSize * (CREATIVE_HOTBAR_Y / CREATIVE_TEXTURE_SIZE),
                panelX + panelSize * (CREATIVE_SEARCH_TEXT_X / CREATIVE_TEXTURE_SIZE),
                panelY + panelSize * (CREATIVE_SEARCH_TEXT_Y / CREATIVE_TEXTURE_SIZE),
                panelX + panelSize * (CREATIVE_SEARCH_FIELD_X / CREATIVE_TEXTURE_SIZE),
                panelY + panelSize * (CREATIVE_SEARCH_FIELD_Y / CREATIVE_TEXTURE_SIZE),
                panelSize * (CREATIVE_SEARCH_FIELD_W / CREATIVE_TEXTURE_SIZE),
                panelSize * (CREATIVE_SEARCH_FIELD_H / CREATIVE_TEXTURE_SIZE),
                scale,
                rows,
                pageSize,
                page,
                pageCount
        );
    }

    private SurvivalInventoryLayout survivalInventoryLayout(final int width, final int height) {
        final float scale = uiScale(width, height);
        final float panelSize = clamp(Math.min(width * 0.42f, height * 0.58f), s(300.0f, scale), s(512.0f, scale));
        final float panelX = centeredTextureX(width, panelSize, SURVIVAL_TEXTURE_SIZE, SURVIVAL_VISIBLE_CENTER_X);
        final float panelY = centeredTextureY(height, panelSize, SURVIVAL_TEXTURE_SIZE, SURVIVAL_VISIBLE_CENTER_Y);
        final float slotSize = panelSize * (SURVIVAL_SLOT_SIZE / SURVIVAL_TEXTURE_SIZE);
        final float slotGap = panelSize * ((SURVIVAL_SLOT_STEP - SURVIVAL_SLOT_SIZE) / SURVIVAL_TEXTURE_SIZE);
        final float iconSize = panelSize * (SURVIVAL_ICON_SIZE / SURVIVAL_TEXTURE_SIZE);
        return new SurvivalInventoryLayout(
                panelX,
                panelY,
                panelSize,
                panelX + panelSize * (SURVIVAL_SLOT_X / SURVIVAL_TEXTURE_SIZE),
                panelY + panelSize * (SURVIVAL_STORAGE_Y / SURVIVAL_TEXTURE_SIZE),
                panelX + panelSize * (SURVIVAL_SLOT_X / SURVIVAL_TEXTURE_SIZE),
                panelY + panelSize * (SURVIVAL_HOTBAR_Y / SURVIVAL_TEXTURE_SIZE),
                slotSize,
                slotGap,
                iconSize
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

    private void renderChat(final int width, final int height, final float hotbarY, final float scale) {
        final List<String> messages = chatSystem.recentMessages();
        final float left = s(10.0f, scale);
        final float lineHeight = s(18.0f, scale);
        float y = hotbarY - s(70.0f, scale) - lineHeight * Math.max(0, messages.size() - 1);
        for (final String message : messages) {
            final String text = shortName(message, Math.max(20, (int) (width / (s(8.0f, scale)))));
            drawColoredQuad(left - s(4.0f, scale), y - s(2.0f, scale), Math.min(width * 0.55f, text.length() * s(8.2f, scale)), lineHeight, 0.0f, 0.0f, 0.0f, 0.38f);
            textRenderer.drawText(text, left, y, font(12, scale), TEXT_COLOR);
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

    private void renderMenu(final int width, final int height) {
        final float scale = uiScale(width, height);
        drawColoredQuad(0, 0, width, height, 0.02f, 0.025f, 0.03f, 0.48f);
        drawTiledTexture(optionsBackgroundTexture, 0.0f, 0.0f, width, height, s(32.0f, scale), 0.56f);
        drawColoredQuad(0, 0, width, height, 0.02f, 0.025f, 0.03f, 0.32f);
        final float panelWidth = s(menuSystem.isOptions() ? 520.0f : 360.0f, scale);
        final float panelHeight = s(menuSystem.isOptions() ? 560.0f : 260.0f, scale);
        final float panelX = (width - panelWidth) * 0.5f;
        final float panelY = (height - panelHeight) * 0.44f;
        drawColoredQuad(panelX - s(5.0f, scale), panelY - s(5.0f, scale),
                panelWidth + s(10.0f, scale), panelHeight + s(10.0f, scale), 0.05f, 0.055f, 0.065f, 0.96f);
        drawTiledTexture(optionsBackgroundTexture, panelX, panelY, panelWidth, panelHeight, s(32.0f, scale), 0.80f);
        drawColoredQuad(panelX, panelY, panelWidth, panelHeight, 0.08f, 0.085f, 0.095f, 0.58f);

        if (menuSystem.isOptions()) {
            renderOptionsMenu(panelX, panelY, panelWidth, scale);
        } else {
            renderPauseMenu(panelX, panelY, panelWidth, scale);
        }
    }

    private void renderPauseMenu(final float panelX, final float panelY, final float panelWidth, final float scale) {
        textRenderer.drawCenteredBoldText(localization.translate("ui.menu.title"), panelX + panelWidth * 0.5f, panelY + s(24.0f, scale), font(24, scale), TEXT_COLOR);
        for (final MenuButton button : menuButtonsForPause(panelX, panelY, panelWidth, scale)) {
            drawMenuButton(button, labelFor(button.action()), scale);
        }
    }

    private void renderOptionsMenu(final float panelX, final float panelY, final float panelWidth, final float scale) {
        final float labelX = panelX + s(38.0f, scale);
        final float centerX = panelX + panelWidth * 0.5f;

        textRenderer.drawCenteredBoldText(localization.translate("ui.options.title"), centerX, panelY + s(22.0f, scale), font(23, scale), TEXT_COLOR);
        textRenderer.drawText(localization.translate("ui.options.sensitivity"), labelX, panelY + s(72.0f, scale), font(15, scale), MUTED_TEXT_COLOR);
        textRenderer.drawCenteredBoldText(String.format(Locale.ROOT, "%.2f", player.getMouseSensitivity()), centerX, panelY + s(100.0f, scale), font(18, scale), TEXT_COLOR);
        textRenderer.drawText(localization.translate("ui.options.music_volume"), labelX, panelY + s(134.0f, scale), font(15, scale), MUTED_TEXT_COLOR);
        textRenderer.drawCenteredBoldText(percent(musicManager.getVolume()), centerX, panelY + s(162.0f, scale), font(18, scale), TEXT_COLOR);
        textRenderer.drawText(localization.translate("ui.options.effects_volume"), labelX, panelY + s(196.0f, scale), font(15, scale), MUTED_TEXT_COLOR);
        textRenderer.drawCenteredBoldText(percent(soundEffectManager.getVolume()), centerX, panelY + s(224.0f, scale), font(18, scale), TEXT_COLOR);
        textRenderer.drawText(localization.translate("ui.options.fullscreen"), labelX, panelY + s(258.0f, scale), font(15, scale), MUTED_TEXT_COLOR);
        textRenderer.drawText(localization.translate("ui.options.resolution"), labelX, panelY + s(338.0f, scale), font(15, scale), MUTED_TEXT_COLOR);
        textRenderer.drawCenteredBoldText(displayManager.currentResolutionLabel(), centerX, panelY + s(366.0f, scale), font(18, scale), TEXT_COLOR);
        textRenderer.drawText(localization.translate("ui.options.language"), labelX, panelY + s(420.0f, scale), font(15, scale), MUTED_TEXT_COLOR);
        for (final MenuButton button : menuButtonsForOptions(panelX, panelY, panelWidth, scale)) {
            drawMenuButton(button, labelFor(button.action()), scale);
        }
    }

    private void drawMenuButton(final MenuButton button, final String label, final float scale) {
        drawColoredQuad(button.x(), button.y(), button.width(), button.height(), 0.055f, 0.06f, 0.07f, 1.0f);
        drawTexturedQuad(menuButtonTexture, button.x(), button.y(), button.width(), button.height());
        drawColoredQuad(button.x() + s(2.0f, scale), button.y() + s(2.0f, scale),
                button.width() - s(4.0f, scale), button.height() - s(4.0f, scale), 0.02f, 0.025f, 0.03f, 0.18f);
        textRenderer.drawCenteredBoldText(label, button.x() + button.width() * 0.5f, button.y() + button.height() * 0.24f, font(15, scale), TEXT_COLOR);
    }

    private String labelFor(final MenuAction action) {
        return switch (action) {
            case RESUME -> localization.translate("ui.menu.resume");
            case OPTIONS -> localization.translate("ui.menu.options");
            case EXIT -> localization.translate("ui.menu.exit");
            case BACK -> localization.translate("ui.options.back");
            case SENSITIVITY_DECREASE -> "-";
            case SENSITIVITY_INCREASE -> "+";
            case MUSIC_VOLUME_DECREASE -> "-";
            case MUSIC_VOLUME_INCREASE -> "+";
            case EFFECTS_VOLUME_DECREASE -> "-";
            case EFFECTS_VOLUME_INCREASE -> "+";
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
        final float scale = uiScale(width, height);
        final float panelWidth = s(menuSystem.isOptions() ? 520.0f : 360.0f, scale);
        final float panelHeight = s(menuSystem.isOptions() ? 560.0f : 260.0f, scale);
        final float panelX = (width - panelWidth) * 0.5f;
        final float panelY = (height - panelHeight) * 0.44f;
        return menuSystem.isOptions()
                ? menuButtonsForOptions(panelX, panelY, panelWidth, scale)
                : menuButtonsForPause(panelX, panelY, panelWidth, scale);
    }

    private MenuButton[] menuButtonsForPause(final float panelX, final float panelY, final float panelWidth, final float scale) {
        final float buttonWidth = s(260.0f, scale);
        final float buttonX = panelX + (panelWidth - buttonWidth) * 0.5f;
        return new MenuButton[]{
                new MenuButton(MenuAction.RESUME, buttonX, panelY + s(82.0f, scale), buttonWidth, s(40.0f, scale)),
                new MenuButton(MenuAction.OPTIONS, buttonX, panelY + s(136.0f, scale), buttonWidth, s(40.0f, scale)),
                new MenuButton(MenuAction.EXIT, buttonX, panelY + s(190.0f, scale), buttonWidth, s(40.0f, scale))
        };
    }

    private MenuButton[] menuButtonsForOptions(final float panelX, final float panelY, final float panelWidth, final float scale) {
        final float centerX = panelX + panelWidth * 0.5f;
        final float smallButtonWidth = s(42.0f, scale);
        final float smallButtonHeight = s(36.0f, scale);
        final float wideButtonWidth = s(220.0f, scale);
        final float wideButtonHeight = s(38.0f, scale);
        final float leftSmallX = centerX - s(110.0f, scale);
        final float rightSmallX = centerX + s(68.0f, scale);
        final float wideButtonX = centerX - wideButtonWidth * 0.5f;
        return new MenuButton[]{
                new MenuButton(MenuAction.SENSITIVITY_DECREASE, leftSmallX, panelY + s(92.0f, scale), smallButtonWidth, smallButtonHeight),
                new MenuButton(MenuAction.SENSITIVITY_INCREASE, rightSmallX, panelY + s(92.0f, scale), smallButtonWidth, smallButtonHeight),
                new MenuButton(MenuAction.MUSIC_VOLUME_DECREASE, leftSmallX, panelY + s(154.0f, scale), smallButtonWidth, smallButtonHeight),
                new MenuButton(MenuAction.MUSIC_VOLUME_INCREASE, rightSmallX, panelY + s(154.0f, scale), smallButtonWidth, smallButtonHeight),
                new MenuButton(MenuAction.EFFECTS_VOLUME_DECREASE, leftSmallX, panelY + s(216.0f, scale), smallButtonWidth, smallButtonHeight),
                new MenuButton(MenuAction.EFFECTS_VOLUME_INCREASE, rightSmallX, panelY + s(216.0f, scale), smallButtonWidth, smallButtonHeight),
                new MenuButton(MenuAction.FULLSCREEN_TOGGLE, wideButtonX, panelY + s(280.0f, scale), wideButtonWidth, wideButtonHeight),
                new MenuButton(MenuAction.RESOLUTION_PREVIOUS, leftSmallX, panelY + s(358.0f, scale), smallButtonWidth, smallButtonHeight),
                new MenuButton(MenuAction.RESOLUTION_NEXT, rightSmallX, panelY + s(358.0f, scale), smallButtonWidth, smallButtonHeight),
                new MenuButton(MenuAction.LANGUAGE_TOGGLE, wideButtonX, panelY + s(442.0f, scale), wideButtonWidth, wideButtonHeight),
                new MenuButton(MenuAction.BACK, wideButtonX, panelY + s(500.0f, scale), wideButtonWidth, wideButtonHeight)
        };
    }

    private String percent(final float value) {
        return Math.round(value * 100.0f) + "%";
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

        inventoryPanelTexture = loadGuiTexture("container/inventory", new Color(68, 70, 78, 245));
        creativeInventoryItemsTexture = loadGuiTexture("container/creative_inventory/tab_items", new Color(68, 70, 78, 245));
        creativeInventorySearchTexture = loadGuiTexture("container/creative_inventory/tab_item_search", new Color(68, 70, 78, 245));
        optionsBackgroundTexture = loadGuiTexture("options_background", new Color(42, 42, 48, 255));
        menuButtonTexture = loadGuiSprite(widgetsAtlas, 0, 66, 200, 20, new Color(82, 86, 96, 255));
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

    private void loadObjectIcons() {
        final Color grassTint = resolveGrassTint();
        for (final GameObject item : registries.items().values()) {
            final ModelComponent model = item.has(ModelComponent.class)
                    ? item.get(ModelComponent.class)
                    : new ModelComponent(item.id().path());

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
        if (item.has(BlockItemComponent.class)) {
            BufferedImage icon = decodeImage(resourcePackLoader.loadBlockTexture(model.textureCandidates()));
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
            float panelSize,
            float storageStartX,
            float storageStartY,
            float hotbarStartX,
            float hotbarStartY,
            float slotSize,
            float slotGap,
            float iconSize
    ) {
    }

    private record SlotRowLayout(float slotStartX, float slotStartY, float slotSize, float slotGap, int slotCount) {
    }

    private record MenuButton(MenuAction action, float x, float y, float width, float height) {
    }
}
