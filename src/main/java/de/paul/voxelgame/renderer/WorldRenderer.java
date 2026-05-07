package de.paul.voxelgame.renderer;

import de.paul.voxelgame.GameConfig;
import de.paul.voxelgame.assets.ResourcePackLoader;
import de.paul.voxelgame.core.TextureLoader;
import de.paul.voxelgame.map.Block;
import de.paul.voxelgame.map.World;
import de.paul.voxelgame.mob.Player;
import de.paul.voxelgame.objects.BlockComponent;
import de.paul.voxelgame.objects.GameObject;
import de.paul.voxelgame.objects.ModelComponent;
import de.paul.voxelgame.objects.RegistryManager;
import de.paul.voxelgame.objects.ResourceId;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_COMPILE;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glCallList;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glDeleteLists;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glEndList;
import static org.lwjgl.opengl.GL11.glFrustum;
import static org.lwjgl.opengl.GL11.glGenLists;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glNewList;
import static org.lwjgl.opengl.GL11.glRotatef;
import static org.lwjgl.opengl.GL11.glTexCoord2f;
import static org.lwjgl.opengl.GL11.glTranslatef;
import static org.lwjgl.opengl.GL11.glVertex3f;

public class WorldRenderer {
    private static final ResourceId DIRT_ID = ResourceId.of("game:dirt");
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
    private static final int MATERIAL_VARIANT_BUCKETS = Math.max(1, Integer.getInteger("voxel.texture.variants", 16));

    private final World world;
    private final RegistryManager registries;
    private final ResourcePackLoader resourcePackLoader = new ResourcePackLoader();
    private final TextureLoader textureLoader = new TextureLoader();
    private final Map<ResourceId, BlockTextures[]> blockTextureVariants = new LinkedHashMap<>();
    private final Map<Integer, Integer> fallbackTextureCache = new HashMap<>();
    private BlockTextures[] fallbackBlockTextures;

    private int displayListId;
    private long builtRevision = -1;

    public WorldRenderer(final World world, final RegistryManager registries) {
        this.world = world;
        this.registries = registries;
        initializeTextures();
    }

    public void render(final Player player, final int width, final int height) {
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
        final Set<Integer> textureIds = new HashSet<>();
        for (final BlockTextures[] variants : blockTextureVariants.values()) {
            if (variants == null) {
                continue;
            }
            for (final BlockTextures textures : variants) {
                if (textures == null) {
                    continue;
                }
                textureIds.add(textures.side());
                textureIds.add(textures.top());
                textureIds.add(textures.bottom());
            }
        }
        for (final Integer textureId : textureIds) {
            textureLoader.deleteTexture(textureId == null ? 0 : textureId);
        }
        blockTextureVariants.clear();
        fallbackTextureCache.clear();
    }

    private void setupProjection(final double fovDeg, final int width, final int height, final double nearClip, final double farClip) {
        final double aspect = Math.max(0.0001, (double) width / (double) height);
        final double top = nearClip * Math.tan(Math.toRadians(fovDeg * 0.5));
        final double right = top * aspect;

        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glFrustum(-right, right, -top, top, nearClip, farClip);
        glMatrixMode(GL_MODELVIEW);
    }

