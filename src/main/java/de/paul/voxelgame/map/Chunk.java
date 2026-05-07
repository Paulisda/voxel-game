package de.paul.voxelgame.map;

import de.paul.voxelgame.GameConfig;
import de.paul.voxelgame.objects.GameObject;
import de.paul.voxelgame.objects.RegistryManager;
import de.paul.voxelgame.objects.ResourceId;

import java.util.function.Consumer;

public class Chunk {
    private final Block[][][] blocks;
    private final RegistryManager registries;
    private final int chunkX;
    private final int chunkZ;
    private final int width;
    private final int height;
    private final int depth;
    private final GameObject bedrock;
    private final GameObject grassBlock;
    private final GameObject dirt;
    private final GameObject stone;

    public Chunk(final int chunkX, final int chunkZ, final int width, final int height, final int depth, final RegistryManager registries) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.registries = registries;
        this.bedrock = block("game:bedrock");
        this.grassBlock = block("game:grass_block");
        this.dirt = block("game:dirt");
        this.stone = block("game:stone");
        this.blocks = new Block[width][height][depth];
        generateChunk();
    }

    private void generateChunk() {
        for (int localX = 0; localX < width; localX++) {
            for (int localZ = 0; localZ < depth; localZ++) {
                final int worldX = chunkX * width + localX;
                final int worldZ = chunkZ * depth + localZ;
                final int surfaceY = getSurfaceHeight(worldX, worldZ);

                for (int y = 0; y <= surfaceY; y++) {
                    final GameObject type = getTerrainBlock(y, surfaceY);
                    blocks[localX][y][localZ] = new Block(worldX, y, worldZ, type);
                }
            }
        }
    }

    private int getSurfaceHeight(final int worldX, final int worldZ) {
        final double terrainNoise = Math.sin(worldX * 0.28) * 1.8
                + Math.cos(worldZ * 0.22) * 1.6
                + Math.sin((worldX + worldZ) * 0.1) * 1.3;
        final int baseHeight = (int) (GameConfig.CHUNK_HEIGHT * 0.35);
        final int surface = baseHeight + (int) Math.round(terrainNoise);
        return Math.max(3, Math.min(height - 2, surface));
    }

    private GameObject getTerrainBlock(final int y, final int surfaceY) {
        if (y == 0) {
            return bedrock;
        }
        if (y == surfaceY) {
            return grassBlock;
        }
        if (y >= surfaceY - 2) {
            return dirt;
        }
        return stone;
    }

    private GameObject block(final String id) {
        return registries.blocks().get(ResourceId.of(id));
    }

    public Block getBlock(final int localX, final int localY, final int localZ) {
        if (!isInside(localX, localY, localZ)) {
            return null;
        }
        return blocks[localX][localY][localZ];
    }

    public void setBlock(final int localX, final int localY, final int localZ, final Block block) {
        if (!isInside(localX, localY, localZ)) {
            return;
        }
        blocks[localX][localY][localZ] = block;
    }

    public void forEachBlock(final Consumer<Block> consumer) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < depth; z++) {
                    final Block block = blocks[x][y][z];
                    if (block != null) {
                        consumer.accept(block);
                    }
                }
            }
        }
    }

    private boolean isInside(final int localX, final int localY, final int localZ) {
        return localX >= 0 && localX < width
                && localY >= 0 && localY < height
                && localZ >= 0 && localZ < depth;
    }
}
