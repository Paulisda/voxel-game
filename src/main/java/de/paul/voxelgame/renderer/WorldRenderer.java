package de.paul.voxelgame.renderer;

import de.paul.voxelgame.GameConfig;
import de.paul.voxelgame.assets.ResourcePackLoader;
import de.paul.voxelgame.map.Block;
import de.paul.voxelgame.map.BlockType;
import de.paul.voxelgame.map.World;
import de.paul.voxelgame.mob.Player;
import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_COMPILE;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
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
import static org.lwjgl.opengl.GL11.glCallList;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glDeleteLists;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glEndList;
import static org.lwjgl.opengl.GL11.glFrustum;
import static org.lwjgl.opengl.GL11.glGenLists;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glNewList;
import static org.lwjgl.opengl.GL11.glRotatef;
import static org.lwjgl.opengl.GL11.glTexCoord2f;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL11.glTranslatef;
import static org.lwjgl.opengl.GL11.glVertex3f;

public class WorldRenderer {
    private static final String[] GRASS_SIDE_OVERLAY_CANDIDATES = {
            "grass_block_side_overlay",
            "grass_block_side_overlay0",
            "grass_block_side_overlay1",
            "grass_block_side_overlay2",
            "grass_block_side_overlay3",
            "grass_block_side_overlay4",
            "grass_side_overlay"
    };
    private static final Color DEFAULT_GRASS_TINT = new Color(0x7F, 0xB2, 0x38);

    private final World world;
    private final ResourcePackLoader resourcePackLoader = new ResourcePackLoader();
    private final Map<BlockType, BlockTextures> blockTextures = new EnumMap<>(BlockType.class);
    private final Map<Integer, Integer> fallbackTextureCache = new HashMap<>();

    private int displayListId;
    private long builtRevision = -1;

    public WorldRenderer(World world) {
        this.world = world;
        initializeTextures();
    }

    public void render(Player player, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }

        setupProjection(player.getFieldOfView(), width, height, 0.05, 1200.0);
        setupCamera(player);

        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        rebuildDisplayListIfNeeded();
        glCallList(displayListId);

