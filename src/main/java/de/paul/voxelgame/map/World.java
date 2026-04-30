package de.paul.voxelgame.map;

import de.paul.voxelgame.GameConfig;
import de.paul.voxelgame.math.Vector3f;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class World {
    private final Map<Long, Chunk> chunks = new HashMap<>();
    private long revision;

    public void generateWorld(boolean debugCollision) {
        chunks.clear();

        for (int chunkX = -GameConfig.WORLD_CHUNK_RADIUS; chunkX <= GameConfig.WORLD_CHUNK_RADIUS; chunkX++) {
            for (int chunkZ = -GameConfig.WORLD_CHUNK_RADIUS; chunkZ <= GameConfig.WORLD_CHUNK_RADIUS; chunkZ++) {
                addChunk(chunkX, chunkZ);
            }
        }

        revision++;
    }

    private void addChunk(int chunkX, int chunkZ) {
        long key = chunkKey(chunkX, chunkZ);
        if (!chunks.containsKey(key)) {
            chunks.put(key, new Chunk(chunkX, chunkZ, GameConfig.CHUNK_WIDTH, GameConfig.CHUNK_HEIGHT, GameConfig.CHUNK_DEPTH));
        }
    }

    public Chunk getChunk(int chunkX, int chunkZ) {
        return chunks.get(chunkKey(chunkX, chunkZ));
    }

    public Collection<Chunk> getChunks() {
        return chunks.values();
    }

    public Block getBlock(int blockX, int blockY, int blockZ) {
        if (blockY < 0 || blockY >= GameConfig.CHUNK_HEIGHT) {
            return null;
        }

        int chunkX = Math.floorDiv(blockX, GameConfig.CHUNK_WIDTH);
        int chunkZ = Math.floorDiv(blockZ, GameConfig.CHUNK_DEPTH);
        Chunk chunk = getChunk(chunkX, chunkZ);
        if (chunk == null) {
            return null;
        }

        int localX = Math.floorMod(blockX, GameConfig.CHUNK_WIDTH);
        int localZ = Math.floorMod(blockZ, GameConfig.CHUNK_DEPTH);
        return chunk.getBlock(localX, blockY, localZ);
    }

    public void setBlock(int blockX, int blockY, int blockZ, Block block) {
        if (blockY < 0 || blockY >= GameConfig.CHUNK_HEIGHT) {
            return;
        }

        int chunkX = Math.floorDiv(blockX, GameConfig.CHUNK_WIDTH);
        int chunkZ = Math.floorDiv(blockZ, GameConfig.CHUNK_DEPTH);
        Chunk chunk = getChunk(chunkX, chunkZ);
        if (chunk == null) {
            if (block == null) {
                return;
            }
            addChunk(chunkX, chunkZ);
            chunk = getChunk(chunkX, chunkZ);
            if (chunk == null) {
                return;
            }
        }

        int localX = Math.floorMod(blockX, GameConfig.CHUNK_WIDTH);
        int localZ = Math.floorMod(blockZ, GameConfig.CHUNK_DEPTH);
        chunk.setBlock(localX, blockY, localZ, block);
        revision++;
    }

    public boolean removeBlock(int blockX, int blockY, int blockZ) {
        Block existing = getBlock(blockX, blockY, blockZ);
        if (existing == null || existing.getType() == BlockType.BEDROCK) {
            return false;
        }
        setBlock(blockX, blockY, blockZ, null);
        return true;
    }

    public boolean placeBlock(int blockX, int blockY, int blockZ, BlockType type) {
        if (type == null || blockY < 0 || blockY >= GameConfig.CHUNK_HEIGHT) {
            return false;
        }
        if (getBlock(blockX, blockY, blockZ) != null) {
            return false;
        }
        setBlock(blockX, blockY, blockZ, new Block(blockX, blockY, blockZ, type));
        return true;
    }

    public int getSurfaceY(int blockX, int blockZ) {
        for (int y = GameConfig.CHUNK_HEIGHT - 1; y >= 0; y--) {
            Block block = getBlock(blockX, y, blockZ);
            if (block != null && block.isSolid()) {
                return y;
            }
        }
        return 0;
    }

    public Vector3f getSpawnPoint() {
        int spawnX = 0;
        int spawnZ = 0;
        int highestY = -1;

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                int surfaceY = getSurfaceY(x, z);
                if (surfaceY > highestY) {
                    highestY = surfaceY;
                    spawnX = x;
                    spawnZ = z;
                }
            }
        }

        int surfaceY = highestY >= 0 ? highestY : getSurfaceY(0, 0);
        double blockSize = GameConfig.BLOCK_SIZE;
        return new Vector3f(
                (spawnX + 0.5) * blockSize,
                (surfaceY + 1.05) * blockSize,
                (spawnZ + 0.5) * blockSize
        );
    }

    public void forEachBlock(Consumer<Block> consumer) {
        for (Chunk chunk : chunks.values()) {
            chunk.forEachBlock(consumer);
        }
    }

    public long getRevision() {
        return revision;
    }

    private long chunkKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }
}