    private void setupCamera(final Player player) {
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

    private void renderBlock(final Block block) {
        if (block == null) {
            return;
        }

        final int x = block.getWorldX();
        final int y = block.getWorldY();
        final int z = block.getWorldZ();
        final float size = GameConfig.BLOCK_SIZE;

        final float minX = x * size;
        final float minY = y * size;
        final float minZ = z * size;
        final float maxX = minX + size;
        final float maxY = minY + size;
        final float maxZ = minZ + size;

        final BlockTextures[] variants = blockTextureVariants.getOrDefault(block.getTypeId(), fallbackBlockTextures);
        if (variants == null || variants.length == 0) {
            return;
        }
        final int variantIndex = computeMaterialVariant(block.getTypeId(), x, y, z, variants.length);
        final BlockTextures textures = variants[variantIndex];

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
            final int textureId,
            final float shade,
            final float x1, final float y1, final float z1,
            final float x2, final float y2, final float z2,
            final float x3, final float y3, final float z3,
            final float x4, final float y4, final float z4
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

    private boolean isFaceVisible(final int neighborX, final int neighborY, final int neighborZ) {
        final Block neighbor = world.getBlock(neighborX, neighborY, neighborZ);
        return neighbor == null || !neighbor.isSolid();
    }

    private void initializeTextures() {
        final Color grassTint = resolveGrassTint();
        for (final GameObject type : registries.blocks().values()) {
            if (!type.has(BlockComponent.class)) {
                continue;
            }

            final ModelComponent model = type.has(ModelComponent.class)
                    ? type.get(ModelComponent.class)
                    : new ModelComponent(type.id().path());
            final int variantCount = resolveVariantCount(model);
            final BlockTextures[] variants = new BlockTextures[variantCount];
            for (int variant = 0; variant < variantCount; variant++) {
                BufferedImage side = loadBlockImageForVariant(model.sideCandidates(), variant);
                BufferedImage top = loadBlockImageForVariant(model.topCandidates(), variant);
                BufferedImage bottom = loadBlockImageForVariant(model.bottomCandidates(), variant);

                if (model.hasTint("grass")) {
                    if (top != null) {
                        top = multiplyTint(top, grassTint);
                    }

                    final BufferedImage sideOverlay = loadBlockImageForVariant(GRASS_SIDE_OVERLAY_CANDIDATES, variant);
                    if (side != null) {
                        if (sideOverlay != null) {
                            side = alphaOverlay(side, multiplyTint(sideOverlay, grassTint));
                        } else {
                            side = multiplyTint(side, grassTint);
                        }
                    }
                }

                final int sideId = createTextureFromImageOrFallback(side, fallbackColor(type, model, Face.SIDE));
                final int topId = createTextureFromImageOrFallback(top, fallbackColor(type, model, Face.TOP));
                final int bottomId = createTextureFromImageOrFallback(bottom, fallbackColor(type, model, Face.BOTTOM));
                variants[variant] = new BlockTextures(sideId, topId, bottomId);
            }
            blockTextureVariants.put(type.id(), variants);
            if (fallbackBlockTextures == null || DIRT_ID.equals(type.id())) {
                fallbackBlockTextures = variants;
            }
        }
    }

    private BufferedImage loadBlockImageForVariant(final String[] candidates, final int variant) {
        final String[] ordered = rotateCandidates(candidates, variant);
        final byte[] data = resourcePackLoader.loadBlockTexture(ordered);
        return decodeImage(data);
    }

    private String[] rotateCandidates(final String[] candidates, final int variant) {
        if (candidates == null || candidates.length == 0 || candidates.length == 1 || !hasIndexedVariantCandidate(candidates)) {
            return candidates;
        }
        final int start = Math.floorMod(variant, candidates.length);
        final String[] ordered = new String[candidates.length];
        for (int i = 0; i < candidates.length; i++) {
            ordered[i] = candidates[(start + i) % candidates.length];
        }
        return ordered;
    }

    private boolean hasIndexedVariantCandidate(final String[] candidates) {
        if (candidates == null || candidates.length == 0) {
            return false;
        }
        for (final String candidate : candidates) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            final char last = candidate.charAt(candidate.length() - 1);
            if (last >= '0' && last <= '9') {
                return true;
            }
        }
        return false;
    }

    private int resolveVariantCount(final ModelComponent model) {
        int max = 1;
        if (hasIndexedVariantCandidate(model.sideCandidates())) {
            max = Math.max(max, model.sideCandidates().length);
        }
        if (hasIndexedVariantCandidate(model.topCandidates())) {
            max = Math.max(max, model.topCandidates().length);
        }
        if (hasIndexedVariantCandidate(model.bottomCandidates())) {
            max = Math.max(max, model.bottomCandidates().length);
        }
        if (model.hasTint("grass") && hasIndexedVariantCandidate(GRASS_SIDE_OVERLAY_CANDIDATES)) {
            max = Math.max(max, GRASS_SIDE_OVERLAY_CANDIDATES.length);
        }
        return Math.max(1, Math.min(max, MATERIAL_VARIANT_BUCKETS));
    }

    private int computeMaterialVariant(final ResourceId typeId, final int worldX, final int worldY, final int worldZ, final int variantCount) {
        int hash = 17;
        hash = 31 * hash + typeId.hashCode();
        hash = 31 * hash + worldX;
        hash = 31 * hash + (worldY * 7);
        hash = 31 * hash + worldZ;
        hash ^= (worldX << 11);
        hash ^= (worldZ >>> 3);
        return Math.floorMod(hash, Math.max(1, variantCount));
    }

    private Color resolveGrassTint() {
        final byte[] colorMapData = resourcePackLoader.loadColorMapTexture("grass");
        final BufferedImage colorMap = decodeImage(colorMapData);
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

    private int createTextureFromImageOrFallback(final BufferedImage image, final int fallbackRgba) {
        if (image == null) {
            return fallbackTexture(fallbackRgba);
        }
        return uploadTexture(image);
    }

    private int fallbackTexture(final int rgba) {
        final Integer cached = fallbackTextureCache.get(rgba);
        if (cached != null) {
            return cached;
        }

        final int textureId = textureLoader.loadSolidTexture(rgba);
        fallbackTextureCache.put(rgba, textureId);
        return textureId;
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

    private BufferedImage alphaOverlay(final BufferedImage base, final BufferedImage overlay) {
        final int width = Math.min(base.getWidth(), overlay.getWidth());
        final int height = Math.min(base.getHeight(), overlay.getHeight());
        final BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int bArgb = base.getRGB(x, y);
                final int oArgb = overlay.getRGB(x, y);

                final float br = ((bArgb >>> 16) & 0xff) / 255.0f;
                final float bg = ((bArgb >>> 8) & 0xff) / 255.0f;
                final float bb = (bArgb & 0xff) / 255.0f;

                final float or = ((oArgb >>> 16) & 0xff) / 255.0f;
                final float og = ((oArgb >>> 8) & 0xff) / 255.0f;
                final float ob = (oArgb & 0xff) / 255.0f;
                final float oa = ((oArgb >>> 24) & 0xff) / 255.0f;

                final int rr = clampInt(Math.round((br * (1.0f - oa) + or * oa) * 255.0f), 0, 255);
                final int gg = clampInt(Math.round((bg * (1.0f - oa) + og * oa) * 255.0f), 0, 255);
                final int bbOut = clampInt(Math.round((bb * (1.0f - oa) + ob * oa) * 255.0f), 0, 255);
                out.setRGB(x, y, (0xff << 24) | (rr << 16) | (gg << 8) | bbOut);
            }
        }
        return out;
    }

    private int uploadTexture(final BufferedImage image) {
        return textureLoader.uploadTexture(image);
    }

    private int fallbackColor(final GameObject type, final ModelComponent model, final Face face) {
        final int defaultColor = switch (type.id().path()) {
            case "grass" -> switch (face) {
                case SIDE -> rgba(110, 157, 74, 255);
                case TOP -> rgba(99, 178, 67, 255);
                case BOTTOM -> rgba(138, 90, 51, 255);
            };
            case "dirt" -> rgba(138, 90, 51, 255);
            case "stone" -> rgba(123, 123, 123, 255);
            case "bedrock" -> rgba(61, 61, 61, 255);
            case "water" -> rgba(62, 128, 216, 200);
            case "wood" -> rgba(122, 74, 26, 255);
            default -> rgba(185, 82, 124, 255);
        };
        return model.fallbackColor(face.key, defaultColor);
    }

    private int rgba(final int r, final int g, final int b, final int a) {
        return ((r & 0xff) << 24) | ((g & 0xff) << 16) | ((b & 0xff) << 8) | (a & 0xff);
    }

    private void setShade(final float shade) {
        final float s = clampFloat(shade, 0.0f, 1.0f);
        glColor3f(s, s, s);
    }

    private float clampFloat(final float value, final float min, final float max) {
        return Math.max(min, Math.min(max, value));
    }

    private int clampInt(final int value, final int min, final int max) {
        return Math.max(min, Math.min(max, value));
    }

    private enum Face {
        SIDE("side"),
        TOP("top"),
        BOTTOM("bottom");

        private final String key;

        Face(final String key) {
            this.key = key;
        }
    }

    private record BlockTextures(int side, int top, int bottom) {
    }
}
