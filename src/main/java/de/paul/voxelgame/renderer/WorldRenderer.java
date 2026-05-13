package de.paul.voxelgame.renderer;

import de.paul.voxelgame.GameConfig;
import de.paul.voxelgame.assets.ResourcePackLoader;
import de.paul.voxelgame.core.EnvironmentSystem;
import de.paul.voxelgame.core.JsonParser;
import de.paul.voxelgame.core.TextureLoader;
import de.paul.voxelgame.map.Block;
import de.paul.voxelgame.map.BlockFacing;
import de.paul.voxelgame.map.Chunk;
import de.paul.voxelgame.map.World;
import de.paul.voxelgame.mob.Player;
import de.paul.voxelgame.objects.BlockComponent;
import de.paul.voxelgame.objects.GameObject;
import de.paul.voxelgame.objects.ModelComponent;
import de.paul.voxelgame.objects.RegistryManager;
import de.paul.voxelgame.objects.ResourceId;
import org.lwjgl.BufferUtils;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.lwjgl.opengl.GL11.GL_ALPHA_TEST;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_COLOR_ARRAY;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_DIFFUSE;
import static org.lwjgl.opengl.GL11.GL_AMBIENT;
import static org.lwjgl.opengl.GL11.GL_AMBIENT_AND_DIFFUSE;
import static org.lwjgl.opengl.GL11.GL_COLOR_MATERIAL;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_FRONT_AND_BACK;
import static org.lwjgl.opengl.GL11.GL_GREATER;
import static org.lwjgl.opengl.GL11.GL_LEQUAL;
import static org.lwjgl.opengl.GL11.GL_LESS;
import static org.lwjgl.opengl.GL11.GL_LIGHT0;
import static org.lwjgl.opengl.GL11.GL_LIGHT1;
import static org.lwjgl.opengl.GL11.GL_LIGHTING;
import static org.lwjgl.opengl.GL11.GL_LIGHT_MODEL_AMBIENT;
import static org.lwjgl.opengl.GL11.GL_LIGHT_MODEL_TWO_SIDE;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_POSITION;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.GL_SPECULAR;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_COORD_ARRAY;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_TRUE;
import static org.lwjgl.opengl.GL11.GL_VERTEX_ARRAY;
import static org.lwjgl.opengl.GL11.GL_NORMAL_ARRAY;
import static org.lwjgl.opengl.GL11.glAlphaFunc;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glColorPointer;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glColor4f;
import static org.lwjgl.opengl.GL11.glColorMaterial;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDisableClientState;
import static org.lwjgl.opengl.GL11.glDepthFunc;
import static org.lwjgl.opengl.GL11.glDepthMask;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glEnableClientState;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glFrustum;
import static org.lwjgl.opengl.GL11.glLightModelfv;
import static org.lwjgl.opengl.GL11.glLightModeli;
import static org.lwjgl.opengl.GL11.glLightfv;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glNormal3f;
import static org.lwjgl.opengl.GL11.glNormalPointer;
import static org.lwjgl.opengl.GL11.glOrtho;
import static org.lwjgl.opengl.GL11.glRotatef;
import static org.lwjgl.opengl.GL11.glTexCoord2f;
import static org.lwjgl.opengl.GL11.glTexCoordPointer;
import static org.lwjgl.opengl.GL11.glTranslatef;
import static org.lwjgl.opengl.GL11.glVertex2f;
import static org.lwjgl.opengl.GL11.glVertex3f;
import static org.lwjgl.opengl.GL11.glVertexPointer;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;

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
    private static final Color DEFAULT_WATER_TINT = new Color(62, 128, 216, 200);
    private static final int MATERIAL_VARIANT_BUCKETS = Math.max(1, Integer.getInteger("voxel.texture.variants", 4));
    private static final int MINECRAFT_MODEL_VARIANT_BUCKETS = Math.max(1, Integer.getInteger("voxel.minecraft.model.variants", 64));
    private static final int MAX_EXPANDED_MODEL_WEIGHT = 64;
    private static final int MAX_MODEL_SELECTION_WEIGHT = 65536;
    private static final boolean ENABLE_MINECRAFT_BLOCK_MODELS = Boolean.parseBoolean(System.getProperty("voxel.minecraft.models", "true"));
    private static final int MOON_PHASE_COLUMNS = 4;
    private static final int MOON_PHASE_ROWS = 2;
    private static final float CELESTIAL_DISTANCE = 700.0f;
    private static final float CELESTIAL_SIZE = 64.0f;
    private static final int VERTEX_FLOATS = 11;
    private static final int VERTEX_STRIDE_BYTES = VERTEX_FLOATS * Float.BYTES;
    private static final long NORMAL_OFFSET_BYTES = 3L * Float.BYTES;
    private static final long TEX_COORD_OFFSET_BYTES = 6L * Float.BYTES;
    private static final long COLOR_OFFSET_BYTES = 8L * Float.BYTES;

    private final World world;
    private final RegistryManager registries;
    private final EnvironmentSystem environment;
    private final ResourcePackLoader resourcePackLoader = new ResourcePackLoader();
    private final TextureLoader textureLoader = new TextureLoader();
    private final FloatBuffer lightScratchBuffer = BufferUtils.createFloatBuffer(4);
    private final Map<ResourceId, BlockTextures[]> blockTextureVariants = new LinkedHashMap<>();
    private final Map<BlockModelKey, MinecraftBlockModel[]> minecraftBlockModels = new LinkedHashMap<>();
    private final Map<ResourceId, String> blockShapes = new LinkedHashMap<>();
    private final Map<Long, ChunkMesh> chunkMeshes = new LinkedHashMap<>();
    private final Map<Integer, Integer> fallbackTextureCache = new HashMap<>();
    private final Map<String, Integer> modelTextureCache = new HashMap<>();
    private final Map<String, ResolvedMinecraftModel> resolvedModelCache = new HashMap<>();
    private Color grassTint = DEFAULT_GRASS_TINT;
    private BlockTextures[] fallbackBlockTextures;
    private int sunTexture;
    private int moonTexture;
    private int rainTexture;

    public WorldRenderer(final World world, final RegistryManager registries, final EnvironmentSystem environment) {
        this.world = world;
        this.registries = registries;
        this.environment = environment;
        initializeEnvironmentTextures();
        initializeTextures();
    }

    public void render(final Player player, final int width, final int height) {
        if (width <= 0 || height <= 0) {
            return;
        }

        setupProjection(player.getFieldOfView(), width, height, 0.05, 1200.0);
        setupSkyCamera(player);
        renderSky();
        setupCamera(player);

        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_ALPHA_TEST);
        glAlphaFunc(GL_GREATER, 0.1f);

        setupWorldLighting();
        renderChunks();
        teardownWorldLighting();

        glDisable(GL_ALPHA_TEST);
        glDisable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);

        renderRainOverlay(width, height);
    }

    public void destroy() {
        for (final ChunkMesh mesh : chunkMeshes.values()) {
            mesh.destroy();
        }
        chunkMeshes.clear();

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
                textureIds.add(textures.front());
                textureIds.add(textures.top());
                textureIds.add(textures.bottom());
            }
        }
        textureIds.add(sunTexture);
        textureIds.add(moonTexture);
        textureIds.add(rainTexture);
        textureIds.addAll(modelTextureCache.values());
        for (final Integer textureId : textureIds) {
            textureLoader.deleteTexture(textureId == null ? 0 : textureId);
        }
        blockTextureVariants.clear();
        minecraftBlockModels.clear();
        blockShapes.clear();
        fallbackTextureCache.clear();
        modelTextureCache.clear();
        resolvedModelCache.clear();
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

    private void setupSkyCamera(final Player player) {
        glLoadIdentity();
        glRotatef((float) -player.getPitchDegrees(), 1.0f, 0.0f, 0.0f);
        glRotatef((float) -player.getYawDegrees(), 0.0f, 1.0f, 0.0f);
    }

    private void initializeEnvironmentTextures() {
        sunTexture = loadEnvironmentTexture("sun", new Color(255, 238, 143, 255));
        moonTexture = loadMoonTexture();
        rainTexture = loadEnvironmentTexture("rain", new Color(165, 188, 220, 150));
    }

    private int loadEnvironmentTexture(final String name, final Color fallbackColor) {
        final BufferedImage image = decodeImage(resourcePackLoader.loadEnvironmentTexture(name));
        return uploadTexture(image == null ? createSolidImage(16, 16, fallbackColor) : image);
    }

    private int loadMoonTexture() {
        BufferedImage image = decodeImage(resourcePackLoader.loadEnvironmentTexture("moon_phases"));
        if (image == null) {
            image = createProceduralMoonAtlas(256, 128);
        }
        return uploadTexture(createGlowingMoonAtlas(image));
    }

    private BufferedImage createGlowingMoonAtlas(final BufferedImage source) {
        final BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        final int cellWidth = Math.max(1, source.getWidth() / MOON_PHASE_COLUMNS);
        final int cellHeight = Math.max(1, source.getHeight() / MOON_PHASE_ROWS);

        for (int row = 0; row < MOON_PHASE_ROWS; row++) {
            for (int column = 0; column < MOON_PHASE_COLUMNS; column++) {
                final int startX = column * cellWidth;
                final int startY = row * cellHeight;
                final int endX = column == MOON_PHASE_COLUMNS - 1 ? source.getWidth() : startX + cellWidth;
                final int endY = row == MOON_PHASE_ROWS - 1 ? source.getHeight() : startY + cellHeight;
                final float phaseGlow = moonCellGlowStrength(source, startX, startY, endX, endY);
                final float centerX = (startX + endX) * 0.5f;
                final float centerY = (startY + endY) * 0.5f;
                final float radius = Math.max(1.0f, Math.min(endX - startX, endY - startY) * 0.58f);

                for (int y = startY; y < endY; y++) {
                    for (int x = startX; x < endX; x++) {
                        final float dx = (x + 0.5f - centerX) / radius;
                        final float dy = (y + 0.5f - centerY) / radius;
                        final float falloff = clampFloat(1.0f - (float) Math.sqrt(dx * dx + dy * dy), 0.0f, 1.0f);
                        final int glowAlpha = clampInt(Math.round(88.0f * phaseGlow * falloff * falloff), 0, 255);
                        if (glowAlpha > 0) {
                            blendPixel(out, x, y, 174, 200, 255, glowAlpha);
                        }
                    }
                }
            }
        }

        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                final int argb = source.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xff;
                final int red = (argb >>> 16) & 0xff;
                final int green = (argb >>> 8) & 0xff;
                final int blue = argb & 0xff;
                if (alpha == 255 && luminance(red, green, blue) < 4.0f) {
                    alpha = 0;
                }
                if (alpha > 0) {
                    blendPixel(out, x, y, red, green, blue, alpha);
                }
            }
        }
        return out;
    }

    private float moonCellGlowStrength(final BufferedImage source, final int startX, final int startY, final int endX, final int endY) {
        float total = 0.0f;
        int count = 0;
        for (int y = startY; y < endY; y++) {
            for (int x = startX; x < endX; x++) {
                final int argb = source.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xff;
                final int red = (argb >>> 16) & 0xff;
                final int green = (argb >>> 8) & 0xff;
                final int blue = argb & 0xff;
                final float lum = luminance(red, green, blue);
                if (alpha == 255 && lum < 4.0f) {
                    alpha = 0;
                }
                if (alpha > 8 && lum > 12.0f) {
                    total += (alpha / 255.0f) * (lum / 255.0f);
                    count++;
                }
            }
        }
        if (count == 0) {
            return 0.20f;
        }
        return clampFloat(0.25f + (total / count) * 1.05f, 0.25f, 1.0f);
    }

    private BufferedImage createProceduralMoonAtlas(final int width, final int height) {
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        final int cellWidth = Math.max(1, width / MOON_PHASE_COLUMNS);
        final int cellHeight = Math.max(1, height / MOON_PHASE_ROWS);
        for (int phase = 0; phase < MOON_PHASE_COLUMNS * MOON_PHASE_ROWS; phase++) {
            final int column = phase % MOON_PHASE_COLUMNS;
            final int row = phase / MOON_PHASE_COLUMNS;
            final float centerX = column * cellWidth + cellWidth * 0.5f;
            final float centerY = row * cellHeight + cellHeight * 0.5f;
            final float radius = Math.min(cellWidth, cellHeight) * 0.28f;
            final float litOffset = (phase - 3.5f) / 3.5f;
            for (int y = row * cellHeight; y < Math.min(height, (row + 1) * cellHeight); y++) {
                for (int x = column * cellWidth; x < Math.min(width, (column + 1) * cellWidth); x++) {
                    final float dx = x + 0.5f - centerX;
                    final float dy = y + 0.5f - centerY;
                    if (dx * dx + dy * dy > radius * radius) {
                        continue;
                    }
                    final float phaseMask = clampFloat(0.55f + litOffset * (dx / radius), 0.12f, 1.0f);
                    final int alpha = clampInt(Math.round(230.0f * phaseMask), 0, 255);
                    blendPixel(image, x, y, 219, 226, 244, alpha);
                }
            }
        }
        return image;
    }

    private void blendPixel(final BufferedImage image, final int x, final int y, final int red, final int green, final int blue, final int alpha) {
        final int destination = image.getRGB(x, y);
        final float srcAlpha = clampInt(alpha, 0, 255) / 255.0f;
        final float dstAlpha = ((destination >>> 24) & 0xff) / 255.0f;
        final float outAlpha = srcAlpha + dstAlpha * (1.0f - srcAlpha);
        if (outAlpha <= 0.0001f) {
            return;
        }

        final float dstRed = (destination >>> 16) & 0xff;
        final float dstGreen = (destination >>> 8) & 0xff;
        final float dstBlue = destination & 0xff;
        final int outRed = clampInt(Math.round((red * srcAlpha + dstRed * dstAlpha * (1.0f - srcAlpha)) / outAlpha), 0, 255);
        final int outGreen = clampInt(Math.round((green * srcAlpha + dstGreen * dstAlpha * (1.0f - srcAlpha)) / outAlpha), 0, 255);
        final int outBlue = clampInt(Math.round((blue * srcAlpha + dstBlue * dstAlpha * (1.0f - srcAlpha)) / outAlpha), 0, 255);
        final int outA = clampInt(Math.round(outAlpha * 255.0f), 0, 255);
        image.setRGB(x, y, (outA << 24) | (outRed << 16) | (outGreen << 8) | outBlue);
    }

    private float luminance(final int red, final int green, final int blue) {
        return red * 0.2126f + green * 0.7152f + blue * 0.0722f;
    }

    private void renderSky() {
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        final double angle = environment.dayProgress() * Math.PI * 2.0;
        final float sunX = (float) Math.cos(angle);
        final float sunY = (float) Math.sin(angle);
        final float sunZ = -0.18f;
        final float[] sunDirection = normalize(sunX, sunY, sunZ);
        final float[] moonDirection = new float[]{-sunDirection[0], -sunDirection[1], -sunDirection[2]};

        final float rainDim = 1.0f - environment.rainStrength() * 0.45f;
        final float sunAlpha = environment.sunVisibility() * rainDim;
        if (sunAlpha > 0.01f) {
            renderCelestialQuad(sunTexture, sunDirection, CELESTIAL_SIZE, sunAlpha, 0.0f, 0.0f, 1.0f, 1.0f);
        }

        final float moonAlpha = environment.moonVisibility() * rainDim;
        if (moonAlpha > 0.01f) {
            final int phase = environment.moonPhase();
            final int column = phase % MOON_PHASE_COLUMNS;
            final int row = phase / MOON_PHASE_COLUMNS;
            final float u1 = column / (float) MOON_PHASE_COLUMNS;
            final float u2 = (column + 1) / (float) MOON_PHASE_COLUMNS;
            final float v1 = row / (float) MOON_PHASE_ROWS;
            final float v2 = (row + 1) / (float) MOON_PHASE_ROWS;
            renderCelestialQuad(moonTexture, moonDirection, CELESTIAL_SIZE * 1.05f, moonAlpha, u1, v1, u2, v2);
        }

        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        glEnable(GL_DEPTH_TEST);
    }

    private void setupWorldLighting() {
        final LightingState lighting = computeLightingState();
        glEnable(GL_LIGHTING);
        glEnable(GL_COLOR_MATERIAL);
        glColorMaterial(GL_FRONT_AND_BACK, GL_AMBIENT_AND_DIFFUSE);
        glLightModeli(GL_LIGHT_MODEL_TWO_SIDE, GL_TRUE);
        glLightModelfv(GL_LIGHT_MODEL_AMBIENT, lightBuffer(lighting.ambient(), lighting.ambient(), lighting.ambient(), 1.0f));

        glEnable(GL_LIGHT0);
        glLightfv(GL_LIGHT0, GL_AMBIENT, lightBuffer(0.0f, 0.0f, 0.0f, 1.0f));
        glLightfv(GL_LIGHT0, GL_DIFFUSE, lightBuffer(lighting.sunDiffuse() * 1.00f, lighting.sunDiffuse() * 0.94f, lighting.sunDiffuse() * 0.82f, 1.0f));
        glLightfv(GL_LIGHT0, GL_SPECULAR, lightBuffer(0.0f, 0.0f, 0.0f, 1.0f));
        glLightfv(GL_LIGHT0, GL_POSITION, lightBuffer(lighting.sunDirection()[0], lighting.sunDirection()[1], lighting.sunDirection()[2], 0.0f));

        glEnable(GL_LIGHT1);
        glLightfv(GL_LIGHT1, GL_AMBIENT, lightBuffer(0.0f, 0.0f, 0.0f, 1.0f));
        glLightfv(GL_LIGHT1, GL_DIFFUSE, lightBuffer(lighting.moonDiffuse() * 0.54f, lighting.moonDiffuse() * 0.66f, lighting.moonDiffuse(), 1.0f));
        glLightfv(GL_LIGHT1, GL_SPECULAR, lightBuffer(0.0f, 0.0f, 0.0f, 1.0f));
        glLightfv(GL_LIGHT1, GL_POSITION, lightBuffer(lighting.moonDirection()[0], lighting.moonDirection()[1], lighting.moonDirection()[2], 0.0f));
    }

    private void teardownWorldLighting() {
        glDisable(GL_LIGHT1);
        glDisable(GL_LIGHT0);
        glDisable(GL_COLOR_MATERIAL);
        glDisable(GL_LIGHTING);
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private LightingState computeLightingState() {
        final double angle = environment.dayProgress() * Math.PI * 2.0;
        final float sunX = (float) Math.cos(angle);
        final float sunY = (float) Math.sin(angle);
        final float sunZ = -0.18f;
        final float[] sunDirection = normalize(sunX, sunY, sunZ);
        final float[] moonDirection = new float[]{-sunDirection[0], -sunDirection[1], -sunDirection[2]};

        final float rain = environment.rainStrength();
        final float skyLight = clampFloat((environment.sunAltitude() + 0.20f) / 1.05f, 0.0f, 1.0f);
        final float rainAmbient = 1.0f - rain * 0.28f;
        final float rainDirect = 1.0f - rain * 0.45f;
        final float ambient = Math.max(0.12f, (0.18f + skyLight * 0.56f + environment.moonVisibility() * 0.05f) * rainAmbient);
        final float sunDiffuse = 0.42f * environment.sunVisibility() * rainDirect;
        final float moonDiffuse = 0.12f * environment.moonVisibility() * (1.0f - rain * 0.20f);
        return new LightingState(ambient, sunDiffuse, moonDiffuse, sunDirection, moonDirection);
    }

    private FloatBuffer lightBuffer(final float r, final float g, final float b, final float a) {
        lightScratchBuffer.clear();
        lightScratchBuffer.put(r).put(g).put(b).put(a).flip();
        return lightScratchBuffer;
    }

    private void renderCelestialQuad(
            final int textureId,
            final float[] direction,
            final float size,
            final float alpha,
            final float u1,
            final float v1,
            final float u2,
            final float v2
    ) {
        final float[] center = new float[]{
                direction[0] * CELESTIAL_DISTANCE,
                direction[1] * CELESTIAL_DISTANCE,
                direction[2] * CELESTIAL_DISTANCE
        };
        float[] right = new float[]{direction[2], 0.0f, -direction[0]};
        if (lengthSquared(right) < 0.0001f) {
            right = new float[]{1.0f, 0.0f, 0.0f};
        } else {
            right = normalize(right);
        }
        final float[] up = normalize(cross(direction, right));

        glBindTexture(GL_TEXTURE_2D, textureId);
        glColor4f(1.0f, 1.0f, 1.0f, clampFloat(alpha, 0.0f, 1.0f));
        glBegin(GL_QUADS);
        glTexCoord2f(u1, v2);
        skyVertex(center, right, up, -size, -size);
        glTexCoord2f(u2, v2);
        skyVertex(center, right, up, size, -size);
        glTexCoord2f(u2, v1);
        skyVertex(center, right, up, size, size);
        glTexCoord2f(u1, v1);
        skyVertex(center, right, up, -size, size);
        glEnd();
    }

    private void skyVertex(final float[] center, final float[] right, final float[] up, final float rx, final float uy) {
        glVertex3f(
                center[0] + right[0] * rx + up[0] * uy,
                center[1] + right[1] * rx + up[1] * uy,
                center[2] + right[2] * rx + up[2] * uy
        );
    }

    private void renderRainOverlay(final int width, final int height) {
        final float rain = environment.rainStrength();
        if (rain <= 0.01f) {
            return;
        }

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, width, height, 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        final float tile = 128.0f;
        final float u2 = width / tile;
        final float scroll = (environment.totalTicks() % 80) / 80.0f;
        final float v1 = scroll * 2.0f;
        final float v2 = v1 + height / tile;

        glBindTexture(GL_TEXTURE_2D, rainTexture);
        glColor4f(0.78f, 0.86f, 1.0f, 0.34f * rain);
        glBegin(GL_QUADS);
        glTexCoord2f(0.0f, v1);
        glVertex2f(0.0f, 0.0f);
        glTexCoord2f(u2, v1);
        glVertex2f(width, 0.0f);
        glTexCoord2f(u2, v2);
        glVertex2f(width, height);
        glTexCoord2f(0.0f, v2);
        glVertex2f(0.0f, height);
        glEnd();

        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
    }

    private void renderChunks() {
        final Set<Long> liveChunkKeys = new HashSet<>();
        for (final Chunk chunk : world.getChunks()) {
            final long key = chunkKey(chunk.getChunkX(), chunk.getChunkZ());
            liveChunkKeys.add(key);

            ChunkMesh mesh = chunkMeshes.get(key);
            if (mesh == null || mesh.builtRevision() != chunk.getRevision()) {
                if (mesh != null) {
                    mesh.destroy();
                }
                mesh = buildChunkMesh(chunk);
                chunkMeshes.put(key, mesh);
            }
        }

        removeStaleChunkMeshes(liveChunkKeys);

        glEnableClientState(GL_VERTEX_ARRAY);
        glEnableClientState(GL_NORMAL_ARRAY);
        glEnableClientState(GL_TEXTURE_COORD_ARRAY);
        glEnableClientState(GL_COLOR_ARRAY);
        for (final RenderLayer layer : RenderLayer.values()) {
            glDepthMask(layer != RenderLayer.TRANSLUCENT);
            for (final ChunkMesh mesh : chunkMeshes.values()) {
                mesh.render(layer);
            }
        }
        glDepthMask(true);
        glDepthFunc(GL_LESS);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glDisableClientState(GL_COLOR_ARRAY);
        glDisableClientState(GL_TEXTURE_COORD_ARRAY);
        glDisableClientState(GL_NORMAL_ARRAY);
        glDisableClientState(GL_VERTEX_ARRAY);
    }

    private ChunkMesh buildChunkMesh(final Chunk chunk) {
        final ChunkMeshBuilder builder = new ChunkMeshBuilder();
        chunk.forEachBlock(block -> appendBlockToMesh(builder, block));
        return builder.upload(chunk.getRevision());
    }

    private void removeStaleChunkMeshes(final Set<Long> liveChunkKeys) {
        final List<Long> staleKeys = new ArrayList<>();
        for (final Long key : chunkMeshes.keySet()) {
            if (!liveChunkKeys.contains(key)) {
                staleKeys.add(key);
            }
        }
        for (final Long key : staleKeys) {
            final ChunkMesh removed = chunkMeshes.remove(key);
            if (removed != null) {
                removed.destroy();
            }
        }
    }

    private long chunkKey(final int x, final int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private void appendBlockToMesh(final ChunkMeshBuilder builder, final Block block) {
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

        final MinecraftBlockModel[] minecraftModels = ENABLE_MINECRAFT_BLOCK_MODELS ? minecraftModelsFor(block) : null;
        if (minecraftModels != null && minecraftModels.length > 0) {
            final int modelIndex = computeMaterialVariant(block.getTypeId(), x, y, z, minecraftModels.length);
            appendMinecraftModelToMesh(builder, minecraftModels[modelIndex], block, minX, minY, minZ, size);
            return;
        }

        final String shape = blockShapes.getOrDefault(block.getTypeId(), "cube");
        if ("cross".equals(shape)) {
            appendCrossPlant(builder, textures.side(), minX, minY, minZ, size);
            return;
        }
        if ("crop".equals(shape)) {
            appendCropPlant(builder, textures.side(), minX, minY, minZ, size);
            return;
        }
        if ("torch".equals(shape)) {
            appendTorchSprite(builder, textures.side(), minX, minY, minZ, size);
            return;
        }
        if ("redstone_wire".equals(shape)) {
            appendRedstoneWire(builder, block, textures.side(), minX, minY, minZ, size);
            return;
        }

        if (isFaceVisible(block, x, y, z + 1)) {
            builder.addQuad(horizontalTexture(horizontalTextures(variants, block, BlockFacing.SOUTH), block, BlockFacing.SOUTH), renderLayer(block), 0.88f, 0.0f, 0.0f, 1.0f, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ);
        }
        if (isFaceVisible(block, x, y, z - 1)) {
            builder.addQuad(horizontalTexture(horizontalTextures(variants, block, BlockFacing.NORTH), block, BlockFacing.NORTH), renderLayer(block), 0.88f, 0.0f, 0.0f, -1.0f, maxX, minY, minZ, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ);
        }
        if (isFaceVisible(block, x + 1, y, z)) {
            builder.addQuad(horizontalTexture(horizontalTextures(variants, block, BlockFacing.EAST), block, BlockFacing.EAST), renderLayer(block), 0.83f, 1.0f, 0.0f, 0.0f, maxX, minY, maxZ, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ);
        }
        if (isFaceVisible(block, x - 1, y, z)) {
            builder.addQuad(horizontalTexture(horizontalTextures(variants, block, BlockFacing.WEST), block, BlockFacing.WEST), renderLayer(block), 0.83f, -1.0f, 0.0f, 0.0f, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ);
        }
        if (isFaceVisible(block, x, y + 1, z)) {
            builder.addQuad(textures.top(), renderLayer(block), 1.0f, 0.0f, 1.0f, 0.0f, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ);
        }
        if (isFaceVisible(block, x, y - 1, z)) {
            builder.addQuad(textures.bottom(), renderLayer(block), 0.72f, 0.0f, -1.0f, 0.0f, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ);
        }
    }

    private RenderLayer renderLayer(final Block block) {
        return isLiquidBlock(block) ? RenderLayer.TRANSLUCENT : RenderLayer.OPAQUE;
    }

    private void appendMinecraftModelToMesh(
            final ChunkMeshBuilder builder,
            final MinecraftBlockModel model,
            final Block block,
            final float baseX,
            final float baseY,
            final float baseZ,
            final float blockSize
    ) {
        for (final ModelQuad quad : model.quads()) {
            if (quad.cullable() && !isModelCullFaceVisible(block, quad.normal())) {
                continue;
            }
            final float[][] vertices = new float[4][3];
            for (int i = 0; i < vertices.length; i++) {
                vertices[i] = new float[]{
                        baseX + (quad.vertices()[i][0] / 16.0f) * blockSize,
                        baseY + (quad.vertices()[i][1] / 16.0f) * blockSize,
                        baseZ + (quad.vertices()[i][2] / 16.0f) * blockSize
                };
            }
            builder.addQuad(
                    quad.textureId(),
                    quad.tinted() ? RenderLayer.DEPTH_EQUAL : RenderLayer.OPAQUE,
                    quad.shade(),
                    quad.normal()[0],
                    quad.normal()[1],
                    quad.normal()[2],
                    vertices,
                    quad.uvs()
            );
        }
    }

    private boolean isModelCullFaceVisible(final Block block, final float[] normal) {
        if (block == null || normal == null || normal.length < 3) {
            return true;
        }

        final float ax = Math.abs(normal[0]);
        final float ay = Math.abs(normal[1]);
        final float az = Math.abs(normal[2]);
        if (ax <= 0.0001f && ay <= 0.0001f && az <= 0.0001f) {
            return true;
        }

        final int x = block.getWorldX();
        final int y = block.getWorldY();
        final int z = block.getWorldZ();
        if (ay >= ax && ay >= az) {
            return isFaceVisible(block, x, y + (normal[1] >= 0.0f ? 1 : -1), z);
        }
        if (ax >= az) {
            return isFaceVisible(block, x + (normal[0] >= 0.0f ? 1 : -1), y, z);
        }
        return isFaceVisible(block, x, y, z + (normal[2] >= 0.0f ? 1 : -1));
    }

    private void appendCrossPlant(final ChunkMeshBuilder builder, final int textureId, final float minX, final float minY, final float minZ, final float size) {
        final float inset = size * 0.05f;
        final float maxY = minY + size;
        final float centerX = minX + size * 0.5f;
        final float centerZ = minZ + size * 0.5f;
        final float leftX = minX + inset;
        final float rightX = minX + size - inset;
        final float nearZ = minZ + inset;
        final float farZ = minZ + size - inset;

        appendFace(builder, textureId, 1.0f, leftX, minY, centerZ, rightX, minY, centerZ, rightX, maxY, centerZ, leftX, maxY, centerZ);
        appendFace(builder, textureId, 1.0f, centerX, minY, farZ, centerX, minY, nearZ, centerX, maxY, nearZ, centerX, maxY, farZ);
    }

    private void appendCropPlant(final ChunkMeshBuilder builder, final int textureId, final float minX, final float minY, final float minZ, final float size) {
        final float maxY = minY + size * 0.875f;
        final float leftX = minX + size * 0.18f;
        final float rightX = minX + size * 0.82f;
        final float nearZ = minZ + size * 0.18f;
        final float farZ = minZ + size * 0.82f;
        final float zA = minZ + size * 0.34f;
        final float zB = minZ + size * 0.66f;
        final float xA = minX + size * 0.34f;
        final float xB = minX + size * 0.66f;

        appendFace(builder, textureId, 1.0f, leftX, minY, zA, rightX, minY, zA, rightX, maxY, zA, leftX, maxY, zA);
        appendFace(builder, textureId, 1.0f, leftX, minY, zB, rightX, minY, zB, rightX, maxY, zB, leftX, maxY, zB);
        appendFace(builder, textureId, 1.0f, xA, minY, farZ, xA, minY, nearZ, xA, maxY, nearZ, xA, maxY, farZ);
        appendFace(builder, textureId, 1.0f, xB, minY, farZ, xB, minY, nearZ, xB, maxY, nearZ, xB, maxY, farZ);
    }

    private void appendTorchSprite(final ChunkMeshBuilder builder, final int textureId, final float minX, final float minY, final float minZ, final float size) {
        final float centerX = minX + size * 0.5f;
        final float centerZ = minZ + size * 0.5f;
        final float halfWidth = size * 0.22f;
        final float bottomY = minY;
        final float topY = minY + size * 0.88f;
        appendFace(builder, textureId, 1.0f,
                centerX - halfWidth, bottomY, centerZ - halfWidth,
                centerX + halfWidth, bottomY, centerZ + halfWidth,
                centerX + halfWidth, topY, centerZ + halfWidth,
                centerX - halfWidth, topY, centerZ - halfWidth);
        appendFace(builder, textureId, 1.0f,
                centerX - halfWidth, bottomY, centerZ + halfWidth,
                centerX + halfWidth, bottomY, centerZ - halfWidth,
                centerX + halfWidth, topY, centerZ - halfWidth,
                centerX - halfWidth, topY, centerZ + halfWidth);
    }

    private void appendRedstoneWire(final ChunkMeshBuilder builder, final Block block, final int textureId, final float minX, final float minY, final float minZ, final float size) {
        final float y = minY + size * 0.028f;
        final float centerX = minX + size * 0.5f;
        final float centerZ = minZ + size * 0.5f;
        final float dotHalf = size * 0.18f;
        final float lineHalf = size * 0.095f;
        final boolean north = isSameBlockType(block, 0, 0, -1);
        final boolean south = isSameBlockType(block, 0, 0, 1);
        final boolean west = isSameBlockType(block, -1, 0, 0);
        final boolean east = isSameBlockType(block, 1, 0, 0);

        builder.addFlatQuad(textureId, 1.0f,
                centerX - dotHalf, y, centerZ - dotHalf,
                centerX + dotHalf, y, centerZ - dotHalf,
                centerX + dotHalf, y, centerZ + dotHalf,
                centerX - dotHalf, y, centerZ + dotHalf,
                0.0f, 0.0f, 1.0f, 1.0f);

        if (north || (!south && !west && !east)) {
            builder.addFlatQuad(textureId, 1.0f,
                    centerX - lineHalf, y, minZ,
                    centerX + lineHalf, y, minZ,
                    centerX + lineHalf, y, centerZ,
                    centerX - lineHalf, y, centerZ,
                    0.0f, 0.0f, 1.0f, 1.0f);
        }
        if (south || (!north && !west && !east)) {
            builder.addFlatQuad(textureId, 1.0f,
                    centerX - lineHalf, y, centerZ,
                    centerX + lineHalf, y, centerZ,
                    centerX + lineHalf, y, minZ + size,
                    centerX - lineHalf, y, minZ + size,
                    0.0f, 0.0f, 1.0f, 1.0f);
        }
        if (west || (!north && !south && !east)) {
            builder.addFlatQuad(textureId, 1.0f,
                    minX, y, centerZ - lineHalf,
                    centerX, y, centerZ - lineHalf,
                    centerX, y, centerZ + lineHalf,
                    minX, y, centerZ + lineHalf,
                    0.0f, 0.0f, 1.0f, 1.0f);
        }
        if (east || (!north && !south && !west)) {
            builder.addFlatQuad(textureId, 1.0f,
                    centerX, y, centerZ - lineHalf,
                    minX + size, y, centerZ - lineHalf,
                    minX + size, y, centerZ + lineHalf,
                    centerX, y, centerZ + lineHalf,
                    0.0f, 0.0f, 1.0f, 1.0f);
        }
    }

    private void appendFace(
            final ChunkMeshBuilder builder,
            final int textureId,
            final float shade,
            final float x1, final float y1, final float z1,
            final float x2, final float y2, final float z2,
            final float x3, final float y3, final float z3,
            final float x4, final float y4, final float z4
    ) {
        final float[] normal = normalFromQuad(
                x1, y1, z1,
                x2, y2, z2,
                x3, y3, z3
        );
        builder.addQuad(textureId, shade, normal[0], normal[1], normal[2], x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4);
    }

    private int horizontalTexture(final BlockTextures textures, final Block block, final BlockFacing face) {
        return textures.directional() && block.getFacing() == face ? textures.front() : textures.side();
    }

    private BlockTextures horizontalTextures(final BlockTextures[] variants, final Block block, final BlockFacing face) {
        if (variants.length <= 1) {
            return variants[0];
        }
        final int variantIndex = computeMaterialVariant(
                block.getTypeId(),
                block.getWorldX(),
                block.getWorldY(),
                block.getWorldZ(),
                variants.length,
                face.ordinal() + 1
        );
        return variants[variantIndex];
    }

    private boolean isSameBlockType(final Block block, final int dx, final int dy, final int dz) {
        final Block neighbor = world.getBlock(block.getWorldX() + dx, block.getWorldY() + dy, block.getWorldZ() + dz);
        return neighbor != null && block.getTypeId().equals(neighbor.getTypeId());
    }

    private boolean isFaceVisible(final Block block, final int neighborX, final int neighborY, final int neighborZ) {
        final Block neighbor = world.getBlock(neighborX, neighborY, neighborZ);
        if (isLiquidBlock(block)) {
            return neighbor == null;
        }
        if (isLeafBlock(block.getTypeId()) && isLeafBlock(neighbor == null ? null : neighbor.getTypeId())) {
            return false;
        }
        if (isLiquidBlock(neighbor)) {
            return true;
        }
        return !isOccludingFullBlock(neighbor);
    }

    private boolean isOccludingFullBlock(final Block block) {
        if (block == null) {
            return false;
        }
        if (isLeafBlock(block.getTypeId())) {
            return false;
        }
        final String shape = blockShapes.getOrDefault(block.getTypeId(), "cube");
        return "cube".equals(shape) && block.isSolid();
    }

    private boolean isLiquidBlock(final Block block) {
        return block != null && ("water".equals(block.getTypeId().path()) || "lava".equals(block.getTypeId().path()));
    }

    private void initializeTextures() {
        grassTint = resolveGrassTint();
        final Color waterTint = DEFAULT_WATER_TINT;
        for (final GameObject type : registries.blocks().values()) {
            if (!type.has(BlockComponent.class)) {
                continue;
            }

            final ModelComponent model = type.has(ModelComponent.class)
                    ? type.get(ModelComponent.class)
                    : new ModelComponent(type.id().path());
            blockShapes.put(type.id(), model.shape());
            if (ENABLE_MINECRAFT_BLOCK_MODELS) {
                final MinecraftBlockModel[] minecraftModels = loadMinecraftBlockModels(type, model, grassTint, BlockFacing.NORTH);
                if (minecraftModels.length > 0) {
                    minecraftBlockModels.put(new BlockModelKey(type.id(), BlockFacing.NORTH), minecraftModels);
                }
            }
            final boolean leafBlock = isLeafBlock(type);
            final int variantCount = leafBlock ? 1 : resolveVariantCount(model);
            final BlockTextures[] variants = new BlockTextures[variantCount];
            for (int variant = 0; variant < variantCount; variant++) {
                BufferedImage side = leafBlock
                        ? loadLeafBaseBlockImage(model.sideCandidates())
                        : loadBlockImageForVariant(model.sideCandidates(), variant);
                BufferedImage front = leafBlock || !model.hasFrontCandidates()
                        ? side
                        : loadBlockImageForVariant(model.frontCandidates(), variant);
                BufferedImage top = leafBlock
                        ? loadLeafBaseBlockImage(model.topCandidates())
                        : loadBlockImageForVariant(model.topCandidates(), variant);
                BufferedImage bottom = leafBlock
                        ? loadLeafBaseBlockImage(model.bottomCandidates())
                        : loadBlockImageForVariant(model.bottomCandidates(), variant);

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
                if (model.hasTint("water")) {
                    side = tintOrSolid(side, waterTint);
                    front = tintOrSolid(front, waterTint);
                    top = tintOrSolid(top, waterTint);
                    bottom = tintOrSolid(bottom, waterTint);
                }

                final int sideId = createTextureFromImageOrFallback(side, fallbackColor(type, model, Face.SIDE));
                final int frontId = model.hasFrontCandidates()
                        ? createTextureFromImageOrFallback(front, fallbackColor(type, model, Face.FRONT))
                        : sideId;
                final int topId = createTextureFromImageOrFallback(top, fallbackColor(type, model, Face.TOP));
                final int bottomId = createTextureFromImageOrFallback(bottom, fallbackColor(type, model, Face.BOTTOM));
                variants[variant] = new BlockTextures(sideId, frontId, topId, bottomId, model.hasFrontCandidates());
            }
            blockTextureVariants.put(type.id(), variants);
            if (fallbackBlockTextures == null || DIRT_ID.equals(type.id())) {
                fallbackBlockTextures = variants;
            }
        }
    }

    private boolean isLeafBlock(final GameObject type) {
        return type != null && type.id().path().contains("leaves");
    }

    private boolean isLeafBlock(final ResourceId typeId) {
        return typeId != null && typeId.path().contains("leaves");
    }

    private boolean hasIndexedTextureCandidates(final ModelComponent model) {
        return hasIndexedVariantCandidate(model.sideCandidates())
                || hasIndexedVariantCandidate(model.frontCandidates())
                || hasIndexedVariantCandidate(model.topCandidates())
                || hasIndexedVariantCandidate(model.bottomCandidates());
    }

    private MinecraftBlockModel[] minecraftModelsFor(final Block block) {
        final BlockFacing facing = block.getFacing() == null ? BlockFacing.NORTH : block.getFacing();
        final BlockModelKey key = new BlockModelKey(block.getTypeId(), facing);
        final MinecraftBlockModel[] cached = minecraftBlockModels.get(key);
        if (cached != null) {
            return cached;
        }

        final GameObject type = block.getType();
        final ModelComponent model = type.has(ModelComponent.class)
                ? type.get(ModelComponent.class)
                : new ModelComponent(type.id().path());
        final MinecraftBlockModel[] models = loadMinecraftBlockModels(type, model, grassTint, facing);
        minecraftBlockModels.put(key, models);
        return models;
    }

    private MinecraftBlockModel[] loadMinecraftBlockModels(final GameObject type, final ModelComponent model, final Color grassTint, final BlockFacing facing) {
        final List<MinecraftBlockModel> models = loadMinecraftBlockModels(loadBlockStateModelDefinitionGroups(type.id().path(), facing), model, grassTint);
        if (!models.isEmpty()) {
            return models.toArray(MinecraftBlockModel[]::new);
        }

        final ModelDefinition directDefinition = new ModelDefinition(type.id().path(), 0.0f, 0.0f, 0.0f);
        return loadMinecraftBlockModels(List.of(new ModelDefinitionGroup(List.of(directDefinition), 1)), model, grassTint)
                .toArray(MinecraftBlockModel[]::new);
    }

    private List<MinecraftBlockModel> loadMinecraftBlockModels(final List<ModelDefinitionGroup> groups, final ModelComponent model, final Color grassTint) {
        final List<MinecraftBlockModel> models = new ArrayList<>();
        for (final ModelDefinitionGroup group : groups) {
            final MinecraftBlockModel resolvedModel = loadMinecraftBlockModel(group, model, grassTint);
            if (resolvedModel != null) {
                final int weight = expandedModelWeight(group);
                for (int i = 0; i < weight; i++) {
                    models.add(resolvedModel);
                }
            }
        }
        return models;
    }

    private int expandedModelWeight(final ModelDefinitionGroup group) {
        return clampInt(group == null ? 1 : group.weight(), 1, MAX_EXPANDED_MODEL_WEIGHT);
    }

    private List<ModelDefinitionGroup> loadBlockStateModelDefinitionGroups(final String blockName, final BlockFacing facing) {
        final String blockStateJson = resourcePackLoader.loadBlockState(blockName);
        if (blockStateJson == null || blockStateJson.isBlank()) {
            return List.of();
        }

        try {
            final Map<String, Object> root = JsonParser.parseObject(blockStateJson);
            final Map<String, Object> variants = object(root.get("variants"));
            if (!variants.isEmpty()) {
                return readModelDefinitionGroups(selectDefaultVariant(variants, facing));
            }

            final List<Object> multipart = list(root.get("multipart"));
            if (!multipart.isEmpty()) {
                final List<List<ModelDefinitionGroup>> choicesByPart = new ArrayList<>();
                for (final Object rawPart : multipart) {
                    final Map<String, Object> part = object(rawPart);
                    if (!matchesDefaultState(part.get("when"), facing)) {
                        continue;
                    }

                    final List<ModelDefinitionGroup> applyChoices = readModelDefinitionGroups(part.get("apply"));
                    if (!applyChoices.isEmpty()) {
                        choicesByPart.add(applyChoices);
                    }
                }
                return buildMultipartDefinitionGroups(choicesByPart);
            }
            return List.of();
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private Object selectDefaultVariant(final Map<String, Object> variants, final BlockFacing facing) {
        final Object emptyVariant = variants.get("");
        if (emptyVariant != null) {
            return emptyVariant;
        }
        if (variants.size() == 1) {
            return variants.values().iterator().next();
        }

        String bestKey = null;
        int bestScore = Integer.MAX_VALUE;
        for (final String key : variants.keySet()) {
            final int score = scoreVariantKey(key, facing);
            if (score < bestScore || (score == bestScore && (bestKey == null || key.compareTo(bestKey) < 0))) {
                bestKey = key;
                bestScore = score;
            }
        }
        return bestKey == null ? null : variants.get(bestKey);
    }

    private List<ModelDefinitionGroup> buildMultipartDefinitionGroups(final List<List<ModelDefinitionGroup>> choicesByPart) {
        if (choicesByPart.isEmpty()) {
            return List.of();
        }

        final int bucketCount = hasMultipartVariations(choicesByPart) ? MINECRAFT_MODEL_VARIANT_BUCKETS : 1;
        final List<ModelDefinitionGroup> groups = new ArrayList<>();
        final Set<String> seen = new HashSet<>();
        for (int bucket = 0; bucket < bucketCount; bucket++) {
            final List<ModelDefinition> composite = new ArrayList<>();
            for (int part = 0; part < choicesByPart.size(); part++) {
                final ModelDefinitionGroup selected = selectWeightedMultipartChoice(choicesByPart.get(part), bucket, part);
                if (selected != null) {
                    composite.addAll(selected.definitions());
                }
            }
            if (composite.isEmpty()) {
                continue;
            }

            final String key = modelDefinitionKey(composite);
            if (seen.add(key)) {
                groups.add(new ModelDefinitionGroup(composite, 1));
            }
        }
        return groups;
    }

    private boolean hasMultipartVariations(final List<List<ModelDefinitionGroup>> choicesByPart) {
        for (final List<ModelDefinitionGroup> choices : choicesByPart) {
            if (choices.size() > 1) {
                return true;
            }
            if (!choices.isEmpty() && choices.get(0).weight() > 1) {
                return true;
            }
        }
        return false;
    }

    private ModelDefinitionGroup selectWeightedMultipartChoice(final List<ModelDefinitionGroup> choices, final int bucket, final int part) {
        if (choices == null || choices.isEmpty()) {
            return null;
        }
        if (choices.size() == 1) {
            return choices.get(0);
        }

        long totalWeight = 0L;
        for (final ModelDefinitionGroup choice : choices) {
            totalWeight += selectionWeight(choice);
        }
        if (totalWeight <= 0L) {
            return choices.get(0);
        }

        long selectedWeight = Math.floorMod(modelVariantSeed(bucket, part), totalWeight);
        for (final ModelDefinitionGroup choice : choices) {
            selectedWeight -= selectionWeight(choice);
            if (selectedWeight < 0L) {
                return choice;
            }
        }
        return choices.get(choices.size() - 1);
    }

    private int selectionWeight(final ModelDefinitionGroup group) {
        return clampInt(group == null ? 1 : group.weight(), 1, MAX_MODEL_SELECTION_WEIGHT);
    }

    private long modelVariantSeed(final int bucket, final int part) {
        long hash = 1469598103934665603L;
        hash = (hash ^ bucket) * 1099511628211L;
        hash = (hash ^ (part * 31L + 0x9E3779B97F4A7C15L)) * 1099511628211L;
        hash ^= hash >>> 32;
        return hash & Long.MAX_VALUE;
    }

    private String modelDefinitionKey(final List<ModelDefinition> definitions) {
        final StringBuilder key = new StringBuilder();
        for (final ModelDefinition definition : definitions) {
            key.append(definition.modelPath())
                    .append('@')
                    .append(definition.xRotation())
                    .append(',')
                    .append(definition.yRotation())
                    .append(',')
                    .append(definition.zRotation())
                    .append(';');
        }
        return key.toString();
    }

    private List<ModelDefinitionGroup> readModelDefinitionGroups(final Object value) {
        if (value == null) {
            return List.of();
        }

        if (value instanceof List<?> list) {
            final List<ModelDefinitionGroup> groups = new ArrayList<>();
            for (final Object item : list) {
                groups.addAll(readModelDefinitionGroups(item));
            }
            return groups;
        }

        final Map<String, Object> model = object(value);
        final String modelPath = normalizeMinecraftModelPath(string(model, "model", ""));
        if (modelPath.isBlank()) {
            return List.of();
        }

        final ModelDefinition definition = new ModelDefinition(
                modelPath,
                number(model.get("x"), 0.0f),
                number(model.get("y"), 0.0f),
                number(model.get("z"), 0.0f)
        );
        final int weight = clampInt(Math.round(number(model.get("weight"), 1.0f)), 1, MAX_MODEL_SELECTION_WEIGHT);
        return List.of(new ModelDefinitionGroup(List.of(definition), weight));
    }

    private boolean matchesDefaultState(final Object rawWhen, final BlockFacing facing) {
        if (rawWhen == null) {
            return true;
        }

        final Map<String, Object> when = object(rawWhen);
        if (when.isEmpty()) {
            return true;
        }

        for (final Map.Entry<String, Object> entry : when.entrySet()) {
            final String key = entry.getKey();
            final Object value = entry.getValue();
            if ("OR".equalsIgnoreCase(key)) {
                if (!matchesAnyDefaultState(value, facing)) {
                    return false;
                }
                continue;
            }
            if ("AND".equalsIgnoreCase(key)) {
                if (!matchesAllDefaultStates(value, facing)) {
                    return false;
                }
                continue;
            }
            if (!conditionAllowsDefault(key, value, facing)) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesAnyDefaultState(final Object value, final BlockFacing facing) {
        for (final Object item : list(value)) {
            if (matchesDefaultState(item, facing)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesAllDefaultStates(final Object value, final BlockFacing facing) {
        final List<Object> conditions = list(value);
        if (conditions.isEmpty()) {
            return true;
        }
        for (final Object item : conditions) {
            if (!matchesDefaultState(item, facing)) {
                return false;
            }
        }
        return true;
    }

    private boolean conditionAllowsDefault(final String property, final Object expectedValue, final BlockFacing facing) {
        final String expected = String.valueOf(expectedValue).trim().toLowerCase();
        final String defaultValue = defaultBlockStateValue(property, expected, facing);
        if (defaultValue.isBlank()) {
            return false;
        }
        for (final String option : expected.split("\\|")) {
            if (defaultValue.equals(option.trim())) {
                return true;
            }
        }
        return false;
    }

    private String defaultBlockStateValue(final String rawProperty, final String expected, final BlockFacing facing) {
        final String property = rawProperty == null ? "" : rawProperty.trim().toLowerCase();
        return switch (property) {
            case "age", "charges", "delay", "distance", "eggs", "hatch", "layers", "level", "moisture",
                    "pickles", "power", "rotation", "stage" -> "0";
            case "axis" -> "y";
            case "face", "vertical_direction" -> "floor";
            case "facing" -> blockStateFacing(facing);
            case "half" -> containsOption(expected, "lower") ? "lower" : "bottom";
            case "hinge" -> "left";
            case "mode" -> "compare";
            case "north", "east", "south", "west", "up", "down" -> "false";
            case "part" -> "foot";
            case "shape" -> "straight";
            case "type" -> containsOption(expected, "single") ? "single" : "bottom";
            case "attachment" -> "floor";
            case "attached", "bottom", "conditional", "disarmed", "drag", "enabled", "extended", "eye",
                    "falling", "hanging", "has_book", "in_wall", "inverted", "lit", "locked", "occupied",
                    "open", "persistent", "powered", "short", "signal_fire", "snowy", "triggered",
                    "unstable", "waterlogged" -> "false";
            default -> {
                if (containsOption(expected, "false")) {
                    yield "false";
                }
                if (containsOption(expected, "0")) {
                    yield "0";
                }
                if (containsOption(expected, "none")) {
                    yield "none";
                }
                yield "";
            }
        };
    }

    private String blockStateFacing(final BlockFacing facing) {
        final BlockFacing normalized = facing == null ? BlockFacing.NORTH : facing;
        return normalized.name().toLowerCase();
    }

    private boolean containsOption(final String expected, final String option) {
        for (final String value : expected.split("\\|")) {
            if (option.equals(value.trim())) {
                return true;
            }
        }
        return false;
    }

    private int scoreVariantKey(final String variantKey, final BlockFacing facing) {
        if (variantKey == null || variantKey.isBlank()) {
            return 0;
        }

        int score = 0;
        for (final String rawPart : variantKey.split(",")) {
            final int equalsIndex = rawPart.indexOf('=');
            if (equalsIndex <= 0) {
                continue;
            }
            final String property = rawPart.substring(0, equalsIndex).trim().toLowerCase();
            final String value = rawPart.substring(equalsIndex + 1).trim().toLowerCase();
            score += scoreVariantProperty(property, value, facing);
        }
        return score;
    }

    private int scoreVariantProperty(final String property, final String value, final BlockFacing facing) {
        final String preferred = defaultBlockStateValue(property, value, facing);
        if (!preferred.isBlank() && preferred.equals(value)) {
            return 0;
        }
        if (isInteger(value)) {
            return Math.min(20, Math.abs(Integer.parseInt(value)));
        }
        if ("false".equals(value) || "none".equals(value)) {
            return 0;
        }
        if ("true".equals(value)) {
            return 4;
        }
        return 2;
    }

    private boolean isInteger(final String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        int start = value.charAt(0) == '-' ? 1 : 0;
        if (start == value.length()) {
            return false;
        }
        for (int i = start; i < value.length(); i++) {
            final char c = value.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    private MinecraftBlockModel loadMinecraftBlockModel(final ModelDefinitionGroup group, final ModelComponent model, final Color grassTint) {
        final List<ModelQuad> quads = new ArrayList<>();
        for (final ModelDefinition definition : group.definitions()) {
            appendMinecraftModelQuads(definition, model, grassTint, quads);
        }
        return quads.isEmpty() ? null : new MinecraftBlockModel(quads);
    }

    private void appendMinecraftModelQuads(
            final ModelDefinition definition,
            final ModelComponent model,
            final Color grassTint,
            final List<ModelQuad> quads
    ) {
        final ResolvedMinecraftModel resolvedModel = resolveMinecraftBlockModel(definition.modelPath(), new HashSet<>());
        if (resolvedModel == null || resolvedModel.elements().isEmpty()) {
            return;
        }

        try {
            for (final Object rawElement : resolvedModel.elements()) {
                final Map<String, Object> element = object(rawElement);
                final float[] from = vec3(element.get("from"));
                final float[] to = vec3(element.get("to"));
                final ModelRotation rotation = readRotation(object(element.get("rotation")));
                final boolean shade = bool(element, "shade", true);
                final Map<String, Object> faces = object(element.get("faces"));
                for (final Map.Entry<String, Object> faceEntry : faces.entrySet()) {
                    final String faceName = faceEntry.getKey();
                    final Map<String, Object> face = object(faceEntry.getValue());
                    final String textureReference = string(face, "texture", "");
                    final String texturePath = resolveTexturePath(textureReference, resolvedModel.textures());
                    if (texturePath.isBlank()) {
                        continue;
                    }
                    final boolean tinted = face.containsKey("tintindex");
                    final boolean cullable = !string(face, "cullface", "").isBlank();
                    final int textureId = loadModelTexture(texturePath, tinted ? grassTint : null);
                    final float[][] uvs = faceUvs(face.get("uv"), Math.round(number(face.get("rotation"), 0.0f)));
                    final float[][] vertices = faceVertices(faceName, from, to);
                    if (vertices.length == 0) {
                        continue;
                    }
                    for (int i = 0; i < vertices.length; i++) {
                        vertices[i] = rotate(vertices[i], rotation);
                        vertices[i] = rotateBlockState(vertices[i], definition);
                    }
                    quads.add(new ModelQuad(
                            textureId,
                            shadeForTransformedFace(faceName, shade, vertices),
                            uvs,
                            vertices,
                            normalFromVertices(vertices),
                            tinted,
                            cullable
                    ));
                }
            }
        } catch (RuntimeException ignored) {
        }
    }

    private ResolvedMinecraftModel resolveMinecraftBlockModel(final String rawModelPath, final Set<String> stack) {
        final String modelPath = normalizeMinecraftModelPath(rawModelPath);
        if (modelPath.isBlank()) {
            return null;
        }

        final ResolvedMinecraftModel cached = resolvedModelCache.get(modelPath);
        if (cached != null) {
            return cached;
        }
        if (!stack.add(modelPath)) {
            return null;
        }

        try {
            final String modelJson = resourcePackLoader.loadBlockModel(modelPath);
            if (modelJson == null || modelJson.isBlank()) {
                return null;
            }

            final Map<String, Object> root = JsonParser.parseObject(modelJson);
            final Map<String, Object> textures = new LinkedHashMap<>();
            List<Object> elements = list(root.get("elements"));

            final String parentPath = string(root, "parent", "");
            if (!parentPath.isBlank()) {
                final ResolvedMinecraftModel parent = resolveMinecraftBlockModel(parentPath, stack);
                if (parent != null) {
                    textures.putAll(parent.textures());
                    if (elements.isEmpty()) {
                        elements = parent.elements();
                    }
                }
            }

            textures.putAll(object(root.get("textures")));
            final ResolvedMinecraftModel resolvedModel = new ResolvedMinecraftModel(elements, textures);
            resolvedModelCache.put(modelPath, resolvedModel);
            return resolvedModel;
        } catch (RuntimeException ignored) {
            return null;
        } finally {
            stack.remove(modelPath);
        }
    }

    private int loadModelTexture(final String texturePath, final Color tint) {
        final String normalizedPath = normalizeMinecraftTexturePath(texturePath);
        final String cacheKey = normalizedPath + "|" + (tint == null ? "" : tint.getRGB());
        final Integer cached = modelTextureCache.get(cacheKey);
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
        modelTextureCache.put(cacheKey, textureId);
        return textureId;
    }

    private String normalizeMinecraftTexturePath(final String texturePath) {
        String path = texturePath == null ? "" : texturePath.trim();
        final int namespaceIndex = path.indexOf(':');
        if (namespaceIndex >= 0) {
            path = path.substring(namespaceIndex + 1);
        }
        return path;
    }

    private String normalizeMinecraftModelPath(final String modelPath) {
        String path = modelPath == null ? "" : modelPath.trim();
        final int namespaceIndex = path.indexOf(':');
        if (namespaceIndex >= 0) {
            path = path.substring(namespaceIndex + 1);
        }
        if (path.startsWith("block/")) {
            path = path.substring("block/".length());
        }
        return path;
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

    private BufferedImage loadBlockImageForVariant(final String[] candidates, final int variant) {
        final String[] ordered = rotateCandidates(candidates, variant);
        BufferedImage firstImage = null;
        for (final String candidate : ordered) {
            final byte[] data = resourcePackLoader.loadBlockTexture(candidate);
            final BufferedImage image = decodeImage(data);
            if (image == null) {
                continue;
            }
            if (firstImage == null) {
                firstImage = image;
            }
            return image;
        }
        return firstImage;
    }

    private BufferedImage loadLeafBaseBlockImage(final String[] candidates) {
        if (candidates != null) {
            for (final String candidate : candidates) {
                if (candidate == null || candidate.isBlank() || isIndexedVariantCandidate(candidate)) {
                    continue;
                }
                final BufferedImage image = decodeImage(resourcePackLoader.loadBlockTexture(candidate));
                if (image != null) {
                    return image;
                }
            }
        }
        return loadBlockImageForVariant(candidates, 0);
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
            if (isIndexedVariantCandidate(candidate)) {
                return true;
            }
        }
        return false;
    }

    private boolean isIndexedVariantCandidate(final String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return false;
        }
        final char last = candidate.charAt(candidate.length() - 1);
        return last >= '0' && last <= '9';
    }

    private int resolveVariantCount(final ModelComponent model) {
        int max = 1;
        if (hasIndexedVariantCandidate(model.sideCandidates())) {
            max = Math.max(max, model.sideCandidates().length);
        }
        if (hasIndexedVariantCandidate(model.frontCandidates())) {
            max = Math.max(max, model.frontCandidates().length);
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
        return computeMaterialVariant(typeId, worldX, worldY, worldZ, variantCount, 0);
    }

    private int computeMaterialVariant(final ResourceId typeId, final int worldX, final int worldY, final int worldZ, final int variantCount, final int salt) {
        int hash = 17;
        hash = 31 * hash + typeId.hashCode();
        hash = 31 * hash + worldX;
        hash = 31 * hash + (worldY * 7);
        hash = 31 * hash + worldZ;
        hash = 31 * hash + salt;
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

    private BufferedImage tintOrSolid(final BufferedImage source, final Color tint) {
        if (source == null) {
            return createSolidImage(16, 16, tint);
        }
        return multiplyTint(source, tint);
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

    private float[] rotateBlockState(final float[] vertex, final ModelDefinition definition) {
        float[] rotated = vertex;
        final float[] origin = new float[]{8.0f, 8.0f, 8.0f};
        if (definition.xRotation() != 0.0f) {
            rotated = rotate(rotated, new ModelRotation("x", definition.xRotation(), origin));
        }
        if (definition.yRotation() != 0.0f) {
            rotated = rotate(rotated, new ModelRotation("y", definition.yRotation(), origin));
        }
        if (definition.zRotation() != 0.0f) {
            rotated = rotate(rotated, new ModelRotation("z", definition.zRotation(), origin));
        }
        return rotated;
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

    private float shadeForTransformedFace(final String fallbackFaceName, final boolean shade, final float[][] vertices) {
        if (!shade || vertices.length < 3) {
            return shadeForFace(fallbackFaceName, shade);
        }

        final float ux = vertices[1][0] - vertices[0][0];
        final float uy = vertices[1][1] - vertices[0][1];
        final float uz = vertices[1][2] - vertices[0][2];
        final float vx = vertices[2][0] - vertices[0][0];
        final float vy = vertices[2][1] - vertices[0][1];
        final float vz = vertices[2][2] - vertices[0][2];

        final float nx = uy * vz - uz * vy;
        final float ny = uz * vx - ux * vz;
        final float nz = ux * vy - uy * vx;
        final float ax = Math.abs(nx);
        final float ay = Math.abs(ny);
        final float az = Math.abs(nz);

        if (ay >= ax && ay >= az) {
            return shadeForFace(ny < 0.0f ? "down" : "up", true);
        }
        if (ax >= az) {
            return shadeForFace(nx < 0.0f ? "west" : "east", true);
        }
        return shadeForFace(nz < 0.0f ? "north" : "south", true);
    }

    private int uploadTexture(final BufferedImage image) {
        return textureLoader.uploadTexture(image);
    }

    private int fallbackColor(final GameObject type, final ModelComponent model, final Face face) {
        final int defaultColor = switch (type.id().path()) {
            case "grass" -> switch (face) {
                case SIDE -> rgba(110, 157, 74, 255);
                case FRONT -> rgba(110, 157, 74, 255);
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

    private float[] normalFromVertices(final float[][] vertices) {
        if (vertices == null || vertices.length < 3) {
            return new float[]{0.0f, 1.0f, 0.0f};
        }
        return normalFromPoints(vertices[0], vertices[1], vertices[2]);
    }

    private float[] normalFromQuad(
            final float x1, final float y1, final float z1,
            final float x2, final float y2, final float z2,
            final float x3, final float y3, final float z3
    ) {
        return normalFromPoints(
                new float[]{x1, y1, z1},
                new float[]{x2, y2, z2},
                new float[]{x3, y3, z3}
        );
    }

    private float[] normalFromPoints(final float[] a, final float[] b, final float[] c) {
        final float[] ab = new float[]{b[0] - a[0], b[1] - a[1], b[2] - a[2]};
        final float[] ac = new float[]{c[0] - a[0], c[1] - a[1], c[2] - a[2]};
        final float[] normal = cross(ab, ac);
        if (lengthSquared(normal) <= 0.000001f) {
            return new float[]{0.0f, 1.0f, 0.0f};
        }
        return normalize(normal);
    }

    private float[] normalize(final float x, final float y, final float z) {
        final float length = (float) Math.sqrt(x * x + y * y + z * z);
        if (length <= 0.0001f) {
            return new float[]{0.0f, 1.0f, 0.0f};
        }
        return new float[]{x / length, y / length, z / length};
    }

    private float[] normalize(final float[] vector) {
        return normalize(vector[0], vector[1], vector[2]);
    }

    private float[] cross(final float[] a, final float[] b) {
        return new float[]{
                a[1] * b[2] - a[2] * b[1],
                a[2] * b[0] - a[0] * b[2],
                a[0] * b[1] - a[1] * b[0]
        };
    }

    private float lengthSquared(final float[] vector) {
        return vector[0] * vector[0] + vector[1] * vector[1] + vector[2] * vector[2];
    }

    private float clampFloat(final float value, final float min, final float max) {
        return Math.max(min, Math.min(max, value));
    }

    private int clampInt(final int value, final int min, final int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class ChunkMeshBuilder {
        private final Map<BatchKey, FloatList> batches = new LinkedHashMap<>();

        private void addQuad(
                final int textureId,
                final float shade,
                final float normalX,
                final float normalY,
                final float normalZ,
                final float x1, final float y1, final float z1,
                final float x2, final float y2, final float z2,
                final float x3, final float y3, final float z3,
                final float x4, final float y4, final float z4
        ) {
            addQuad(textureId, RenderLayer.OPAQUE, shade, normalX, normalY, normalZ, new float[][]{
                    {x1, y1, z1},
                    {x2, y2, z2},
                    {x3, y3, z3},
                    {x4, y4, z4}
            }, new float[][]{
                    {0.0f, 0.0f},
                    {1.0f, 0.0f},
                    {1.0f, 1.0f},
                    {0.0f, 1.0f}
            });
        }

        private void addQuad(
                final int textureId,
                final RenderLayer layer,
                final float shade,
                final float normalX,
                final float normalY,
                final float normalZ,
                final float x1, final float y1, final float z1,
                final float x2, final float y2, final float z2,
                final float x3, final float y3, final float z3,
                final float x4, final float y4, final float z4
        ) {
            addQuad(textureId, layer, shade, normalX, normalY, normalZ, new float[][]{
                    {x1, y1, z1},
                    {x2, y2, z2},
                    {x3, y3, z3},
                    {x4, y4, z4}
            }, new float[][]{
                    {0.0f, 0.0f},
                    {1.0f, 0.0f},
                    {1.0f, 1.0f},
                    {0.0f, 1.0f}
            });
        }

        private void addFlatQuad(
                final int textureId,
                final float shade,
                final float x1, final float y1, final float z1,
                final float x2, final float y2, final float z2,
                final float x3, final float y3, final float z3,
                final float x4, final float y4, final float z4,
                final float u1, final float v1, final float u2, final float v2
        ) {
            addQuad(textureId, shade, 0.0f, 1.0f, 0.0f, new float[][]{
                    {x1, y1, z1},
                    {x2, y2, z2},
                    {x3, y3, z3},
                    {x4, y4, z4}
            }, new float[][]{
                    {u1, v1},
                    {u2, v1},
                    {u2, v2},
                    {u1, v2}
            });
        }

        private void addQuad(
                final int textureId,
                final float shade,
                final float normalX,
                final float normalY,
                final float normalZ,
                final float[][] vertices,
                final float[][] uvs
        ) {
            addQuad(textureId, RenderLayer.OPAQUE, shade, normalX, normalY, normalZ, vertices, uvs);
        }

        private void addQuad(
                final int textureId,
                final RenderLayer layer,
                final float shade,
                final float normalX,
                final float normalY,
                final float normalZ,
                final float[][] vertices,
                final float[][] uvs
        ) {
            if (textureId == 0 || vertices.length < 4 || uvs.length < 4) {
                return;
            }
            final FloatList data = batches.computeIfAbsent(new BatchKey(textureId, layer), ignored -> new FloatList());
            addTriangle(data, shade, normalX, normalY, normalZ, vertices, uvs, 0, 1, 2);
            addTriangle(data, shade, normalX, normalY, normalZ, vertices, uvs, 0, 2, 3);
        }

        private void addTriangle(
                final FloatList data,
                final float shade,
                final float normalX,
                final float normalY,
                final float normalZ,
                final float[][] vertices,
                final float[][] uvs,
                final int a,
                final int b,
                final int c
        ) {
            addVertex(data, shade, normalX, normalY, normalZ, vertices[a], uvs[a]);
            addVertex(data, shade, normalX, normalY, normalZ, vertices[b], uvs[b]);
            addVertex(data, shade, normalX, normalY, normalZ, vertices[c], uvs[c]);
        }

        private void addVertex(
                final FloatList data,
                final float shade,
                final float normalX,
                final float normalY,
                final float normalZ,
                final float[] vertex,
                final float[] uv
        ) {
            data.add(vertex[0]);
            data.add(vertex[1]);
            data.add(vertex[2]);
            data.add(normalX);
            data.add(normalY);
            data.add(normalZ);
            data.add(uv[0]);
            data.add(uv[1]);
            data.add(shade);
            data.add(shade);
            data.add(shade);
        }

        private ChunkMesh upload(final long revision) {
            final List<MeshBatch> uploaded = new ArrayList<>();
            for (final RenderLayer layer : RenderLayer.values()) {
                uploadBatches(uploaded, layer);
            }
            glBindBuffer(GL_ARRAY_BUFFER, 0);
            return new ChunkMesh(revision, uploaded);
        }

        private void uploadBatches(final List<MeshBatch> uploaded, final RenderLayer layer) {
            for (final Map.Entry<BatchKey, FloatList> entry : batches.entrySet()) {
                if (entry.getKey().layer() != layer) {
                    continue;
                }
                final FloatList data = entry.getValue();
                if (data.isEmpty()) {
                    continue;
                }

                final int vboId = glGenBuffers();
                if (vboId == 0) {
                    continue;
                }

                glBindBuffer(GL_ARRAY_BUFFER, vboId);
                glBufferData(GL_ARRAY_BUFFER, data.toBuffer(), GL_STATIC_DRAW);
                uploaded.add(new MeshBatch(entry.getKey().textureId(), entry.getKey().layer(), vboId, data.vertexCount()));
            }
        }
    }

    private record BatchKey(int textureId, RenderLayer layer) {
    }

    private enum RenderLayer {
        OPAQUE,
        DEPTH_EQUAL,
        TRANSLUCENT
    }

    private static final class FloatList {
        private float[] values = new float[1024];
        private int size;

        private void add(final float value) {
            if (size == values.length) {
                final float[] grown = new float[values.length * 2];
                System.arraycopy(values, 0, grown, 0, values.length);
                values = grown;
            }
            values[size++] = value;
        }

        private boolean isEmpty() {
            return size == 0;
        }

        private int vertexCount() {
            return size / VERTEX_FLOATS;
        }

        private FloatBuffer toBuffer() {
            final FloatBuffer buffer = BufferUtils.createFloatBuffer(size);
            buffer.put(values, 0, size);
            buffer.flip();
            return buffer;
        }
    }

    private record ChunkMesh(long builtRevision, List<MeshBatch> batches) {
        private void render(final RenderLayer layer) {
            for (final MeshBatch batch : batches) {
                if (batch.layer() != layer) {
                    continue;
                }
                batch.render();
            }
        }

        private void destroy() {
            for (final MeshBatch batch : batches) {
                batch.destroy();
            }
        }
    }

    private record MeshBatch(int textureId, RenderLayer layer, int vboId, int vertexCount) {
        private void render() {
            if (vboId == 0 || vertexCount <= 0) {
                return;
            }
            glDepthFunc(layer == RenderLayer.OPAQUE ? GL_LESS : GL_LEQUAL);
            glBindTexture(GL_TEXTURE_2D, textureId);
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glVertexPointer(3, GL_FLOAT, VERTEX_STRIDE_BYTES, 0L);
            glNormalPointer(GL_FLOAT, VERTEX_STRIDE_BYTES, NORMAL_OFFSET_BYTES);
            glTexCoordPointer(2, GL_FLOAT, VERTEX_STRIDE_BYTES, TEX_COORD_OFFSET_BYTES);
            glColorPointer(3, GL_FLOAT, VERTEX_STRIDE_BYTES, COLOR_OFFSET_BYTES);
            glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        }

        private void destroy() {
            if (vboId != 0) {
                glDeleteBuffers(vboId);
            }
        }
    }

    private enum Face {
        SIDE("side"),
        FRONT("front"),
        TOP("top"),
        BOTTOM("bottom");

        private final String key;

        Face(final String key) {
            this.key = key;
        }
    }

    private record BlockTextures(int side, int front, int top, int bottom, boolean directional) {
    }

    private record BlockModelKey(ResourceId blockId, BlockFacing facing) {
    }

    private record LightingState(float ambient, float sunDiffuse, float moonDiffuse, float[] sunDirection, float[] moonDirection) {
    }

    private record MinecraftBlockModel(List<ModelQuad> quads) {
        private void render(final float baseX, final float baseY, final float baseZ, final float blockSize) {
            boolean hasTintedQuads = false;
            for (final ModelQuad quad : quads) {
                if (quad.tinted()) {
                    hasTintedQuads = true;
                    continue;
                }
                renderQuad(quad, baseX, baseY, baseZ, blockSize);
            }
            if (!hasTintedQuads) {
                return;
            }

            glDepthFunc(GL_LEQUAL);
            for (final ModelQuad quad : quads) {
                if (quad.tinted()) {
                    renderQuad(quad, baseX, baseY, baseZ, blockSize);
                }
            }
            glDepthFunc(GL_LESS);
        }

        private static void renderQuad(final ModelQuad quad, final float baseX, final float baseY, final float baseZ, final float blockSize) {
            glBindTexture(GL_TEXTURE_2D, quad.textureId());
            setStaticShade(quad.shade());
            glNormal3f(quad.normal()[0], quad.normal()[1], quad.normal()[2]);
            glBegin(GL_QUADS);
            texVertex(quad.uvs()[0], quad.vertices()[0], baseX, baseY, baseZ, blockSize);
            texVertex(quad.uvs()[1], quad.vertices()[1], baseX, baseY, baseZ, blockSize);
            texVertex(quad.uvs()[2], quad.vertices()[2], baseX, baseY, baseZ, blockSize);
            texVertex(quad.uvs()[3], quad.vertices()[3], baseX, baseY, baseZ, blockSize);
            glEnd();
        }

        private static void texVertex(final float[] uv, final float[] vertex, final float baseX, final float baseY, final float baseZ, final float blockSize) {
            glTexCoord2f(uv[0], uv[1]);
            glVertex3f(
                    baseX + (vertex[0] / 16.0f) * blockSize,
                    baseY + (vertex[1] / 16.0f) * blockSize,
                    baseZ + (vertex[2] / 16.0f) * blockSize
            );
        }

        private static void setStaticShade(final float shade) {
            final float s = Math.max(0.0f, Math.min(1.0f, shade));
            glColor3f(s, s, s);
        }
    }

    private record ModelQuad(int textureId, float shade, float[][] uvs, float[][] vertices, float[] normal, boolean tinted, boolean cullable) {
    }

    private record ModelDefinition(String modelPath, float xRotation, float yRotation, float zRotation) {
    }

    private record ModelDefinitionGroup(List<ModelDefinition> definitions, int weight) {
    }

    private record ResolvedMinecraftModel(List<Object> elements, Map<String, Object> textures) {
    }

    private record ModelRotation(String axis, float angle, float[] origin) {
        private static final ModelRotation NONE = new ModelRotation("", 0.0f, new float[]{0.0f, 0.0f, 0.0f});
    }
}
