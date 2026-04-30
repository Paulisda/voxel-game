package de.paul.voxelgame.renderer;

import de.paul.voxelgame.assets.ResourcePackLoader;
import de.paul.voxelgame.map.BlockType;
import de.paul.voxelgame.mob.Player;
import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glIsEnabled;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glOrtho;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;
import static org.lwjgl.opengl.GL11.glTexCoord2f;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL11.glVertex2f;

public class HudRenderer {
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

    private static final Color DEFAULT_GRASS_TINT = new Color(0x7F, 0xB2, 0x38);

    private final Player player;
    private final ResourcePackLoader resourcePackLoader = new ResourcePackLoader();
    private final Map<BlockType, Integer> blockIcons = new EnumMap<>(BlockType.class);

    private int hotbarBackgroundTexture;
    private int hotbarSelectorTexture;
    private int heartEmptyTexture;
    private int heartHalfTexture;
    private int heartFullTexture;
    private int hungerEmptyTexture;
    private int hungerHalfTexture;
    private int hungerFullTexture;

    public HudRenderer(Player player) {
        this.player = player;
        loadHudTextures();
        loadHotbarItemIcons();
    }

    public void render(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }

        boolean cullWasEnabled = glIsEnabled(GL_CULL_FACE);

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

        float hotbarWidth = HOTBAR_W * HUD_SCALE;
        float hotbarHeight = HOTBAR_H * HUD_SCALE;
        float hotbarX = (width - hotbarWidth) * 0.5f;
        float hotbarY = height - hotbarHeight - (12.0f * HUD_SCALE);

        drawTexturedQuad(hotbarBackgroundTexture, hotbarX, hotbarY, hotbarWidth, hotbarHeight);

        int selected = player.getSelectedHotbarSlot();
        float selectorX = hotbarX + ((selected * 20.0f) - 1.0f) * HUD_SCALE;
        float selectorY = hotbarY - HUD_SCALE;
        drawTexturedQuad(hotbarSelectorTexture, selectorX, selectorY, SLOT_SELECTOR_W * HUD_SCALE, SLOT_SELECTOR_H * HUD_SCALE);

        float iconSize = 16.0f * HUD_SCALE;
        for (int i = 0; i < HUD_SLOT_COUNT; i++) {
            BlockType type = player.getHotbarBlock(i);
            int iconTexture = blockIcons.getOrDefault(type, blockIcons.get(BlockType.DIRT));
            float iconX = hotbarX + (3 + i * 20) * HUD_SCALE;
            float iconY = hotbarY + 3.0f * HUD_SCALE;
            drawTexturedQuad(iconTexture, iconX, iconY, iconSize, iconSize);
        }

        float pipSize = ICON_W * HUD_SCALE;
        float pipStep = 8.0f * HUD_SCALE;
        float statsY = hotbarY - pipSize - (4.0f * HUD_SCALE);
        float heartsX = hotbarX;
        float hungerX = hotbarX + hotbarWidth - (pipSize + pipStep * (STAT_PIP_COUNT - 1));

        drawStatsRow(player.getHealthPoints(), heartsX, statsY, pipStep, pipSize, heartEmptyTexture, heartHalfTexture, heartFullTexture);
        drawStatsRow(player.getHungerPoints(), hungerX, statsY, pipStep, pipSize, hungerEmptyTexture, hungerHalfTexture, hungerFullTexture);

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
        Set<Integer> textureIds = new HashSet<>();
        textureIds.add(hotbarBackgroundTexture);
        textureIds.add(hotbarSelectorTexture);
        textureIds.add(heartEmptyTexture);
        textureIds.add(heartHalfTexture);
        textureIds.add(heartFullTexture);
        textureIds.add(hungerEmptyTexture);
        textureIds.add(hungerHalfTexture);
        textureIds.add(hungerFullTexture);
        textureIds.addAll(blockIcons.values());

