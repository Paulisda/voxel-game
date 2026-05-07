package de.paul.voxelgame.map;

import de.paul.voxelgame.GameConfig;
import de.paul.voxelgame.math.Vector3f;
import de.paul.voxelgame.objects.BlockComponent;
import de.paul.voxelgame.objects.GameObject;
import de.paul.voxelgame.objects.ObjectKind;
import de.paul.voxelgame.objects.RegistryManager;
import de.paul.voxelgame.objects.ResourceId;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class World {
    private final Map<Long, Chunk> chunks = new HashMap<>();
    private final RegistryManager registries;
    private long revision;

    public World(final RegistryManager registries) {
        this.registries = registries;
    }

    public void generateWorld(final boolean debugCollision) {
        chunks.clear();

        for (int chunkX = -GameConfig.WORLD_CHUNK_RADIUS; chunkX <= GameConfig.WORLD_CHUNK_RADIUS; chunkX++) {
            for (int chunkZ = -GameConfig.WORLD_CHUNK_RADIUS; chunkZ <= GameConfig.WORLD_CHUNK_RADIUS; chunkZ++) {
                addChunk(chunkX, chunkZ);
            }
        }

        revision++;
    }

    private void addChunk(final int chunkX, final int chunkZ) {
        final long key = chunkKey(chunkX, chunkZ);
        if (!chunks.containsKey(key)) {
            chunks.put(key, new Chunk(chunkX, chunkZ, GameConfig.CHUNK_WIDTH, GameConfig.CHUNK_HEIGHT, GameConfig.CHUNK_DEPTH, registries));
        }
    }

    public Chunk getChunk(final int chunkX, final int chunkZ) {
        return chunks.get(chunkKey(chunkX, chunkZ));
    }

    public Collection<Chunk> getChunks() {
        return chunks.values();
    }

    public Block getBlock(final int blockX, final int blockY, final int blockZ) {
        if (blockY < 0 || blockY >= GameConfig.CHUNK_HEIGHT) {
            return null;
        }

        final int chunkX = Math.floorDiv(blockX, GameConfig.CHUNK_WIDTH);
        final int chunkZ = Math.floorDiv(blockZ, GameConfig.CHUNK_DEPTH);
        Chunk chunk = getChunk(chunkX, chunkZ);
        if (chunk == null) {
            return null;
        }

        final int localX = Math.floorMod(blockX, GameConfig.CHUNK_WIDTH);
        final int localZ = Math.floorMod(blockZ, GameConfig.CHUNK_DEPTH);
        return chunk.getBlock(localX, blockY, localZ);
    }

    public void setBlock(final int blockX, final int blockY, final int blockZ, final Block block) {
        if (blockY < 0 || blockY >= GameConfig.CHUNK_HEIGHT) {
            return;
        }

        final int chunkX = Math.floorDiv(blockX, GameConfig.CHUNK_WIDTH);
        final int chunkZ = Math.floorDiv(blockZ, GameConfig.CHUNK_DEPTH);
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

        final int localX = Math.floorMod(blockX, GameConfig.CHUNK_WIDTH);
        final int localZ = Math.floorMod(blockZ, GameConfig.CHUNK_DEPTH);
        chunk.setBlock(localX, blockY, localZ, block);
        revision++;
    }

    public boolean removeBlock(final int blockX, final int blockY, final int blockZ) {
        final Block existing = getBlock(blockX, blockY, blockZ);
        if (existing == null || !isBreakable(existing)) {
            return false;
        }
        setBlock(blockX, blockY, blockZ, null);
        return true;
    }

    public boolean placeBlock(final int blockX, final int blockY, final int blockZ, final GameObject type) {
        if (!isPlaceableBlock(type) || blockY < 0 || blockY >= GameConfig.CHUNK_HEIGHT) {
            return false;
        }
        if (getBlock(blockX, blockY, blockZ) != null) {
            return false;
        }
        setBlock(blockX, blockY, blockZ, new Block(blockX, blockY, blockZ, type));
        return true;
    }

    public boolean placeBlock(final int blockX, final int blockY, final int blockZ, final ResourceId typeId) {
        return registries.blocks()
                .find(typeId)
                .map(type -> placeBlock(blockX, blockY, blockZ, type))
                .orElse(false);
    }

    private boolean isPlaceableBlock(final GameObject type) {
        return type != null && type.kind() == ObjectKind.BLOCK && type.has(BlockComponent.class);
    }

    private boolean isBreakable(final Block block) {
        final GameObject type = block.getType();
        return type != null && type.has(BlockComponent.class) && type.get(BlockComponent.class).breakable();
    }

    public int getSurfaceY(final int blockX, final int blockZ) {
        for (int y = GameConfig.CHUNK_HEIGHT - 1; y >= 0; y--) {
            final Block block = getBlock(blockX, y, blockZ);
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

        final int surfaceY = highestY >= 0 ? highestY : getSurfaceY(0, 0);
        final double blockSize = GameConfig.BLOCK_SIZE;
        return new Vector3f(
                (spawnX + 0.5) * blockSize,
                (surfaceY + 1.05) * blockSize,
                (spawnZ + 0.5) * blockSize
        );
    }

    public void forEachBlock(final Consumer<Block> consumer) {
        for (final Chunk chunk : chunks.values()) {
            chunk.forEachBlock(consumer);
        }
    }

    public long getRevision() {
        return revision;
    }

    private long chunkKey(final int x, final int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }
}
