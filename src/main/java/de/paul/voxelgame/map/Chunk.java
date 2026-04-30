package de.paul.voxelgame.map;

import de.paul.voxelgame.GameConfig;

import java.util.function.Consumer;

public class Chunk {
    private final Block[][][] blocks;
    private final int chunkX;
    private final int chunkZ;
    private final int width;
    private final int height;
    private final int depth;

    public Chunk(int chunkX, int chunkZ, int width, int height, int depth) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.blocks = new Block[width][height][depth];
        generateChunk();
    }

    private void generateChunk() {
        for (int localX = 0; localX < width; localX++) {
            for (int localZ = 0; localZ < depth; localZ++) {
                int worldX = chunkX * width + localX;
                int worldZ = chunkZ * depth + localZ;
                int surfaceY = getSurfaceHeight(worldX, worldZ);

                for (int y = 0; y <= surfaceY; y++) {
                    BlockType type = getBlockType(y, surfaceY);
                    blocks[localX][y][localZ] = new Block(worldX, y, worldZ, type);
                }
            }
        }
    }

    private int getSurfaceHeight(int worldX, int worldZ) {
        double terrainNoise = Math.sin(worldX * 0.28) * 1.8
                + Math.cos(worldZ * 0.22) * 1.6
                + Math.sin((worldX + worldZ) * 0.1) * 1.3;
        int baseHeight = (int) (GameConfig.CHUNK_HEIGHT * 0.35);
        int surface = baseHeight + (int) Math.round(terrainNoise);
        return Math.max(3, Math.min(height - 2, surface));
    }

    private BlockType getBlockType(int y, int surfaceY) {
        if (y == 0) {
            return BlockType.BEDROCK;
        }
        if (y == surfaceY) {
            return BlockType.GRASS;
        }
        if (y >= surfaceY - 2) {
            return BlockType.DIRT;
        }
        return BlockType.STONE;
    }

    public Block getBlock(int localX, int localY, int localZ) {
        if (!isInside(localX, localY, localZ)) {
            return null;
        }
        return blocks[localX][localY][localZ];
    }

    public void setBlock(int localX, int localY, int localZ, Block block) {
        if (!isInside(localX, localY, localZ)) {
            return;
        }
        blocks[localX][localY][localZ] = block;
    }

    public void forEachBlock(Consumer<Block> consumer) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < depth; z++) {
                    Block block = blocks[x][y][z];
                    if (block != null) {
                        consumer.accept(block);
                    }
                }
            }
        }
    }

    private boolean isInside(int localX, int localY, int localZ) {
        return localX >= 0 && localX < width
                && localY >= 0 && localY < height
                && localZ >= 0 && localZ < depth;
    }
}