        for (Integer textureId : textureIds) {
            if (textureId != null && textureId > 0) {
                glDeleteTextures(textureId);
            }
        }
        blockIcons.clear();
    }

    private void drawStatsRow(int value, float startX, float y, float step, float size, int empty, int half, int full) {
        for (int i = 0; i < STAT_PIP_COUNT; i++) {
            int required = (i + 1) * 2;
            int texture = value >= required ? full : (value == required - 1 ? half : empty);
            drawTexturedQuad(texture, startX + i * step, y, size, size);
        }
    }

    private void drawTexturedQuad(int textureId, float x, float y, float width, float height) {
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

    private void loadHudTextures() {
        BufferedImage widgetsAtlas = decodeImage(resourcePackLoader.loadGuiTexture("widgets"));
        BufferedImage iconsAtlas = decodeImage(resourcePackLoader.loadGuiTexture("icons"));

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

    private int loadGuiSprite(BufferedImage atlas, int u, int v, int w, int h, Color fallbackColor) {
        if (atlas == null) {
            return uploadTexture(createSolidImage(w, h, fallbackColor));
        }

        double scaleX = atlas.getWidth() / 256.0;
        double scaleY = atlas.getHeight() / 256.0;

        int sx = clampInt((int) Math.round(u * scaleX), 0, Math.max(0, atlas.getWidth() - 1));
        int sy = clampInt((int) Math.round(v * scaleY), 0, Math.max(0, atlas.getHeight() - 1));
        int sw = Math.max(1, (int) Math.round(w * scaleX));
        int sh = Math.max(1, (int) Math.round(h * scaleY));

        if (sx + sw > atlas.getWidth()) {
            sw = Math.max(1, atlas.getWidth() - sx);
        }
        if (sy + sh > atlas.getHeight()) {
            sh = Math.max(1, atlas.getHeight() - sy);
        }

        BufferedImage cropped = copyRegion(atlas, sx, sy, sw, sh);
        return uploadTexture(cropped);
    }

    private void loadHotbarItemIcons() {
        Color grassTint = resolveGrassTint();
        for (BlockType type : BlockType.values()) {
            BufferedImage icon = decodeImage(resourcePackLoader.loadBlockTexture(type.getTopTextureCandidates()));
            if (icon == null) {
                icon = decodeImage(resourcePackLoader.loadBlockTexture(type.getSideTextureCandidates()));
            }
            if (icon == null) {
                icon = createSolidImage(16, 16, fallbackColor(type));
            } else if (type == BlockType.GRASS) {
                icon = multiplyTint(icon, grassTint);
            }
            blockIcons.put(type, uploadTexture(icon));
        }
    }

    private BufferedImage decodeImage(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        try {
            return ImageIO.read(new ByteArrayInputStream(data));
        } catch (IOException e) {
            return null;
        }
    }

    private BufferedImage copyRegion(BufferedImage image, int x, int y, int width, int height) {
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int iy = 0; iy < height; iy++) {
            for (int ix = 0; ix < width; ix++) {
                out.setRGB(ix, iy, image.getRGB(x + ix, y + iy));
            }
        }
        return out;
    }

    private BufferedImage createSolidImage(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int argb = color.getRGB();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, argb);
            }
        }
        return image;
    }

    private BufferedImage multiplyTint(BufferedImage source, Color tint) {
        BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        float tr = tint.getRed() / 255.0f;
        float tg = tint.getGreen() / 255.0f;
        float tb = tint.getBlue() / 255.0f;

        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int argb = source.getRGB(x, y);
                int a = (argb >>> 24) & 0xff;
                int r = (argb >>> 16) & 0xff;
                int g = (argb >>> 8) & 0xff;
                int b = argb & 0xff;

                int rr = clampInt(Math.round(r * tr), 0, 255);
                int gg = clampInt(Math.round(g * tg), 0, 255);
                int bb = clampInt(Math.round(b * tb), 0, 255);
                out.setRGB(x, y, (a << 24) | (rr << 16) | (gg << 8) | bb);
            }
        }
        return out;
    }

    private Color resolveGrassTint() {
        BufferedImage colorMap = decodeImage(resourcePackLoader.loadColorMapTexture("grass"));
        if (colorMap == null) {
            return DEFAULT_GRASS_TINT;
        }

        double temperature = 0.8;
        double rainfall = 0.4;
        rainfall *= temperature;

        int colorX = clampInt((int) ((1.0 - temperature) * 255.0), 0, 255);
        int colorY = clampInt((int) ((1.0 - rainfall) * 255.0), 0, 255);

        int sampleX = clampInt((int) Math.round((colorX / 255.0) * (colorMap.getWidth() - 1)), 0, colorMap.getWidth() - 1);
        int sampleY = clampInt((int) Math.round((colorY / 255.0) * (colorMap.getHeight() - 1)), 0, colorMap.getHeight() - 1);
        return new Color(colorMap.getRGB(sampleX, sampleY), true);
    }

    private Color fallbackColor(BlockType type) {
        return switch (type) {
            case GRASS -> new Color(118, 190, 74, 255);
            case DIRT -> new Color(138, 90, 51, 255);
            case STONE -> new Color(123, 123, 123, 255);
            case BEDROCK -> new Color(65, 65, 65, 255);
            case WATER -> new Color(62, 128, 216, 220);
            case WOOD -> new Color(122, 74, 26, 255);
        };
    }

    private int uploadTexture(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] argb = new int[width * height];
        image.getRGB(0, 0, width, height, argb, 0, width);

        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
        for (int y = height - 1; y >= 0; y--) {
            for (int x = 0; x < width; x++) {
                int pixel = argb[y * width + x];
                buffer.put((byte) ((pixel >>> 16) & 0xff));
                buffer.put((byte) ((pixel >>> 8) & 0xff));
                buffer.put((byte) (pixel & 0xff));
                buffer.put((byte) ((pixel >>> 24) & 0xff));
            }
        }
        buffer.flip();

        int textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        return textureId;
    }

    private int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
