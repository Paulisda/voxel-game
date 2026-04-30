package de.paul.voxelgame.renderer;

import de.paul.voxelgame.GameConfig;
import de.paul.voxelgame.map.Block;
import de.paul.voxelgame.map.BlockType;
import de.paul.voxelgame.map.World;
import de.paul.voxelgame.mob.Player;

import static org.lwjgl.opengl.GL11.GL_COMPILE;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.glBegin;
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
import static org.lwjgl.opengl.GL11.glTranslatef;
import static org.lwjgl.opengl.GL11.glVertex3f;

public class WorldRenderer {
    private final World world;
    private int displayListId;
    private long builtRevision = -1;

    public WorldRenderer(World world) {
        this.world = world;
    }

    public void render(Player player, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }

        setupProjection(player.getFieldOfView(), width, height, 0.05, 1200.0);
        setupCamera(player);
        rebuildDisplayListIfNeeded();
        glCallList(displayListId);
    }

    public void destroy() {
        if (displayListId != 0) {
            glDeleteLists(displayListId, 1);
            displayListId = 0;
        }
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

        float[] base = baseColor(block.getType());
        float topShade = 1.0f;
        float sideShade = 0.85f;
        float bottomShade = 0.70f;

        glBegin(GL_QUADS);

        if (isFaceVisible(x, y, z + 1)) {
            setColor(base, sideShade);
            glVertex3f(minX, minY, maxZ);
            glVertex3f(maxX, minY, maxZ);
            glVertex3f(maxX, maxY, maxZ);
            glVertex3f(minX, maxY, maxZ);
        }
        if (isFaceVisible(x, y, z - 1)) {
            setColor(base, sideShade);
            glVertex3f(maxX, minY, minZ);
            glVertex3f(minX, minY, minZ);
            glVertex3f(minX, maxY, minZ);
            glVertex3f(maxX, maxY, minZ);
        }
        if (isFaceVisible(x + 1, y, z)) {
            setColor(base, sideShade);
            glVertex3f(maxX, minY, maxZ);
            glVertex3f(maxX, minY, minZ);
            glVertex3f(maxX, maxY, minZ);
            glVertex3f(maxX, maxY, maxZ);
        }
        if (isFaceVisible(x - 1, y, z)) {
            setColor(base, sideShade);
            glVertex3f(minX, minY, minZ);
            glVertex3f(minX, minY, maxZ);
            glVertex3f(minX, maxY, maxZ);
            glVertex3f(minX, maxY, minZ);
        }
        if (isFaceVisible(x, y + 1, z)) {
            setColor(base, topShade);
            glVertex3f(minX, maxY, maxZ);
            glVertex3f(maxX, maxY, maxZ);
            glVertex3f(maxX, maxY, minZ);
            glVertex3f(minX, maxY, minZ);
        }
        if (isFaceVisible(x, y - 1, z)) {
            setColor(base, bottomShade);
            glVertex3f(minX, minY, minZ);
            glVertex3f(maxX, minY, minZ);
            glVertex3f(maxX, minY, maxZ);
            glVertex3f(minX, minY, maxZ);
        }

        glEnd();
    }

    private boolean isFaceVisible(int neighborX, int neighborY, int neighborZ) {
        Block neighbor = world.getBlock(neighborX, neighborY, neighborZ);
        return neighbor == null || !neighbor.isSolid();
    }

    private float[] baseColor(BlockType type) {
        return switch (type) {
            case GRASS -> new float[]{0.40f, 0.73f, 0.29f};
            case DIRT -> new float[]{0.58f, 0.39f, 0.21f};
            case STONE -> new float[]{0.58f, 0.58f, 0.58f};
            case BEDROCK -> new float[]{0.29f, 0.29f, 0.29f};
            case WATER -> new float[]{0.25f, 0.50f, 0.87f};
            case WOOD -> new float[]{0.62f, 0.45f, 0.24f};
        };
    }

    private void setColor(float[] base, float shade) {
        glColor3f(
                clamp01(base[0] * shade),
                clamp01(base[1] * shade),
                clamp01(base[2] * shade)
        );
    }

    private float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
