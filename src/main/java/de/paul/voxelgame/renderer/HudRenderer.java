package de.paul.voxelgame.renderer;

import de.paul.voxelgame.assets.ResourcePackLoader;
import de.paul.voxelgame.audio.MusicManager;
import de.paul.voxelgame.audio.SoundEffectManager;
import de.paul.voxelgame.core.InventorySystem;
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
    private static final float INVENTORY_SLOT_SIZE = 58.0f;
    private static final float INVENTORY_SLOT_GAP = 6.0f;
    private static final float INVENTORY_PADDING = 18.0f;
    private static final float INVENTORY_ICON_SIZE = 30.0f;
    private static final float INVENTORY_TITLE_HEIGHT = 24.0f;
    private static final Color TEXT_COLOR = new Color(242, 244, 246, 255);
    private static final Color MUTED_TEXT_COLOR = new Color(190, 197, 205, 255);

    private static final Color DEFAULT_GRASS_TINT = new Color(0x7F, 0xB2, 0x38);

    private final Player player;
    private final RegistryManager registries;
    private final InventorySystem inventorySystem;
    private final MenuSystem menuSystem;
    private final LocalizationManager localization;
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

    public HudRenderer(
            final Player player,
            final RegistryManager registries,
            final InventorySystem inventorySystem,
            final MenuSystem menuSystem,
            final LocalizationManager localization,
            final MusicManager musicManager,
            final SoundEffectManager soundEffectManager
    ) {
        this.player = player;
        this.registries = registries;
        this.inventorySystem = inventorySystem;
        this.menuSystem = menuSystem;
        this.localization = localization;
        this.musicManager = musicManager;
        this.soundEffectManager = soundEffectManager;
        loadHudTextures();
        loadObjectIcons();
    }

    public void render(final int width, final int height) {
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

        final float hotbarWidth = HOTBAR_W * HUD_SCALE;
        final float hotbarHeight = HOTBAR_H * HUD_SCALE;
        final float hotbarX = (width - hotbarWidth) * 0.5f;
        final float hotbarY = height - hotbarHeight - (12.0f * HUD_SCALE);

        drawTexturedQuad(hotbarBackgroundTexture, hotbarX, hotbarY, hotbarWidth, hotbarHeight);

        final int selected = player.getSelectedHotbarSlot();
        final float selectorX = hotbarX + ((selected * 20.0f) - 1.0f) * HUD_SCALE;
        final float selectorY = hotbarY - HUD_SCALE;
        drawTexturedQuad(hotbarSelectorTexture, selectorX, selectorY, SLOT_SELECTOR_W * HUD_SCALE, SLOT_SELECTOR_H * HUD_SCALE);

        final float iconSize = 16.0f * HUD_SCALE;
        for (int i = 0; i < HUD_SLOT_COUNT; i++) {
            final GameObject item = player.getHotbarItem(i);
            final int iconTexture = iconFor(item);
            final float iconX = hotbarX + (3 + i * 20) * HUD_SCALE;
            final float iconY = hotbarY + 3.0f * HUD_SCALE;
            drawTexturedQuad(iconTexture, iconX, iconY, iconSize, iconSize);
        }

        final GameObject selectedItem = player.getHotbarItem(selected);
        final String selectedName = localization.translate("ui.hotbar.selected") + ": " + localization.objectName(selectedItem);
        textRenderer.drawCenteredText(selectedName, width * 0.5f, hotbarY - 42.0f, 14, TEXT_COLOR);

        final float pipSize = ICON_W * HUD_SCALE;
        final float pipStep = 8.0f * HUD_SCALE;
        final float statsY = hotbarY - pipSize - (4.0f * HUD_SCALE);
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
        textureIds.addAll(objectIcons.values());

        for (final Integer textureId : textureIds) {
            textureLoader.deleteTexture(textureId == null ? 0 : textureId);
        }
        objectIcons.clear();
        textRenderer.destroy();
    }

    public GameObject pickInventoryItem(final double mouseX, final double mouseY, final int width, final int height) {
        if (!inventorySystem.isOpen()) {
            return null;
        }

        final List<GameObject> entries = inventorySystem.allInventoryEntries();
        final InventoryLayout layout = inventoryLayout(entries.size(), width, height);
        for (int i = 0; i < entries.size(); i++) {
            final int column = i % layout.columns();
            final int row = i / layout.columns();
            final float slotX = layout.slotStartX() + column * (INVENTORY_SLOT_SIZE + INVENTORY_SLOT_GAP);
            final float slotY = layout.slotStartY() + row * (INVENTORY_SLOT_SIZE + INVENTORY_SLOT_GAP);
            if (mouseX >= slotX && mouseX <= slotX + INVENTORY_SLOT_SIZE
                    && mouseY >= slotY && mouseY <= slotY + INVENTORY_SLOT_SIZE) {
                return entries.get(i);
            }
        }
        return null;
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

    private void renderInventory(final int width, final int height) {
        final List<GameObject> entries = inventorySystem.allInventoryEntries();
        final InventoryLayout layout = inventoryLayout(entries.size(), width, height);

        drawColoredQuad(0, 0, width, height, 0.03f, 0.035f, 0.04f, 0.42f);
        drawColoredQuad(layout.panelX() - 5.0f, layout.panelY() - 5.0f, layout.panelWidth() + 10.0f, layout.panelHeight() + 10.0f, 0.06f, 0.065f, 0.075f, 0.94f);
        drawColoredQuad(layout.panelX(), layout.panelY(), layout.panelWidth(), layout.panelHeight(), 0.16f, 0.17f, 0.18f, 0.96f);
        textRenderer.drawBoldText(localization.translate("ui.inventory.title"), layout.panelX() + INVENTORY_PADDING, layout.panelY() + 8.0f, 18, TEXT_COLOR);

        for (int i = 0; i < entries.size(); i++) {
            final int column = i % layout.columns();
            final int row = i / layout.columns();
            final float slotX = layout.slotStartX() + column * (INVENTORY_SLOT_SIZE + INVENTORY_SLOT_GAP);
            final float slotY = layout.slotStartY() + row * (INVENTORY_SLOT_SIZE + INVENTORY_SLOT_GAP);
            final GameObject entry = entries.get(i);

            final boolean block = entry.has(BlockItemComponent.class);
            drawColoredQuad(slotX, slotY, INVENTORY_SLOT_SIZE, INVENTORY_SLOT_SIZE, 0.055f, 0.06f, 0.067f, 1.0f);
            drawColoredQuad(slotX + 2.0f, slotY + 2.0f, INVENTORY_SLOT_SIZE - 4.0f, INVENTORY_SLOT_SIZE - 4.0f,
                    block ? 0.20f : 0.17f,
                    block ? 0.22f : 0.18f,
                    block ? 0.20f : 0.23f,
                    1.0f);

            final float iconX = slotX + (INVENTORY_SLOT_SIZE - INVENTORY_ICON_SIZE) * 0.5f;
            final float iconY = slotY + (INVENTORY_SLOT_SIZE - INVENTORY_ICON_SIZE) * 0.5f;
            drawTexturedQuad(iconFor(entry), iconX, iconY, INVENTORY_ICON_SIZE, INVENTORY_ICON_SIZE);
            textRenderer.drawCenteredText(shortName(localization.objectName(entry), 12), slotX + INVENTORY_SLOT_SIZE * 0.5f, slotY + 41.0f, 10, TEXT_COLOR);
        }
    }

    private InventoryLayout inventoryLayout(final int entryCount, final int width, final int height) {
        final int columns = Math.max(1, Math.min(INVENTORY_COLUMNS, Math.max(1, entryCount)));
        final int rows = Math.max(1, (int) Math.ceil(entryCount / (double) columns));
        final float panelWidth = INVENTORY_PADDING * 2.0f
                + columns * INVENTORY_SLOT_SIZE
                + (columns - 1) * INVENTORY_SLOT_GAP;
        final float panelHeight = INVENTORY_PADDING * 2.0f
                + INVENTORY_TITLE_HEIGHT
                + rows * INVENTORY_SLOT_SIZE
                + (rows - 1) * INVENTORY_SLOT_GAP;
        final float panelX = Math.max(12.0f, (width - panelWidth) * 0.5f);
        final float panelY = Math.max(18.0f, (height - panelHeight) * 0.42f);
        return new InventoryLayout(
                columns,
                panelX,
                panelY,
                panelWidth,
                panelHeight,
                panelX + INVENTORY_PADDING,
                panelY + INVENTORY_PADDING + INVENTORY_TITLE_HEIGHT
        );
    }

    private void renderMenu(final int width, final int height) {
        drawColoredQuad(0, 0, width, height, 0.02f, 0.025f, 0.03f, 0.58f);
        final float panelWidth = menuSystem.isOptions() ? 460.0f : 360.0f;
        final float panelHeight = menuSystem.isOptions() ? 430.0f : 260.0f;
        final float panelX = (width - panelWidth) * 0.5f;
        final float panelY = (height - panelHeight) * 0.44f;
        drawColoredQuad(panelX - 5.0f, panelY - 5.0f, panelWidth + 10.0f, panelHeight + 10.0f, 0.05f, 0.055f, 0.065f, 0.96f);
        drawColoredQuad(panelX, panelY, panelWidth, panelHeight, 0.15f, 0.16f, 0.18f, 0.98f);

        if (menuSystem.isOptions()) {
            renderOptionsMenu(panelX, panelY, panelWidth);
        } else {
            renderPauseMenu(panelX, panelY, panelWidth);
        }
    }

    private void renderPauseMenu(final float panelX, final float panelY, final float panelWidth) {
        textRenderer.drawCenteredBoldText(localization.translate("ui.menu.title"), panelX + panelWidth * 0.5f, panelY + 24.0f, 24, TEXT_COLOR);
        for (final MenuButton button : menuButtonsForPause(panelX, panelY, panelWidth)) {
            drawMenuButton(button, labelFor(button.action()));
        }
    }

    private void renderOptionsMenu(final float panelX, final float panelY, final float panelWidth) {
        textRenderer.drawCenteredBoldText(localization.translate("ui.options.title"), panelX + panelWidth * 0.5f, panelY + 22.0f, 23, TEXT_COLOR);
        textRenderer.drawText(localization.translate("ui.options.sensitivity"), panelX + 38.0f, panelY + 84.0f, 15, MUTED_TEXT_COLOR);
        textRenderer.drawCenteredBoldText(String.format(Locale.ROOT, "%.2f", player.getMouseSensitivity()), panelX + panelWidth * 0.5f, panelY + 112.0f, 18, TEXT_COLOR);
        textRenderer.drawText(localization.translate("ui.options.music_volume"), panelX + 38.0f, panelY + 154.0f, 15, MUTED_TEXT_COLOR);
        textRenderer.drawCenteredBoldText(percent(musicManager.getVolume()), panelX + panelWidth * 0.5f, panelY + 182.0f, 18, TEXT_COLOR);
        textRenderer.drawText(localization.translate("ui.options.effects_volume"), panelX + 38.0f, panelY + 224.0f, 15, MUTED_TEXT_COLOR);
        textRenderer.drawCenteredBoldText(percent(soundEffectManager.getVolume()), panelX + panelWidth * 0.5f, panelY + 252.0f, 18, TEXT_COLOR);
        textRenderer.drawText(localization.translate("ui.options.language"), panelX + 38.0f, panelY + 294.0f, 15, MUTED_TEXT_COLOR);
        for (final MenuButton button : menuButtonsForOptions(panelX, panelY, panelWidth)) {
            drawMenuButton(button, labelFor(button.action()));
        }
    }

    private void drawMenuButton(final MenuButton button, final String label) {
        drawColoredQuad(button.x(), button.y(), button.width(), button.height(), 0.08f, 0.09f, 0.105f, 1.0f);
        drawColoredQuad(button.x() + 2.0f, button.y() + 2.0f, button.width() - 4.0f, button.height() - 4.0f, 0.23f, 0.25f, 0.28f, 1.0f);
        textRenderer.drawCenteredBoldText(label, button.x() + button.width() * 0.5f, button.y() + 9.0f, 15, TEXT_COLOR);
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
            case LANGUAGE_TOGGLE -> localization.currentLanguageName();
            case NONE -> "";
        };
    }

    private MenuButton[] menuButtons(final int width, final int height) {
        final float panelWidth = menuSystem.isOptions() ? 460.0f : 360.0f;
        final float panelHeight = menuSystem.isOptions() ? 430.0f : 260.0f;
        final float panelX = (width - panelWidth) * 0.5f;
        final float panelY = (height - panelHeight) * 0.44f;
        return menuSystem.isOptions()
                ? menuButtonsForOptions(panelX, panelY, panelWidth)
                : menuButtonsForPause(panelX, panelY, panelWidth);
    }

    private MenuButton[] menuButtonsForPause(final float panelX, final float panelY, final float panelWidth) {
        final float buttonWidth = 260.0f;
        final float buttonX = panelX + (panelWidth - buttonWidth) * 0.5f;
        return new MenuButton[]{
                new MenuButton(MenuAction.RESUME, buttonX, panelY + 82.0f, buttonWidth, 40.0f),
                new MenuButton(MenuAction.OPTIONS, buttonX, panelY + 136.0f, buttonWidth, 40.0f),
                new MenuButton(MenuAction.EXIT, buttonX, panelY + 190.0f, buttonWidth, 40.0f)
        };
    }

    private MenuButton[] menuButtonsForOptions(final float panelX, final float panelY, final float panelWidth) {
        final float centerX = panelX + panelWidth * 0.5f;
        return new MenuButton[]{
                new MenuButton(MenuAction.SENSITIVITY_DECREASE, centerX - 110.0f, panelY + 104.0f, 42.0f, 36.0f),
                new MenuButton(MenuAction.SENSITIVITY_INCREASE, centerX + 68.0f, panelY + 104.0f, 42.0f, 36.0f),
                new MenuButton(MenuAction.MUSIC_VOLUME_DECREASE, centerX - 110.0f, panelY + 174.0f, 42.0f, 36.0f),
                new MenuButton(MenuAction.MUSIC_VOLUME_INCREASE, centerX + 68.0f, panelY + 174.0f, 42.0f, 36.0f),
                new MenuButton(MenuAction.EFFECTS_VOLUME_DECREASE, centerX - 110.0f, panelY + 244.0f, 42.0f, 36.0f),
                new MenuButton(MenuAction.EFFECTS_VOLUME_INCREASE, centerX + 68.0f, panelY + 244.0f, 42.0f, 36.0f),
                new MenuButton(MenuAction.LANGUAGE_TOGGLE, centerX - 110.0f, panelY + 318.0f, 220.0f, 38.0f),
                new MenuButton(MenuAction.BACK, centerX - 110.0f, panelY + 372.0f, 220.0f, 38.0f)
        };
    }

    private String percent(final float value) {
        return Math.round(value * 100.0f) + "%";
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
        glTexCoord2f(0.0f, 0.0f);
        glVertex2f(x, y);
        glTexCoord2f(1.0f, 0.0f);
        glVertex2f(x + width, y);
        glTexCoord2f(1.0f, 1.0f);
        glVertex2f(x + width, y + height);
        glTexCoord2f(0.0f, 1.0f);
        glVertex2f(x, y + height);
        glEnd();
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
            BufferedImage icon = decodeImage(resourcePackLoader.loadBlockTexture(model.topCandidates()));
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

    private record InventoryLayout(int columns, float panelX, float panelY, float panelWidth, float panelHeight, float slotStartX, float slotStartY) {
    }

    private record MenuButton(MenuAction action, float x, float y, float width, float height) {
    }
}