        glDisable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);
    }

    public void destroy() {
        if (displayListId != 0) {
            glDeleteLists(displayListId, 1);
            displayListId = 0;
        }
        for (BlockTextures textures : blockTextures.values()) {
            glDeleteTextures(textures.side());
            glDeleteTextures(textures.top());
            glDeleteTextures(textures.bottom());
        }
        blockTextures.clear();
        fallbackTextureCache.clear();
    }

    private void setupProjection(double fovDeg, int width, int height, double nearClip, double farClip) {
        double aspect = Math.max(0.0001, (double) width / (double) height);
        double top = nearClip * Math.tan(Math.toRadians(fovDeg * 0.5));
        double right = top * aspect;

        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glFrustum(-right, right, -top, top, nearClip, farClip);
        glMatrixMode(GL_MODELVIEW);
    }

    private void setupCamera(Player player) {
        glLoadIdentity();
        glRotatef((float) -player.getPitchDegrees(), 1.0f, 0.0f, 0.0f);
        glRotatef((float) -player.getYawDegrees(), 0.0f, 1.0f, 0.0f);
        glTranslatef((float) -player.getCameraX(), (float) -player.getCameraY(), (float) -player.getCameraZ());
    }

    private void rebuildDisplayListIfNeeded() {
        if (displayListId == 0) {
            displayListId = glGenLists(1);
        }
        if (builtRevision == world.getRevision()) {
            return;
        }

        glNewList(displayListId, GL_COMPILE);
        glDisable(GL_CULL_FACE);
        world.forEachBlock(this::renderBlock);
        glEnable(GL_CULL_FACE);
        glEndList();

        builtRevision = world.getRevision();
    }

    private void renderBlock(Block block) {
        if (block == null || !block.isSolid()) {
            return;
        }

        int x = block.getWorldX();
        int y = block.getWorldY();
        int z = block.getWorldZ();
        float size = GameConfig.BLOCK_SIZE;

        float minX = x * size;
        float minY = y * size;
        float minZ = z * size;
        float maxX = minX + size;
        float maxY = minY + size;
        float maxZ = minZ + size;

        BlockTextures textures = blockTextures.getOrDefault(block.getType(), blockTextures.get(BlockType.DIRT));

        if (isFaceVisible(x, y, z + 1)) {
            drawFace(textures.side(), 0.88f, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ);
        }
        if (isFaceVisible(x, y, z - 1)) {
            drawFace(textures.side(), 0.88f, maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ);
        }
        if (isFaceVisible(x + 1, y, z)) {
            drawFace(textures.side(), 0.83f, maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ);
        }
        if (isFaceVisible(x - 1, y, z)) {
            drawFace(textures.side(), 0.83f, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ);
        }
        if (isFaceVisible(x, y + 1, z)) {
            drawFace(textures.top(), 1.0f, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ);
        }
        if (isFaceVisible(x, y - 1, z)) {
            drawFace(textures.bottom(), 0.72f, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ);
        }
    }

    private void drawFace(
            int textureId,
            float shade,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float x4, float y4, float z4
    ) {
        glBindTexture(GL_TEXTURE_2D, textureId);
        setShade(shade);
        glBegin(GL_QUADS);
        glTexCoord2f(0.0f, 0.0f);
        glVertex3f(x1, y1, z1);
        glTexCoord2f(1.0f, 0.0f);
        glVertex3f(x2, y2, z2);
        glTexCoord2f(1.0f, 1.0f);
        glVertex3f(x3, y3, z3);
        glTexCoord2f(0.0f, 1.0f);
        glVertex3f(x4, y4, z4);
        glEnd();
    }

    private boolean isFaceVisible(int neighborX, int neighborY, int neighborZ) {
        Block neighbor = world.getBlock(neighborX, neighborY, neighborZ);
        return neighbor == null || !neighbor.isSolid();
    }

    private void initializeTextures() {
        Color grassTint = resolveGrassTint();
        for (BlockType type : BlockType.values()) {
            BufferedImage side = loadBlockImage(type.getSideTextureCandidates());
            BufferedImage top = loadBlockImage(type.getTopTextureCandidates());
            BufferedImage bottom = loadBlockImage(type.getBottomTextureCandidates());

            if (type == BlockType.GRASS) {
                if (top != null) {
                    top = multiplyTint(top, grassTint);
                }

                BufferedImage sideOverlay = loadBlockImage(GRASS_SIDE_OVERLAY_CANDIDATES);
                if (side != null) {
                    if (sideOverlay != null) {
                        side = alphaOverlay(side, multiplyTint(sideOverlay, grassTint));
                    } else {
                        side = multiplyTint(side, grassTint);
                    }
                }
            }

            int sideId = createTextureFromImageOrFallback(side, fallbackColor(type, Face.SIDE));
            int topId = createTextureFromImageOrFallback(top, fallbackColor(type, Face.TOP));
            int bottomId = createTextureFromImageOrFallback(bottom, fallbackColor(type, Face.BOTTOM));
            blockTextures.put(type, new BlockTextures(sideId, topId, bottomId));
        }
    }

    private BufferedImage loadBlockImage(String... candidates) {
        byte[] data = resourcePackLoader.loadBlockTexture(candidates);
        return decodeImage(data);
    }

    private Color resolveGrassTint() {
        byte[] colorMapData = resourcePackLoader.loadColorMapTexture("grass");
        BufferedImage colorMap = decodeImage(colorMapData);
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

    private int createTextureFromImageOrFallback(BufferedImage image, int fallbackRgba) {
        if (image == null) {
            return fallbackTexture(fallbackRgba);
        }
        return uploadTexture(image);
    }

    private int fallbackTexture(int rgba) {
        Integer cached = fallbackTextureCache.get(rgba);
        if (cached != null) {
            return cached;
        }

        ByteBuffer buffer = BufferUtils.createByteBuffer(4);
        buffer.put((byte) ((rgba >>> 24) & 0xff));
        buffer.put((byte) ((rgba >>> 16) & 0xff));
        buffer.put((byte) ((rgba >>> 8) & 0xff));
        buffer.put((byte) (rgba & 0xff));
        buffer.flip();

        int textureId = uploadTexture(1, 1, buffer);
        fallbackTextureCache.put(rgba, textureId);
        return textureId;
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

    private BufferedImage alphaOverlay(BufferedImage base, BufferedImage overlay) {
        int width = Math.min(base.getWidth(), overlay.getWidth());
        int height = Math.min(base.getHeight(), overlay.getHeight());
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int bArgb = base.getRGB(x, y);
                int oArgb = overlay.getRGB(x, y);

                float br = ((bArgb >>> 16) & 0xff) / 255.0f;
                float bg = ((bArgb >>> 8) & 0xff) / 255.0f;
                float bb = (bArgb & 0xff) / 255.0f;

                float or = ((oArgb >>> 16) & 0xff) / 255.0f;
                float og = ((oArgb >>> 8) & 0xff) / 255.0f;
                float ob = (oArgb & 0xff) / 255.0f;
                float oa = ((oArgb >>> 24) & 0xff) / 255.0f;

                int rr = clampInt(Math.round((br * (1.0f - oa) + or * oa) * 255.0f), 0, 255);
                int gg = clampInt(Math.round((bg * (1.0f - oa) + og * oa) * 255.0f), 0, 255);
                int bbOut = clampInt(Math.round((bb * (1.0f - oa) + ob * oa) * 255.0f), 0, 255);
                out.setRGB(x, y, (0xff << 24) | (rr << 16) | (gg << 8) | bbOut);
            }
        }
        return out;
    }

    private int uploadTexture(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] argb = new int[width * height];
        image.getRGB(0, 0, width, height, argb, 0, width);

        ByteBuffer rgba = BufferUtils.createByteBuffer(width * height * 4);
        for (int y = height - 1; y >= 0; y--) {
            for (int x = 0; x < width; x++) {
                int pixel = argb[y * width + x];
                rgba.put((byte) ((pixel >>> 16) & 0xff));
                rgba.put((byte) ((pixel >>> 8) & 0xff));
                rgba.put((byte) (pixel & 0xff));
                rgba.put((byte) ((pixel >>> 24) & 0xff));
            }
        }
        rgba.flip();

        return uploadTexture(width, height, rgba);
    }

    private int uploadTexture(int width, int height, ByteBuffer pixels) {
        int textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
        return textureId;
    }

    private int fallbackColor(BlockType type, Face face) {
        return switch (type) {
            case GRASS -> switch (face) {
                case SIDE -> rgba(110, 157, 74, 255);
                case TOP -> rgba(99, 178, 67, 255);
                case BOTTOM -> rgba(138, 90, 51, 255);
            };
            case DIRT -> rgba(138, 90, 51, 255);
            case STONE -> rgba(123, 123, 123, 255);
            case BEDROCK -> rgba(61, 61, 61, 255);
            case WATER -> rgba(62, 128, 216, 200);
            case WOOD -> rgba(122, 74, 26, 255);
        };
    }

    private int rgba(int r, int g, int b, int a) {
        return ((r & 0xff) << 24) | ((g & 0xff) << 16) | ((b & 0xff) << 8) | (a & 0xff);
    }

    private void setShade(float shade) {
        float s = clampFloat(shade, 0.0f, 1.0f);
        glColor3f(s, s, s);
    }

    private float clampFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private enum Face {
        SIDE,
        TOP,
        BOTTOM
    }

    private record BlockTextures(int side, int top, int bottom) {
    }
}
