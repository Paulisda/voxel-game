package de.paul.voxelgame.map;

import de.paul.voxelgame.GameConfig;
import de.paul.voxelgame.math.Vector3f;
import de.paul.voxelgame.objects.BlockComponent;
import de.paul.voxelgame.objects.GameObject;
import de.paul.voxelgame.objects.ObjectKind;
import de.paul.voxelgame.objects.RegistryManager;
import de.paul.voxelgame.objects.ResourceId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class World {
    private static final int IMMEDIATE_LOAD_RADIUS = 2;
    private static final int MAX_CHUNKS_GENERATED_PER_UPDATE = 18;
    private static final int UNLOAD_MARGIN_CHUNKS = 2;
    private static final int SPAWN_SEARCH_RADIUS_BLOCKS = 64;

    private final Map<Long, Chunk> chunks = new HashMap<>();
    private final RegistryManager registries;
    private long revision;

    public World(final RegistryManager registries) {
        this.registries = registries;
    }

    public void generateWorld(final boolean debugCollision) {
        chunks.clear();
        loadChunksAround(0, 0, IMMEDIATE_LOAD_RADIUS, Integer.MAX_VALUE);
        revision++;
    }

    public void updateLoadedChunks(final double centerWorldX, final double centerWorldZ, final int viewDistanceChunks) {
        final int centerChunkX = Math.floorDiv((int) Math.floor(centerWorldX), GameConfig.CHUNK_WIDTH);
        final int centerChunkZ = Math.floorDiv((int) Math.floor(centerWorldZ), GameConfig.CHUNK_DEPTH);
        final int clampedDistance = clampViewDistance(viewDistanceChunks);

        loadChunksAround(centerChunkX, centerChunkZ, Math.min(IMMEDIATE_LOAD_RADIUS, clampedDistance), Integer.MAX_VALUE);
        loadChunksAround(centerChunkX, centerChunkZ, clampedDistance, MAX_CHUNKS_GENERATED_PER_UPDATE);
        unloadChunksOutside(centerChunkX, centerChunkZ, clampedDistance + UNLOAD_MARGIN_CHUNKS);
    }

    private int loadChunksAround(final int centerChunkX, final int centerChunkZ, final int radius, final int maxNewChunks) {
        int loaded = 0;
        for (int ring = 0; ring <= radius; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) {
                        continue;
                    }
                    if (loaded >= maxNewChunks) {
                        return loaded;
                    }
                    if (addChunk(centerChunkX + dx, centerChunkZ + dz)) {
                        loaded++;
                    }
                }
            }
        }
        return loaded;
    }

    private boolean addChunk(final int chunkX, final int chunkZ) {
        if (!chunkIntersectsWorld(chunkX, chunkZ)) {
            return false;
        }
        final long key = chunkKey(chunkX, chunkZ);
        if (chunks.containsKey(key)) {
            return false;
        }
        chunks.put(key, new Chunk(chunkX, chunkZ, GameConfig.CHUNK_WIDTH, GameConfig.CHUNK_HEIGHT, GameConfig.CHUNK_DEPTH, registries));
        markAdjacentChunksDirty(chunkX, chunkZ);
        revision++;
        return true;
    }

    private void unloadChunksOutside(final int centerChunkX, final int centerChunkZ, final int keepRadius) {
        final List<Long> staleKeys = new ArrayList<>();
        for (final Map.Entry<Long, Chunk> entry : chunks.entrySet()) {
            final Chunk chunk = entry.getValue();
            final int distance = Math.max(Math.abs(chunk.getChunkX() - centerChunkX), Math.abs(chunk.getChunkZ() - centerChunkZ));
            if (distance > keepRadius || !chunkIntersectsWorld(chunk.getChunkX(), chunk.getChunkZ())) {
                staleKeys.add(entry.getKey());
            }
        }
        if (staleKeys.isEmpty()) {
            return;
        }
        for (final Long key : staleKeys) {
            chunks.remove(key);
        }
        revision++;
    }

    public Chunk getChunk(final int chunkX, final int chunkZ) {
        return chunks.get(chunkKey(chunkX, chunkZ));
    }

    public Collection<Chunk> getChunks() {
        return chunks.values();
    }

    public Block getBlock(final int blockX, final int blockY, final int blockZ) {
        if (!isInsideWorld(blockX, blockY, blockZ)) {
            return null;
        }

        final int chunkX = Math.floorDiv(blockX, GameConfig.CHUNK_WIDTH);
        final int chunkZ = Math.floorDiv(blockZ, GameConfig.CHUNK_DEPTH);
        final Chunk chunk = getChunk(chunkX, chunkZ);
        if (chunk == null) {
            return null;
        }

        final int localX = Math.floorMod(blockX, GameConfig.CHUNK_WIDTH);
        final int localZ = Math.floorMod(blockZ, GameConfig.CHUNK_DEPTH);
        return chunk.getBlock(localX, blockY, localZ);
    }

    public boolean isSolidCollisionBlock(final int blockX, final int blockY, final int blockZ) {
        if (blockY < 0) {
            return true;
        }
        if (blockY >= GameConfig.CHUNK_HEIGHT) {
            return false;
        }
        if (!GameConfig.isInsideWorld(blockX, blockZ)) {
            return true;
        }
        if (!hasLoadedChunkAt(blockX, blockZ)) {
            return true;
        }

        final Block block = getBlock(blockX, blockY, blockZ);
        return block != null && block.isSolid();
    }

    public boolean hasLoadedChunkAt(final int blockX, final int blockZ) {
        if (!GameConfig.isInsideWorld(blockX, blockZ)) {
            return false;
        }
        final int chunkX = Math.floorDiv(blockX, GameConfig.CHUNK_WIDTH);
        final int chunkZ = Math.floorDiv(blockZ, GameConfig.CHUNK_DEPTH);
        return getChunk(chunkX, chunkZ) != null;
    }

    public void setBlock(final int blockX, final int blockY, final int blockZ, final Block block) {
        if (!isInsideWorld(blockX, blockY, blockZ)) {
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
        markNeighborChunksDirty(chunkX, chunkZ, localX, localZ);
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
        return placeBlock(blockX, blockY, blockZ, type, BlockFacing.NORTH);
    }

    public boolean placeBlock(final int blockX, final int blockY, final int blockZ, final GameObject type, final BlockFacing facing) {
        if (!isPlaceableBlock(type) || !isInsideWorld(blockX, blockY, blockZ)) {
            return false;
        }
        if (getBlock(blockX, blockY, blockZ) != null) {
            return false;
        }
        setBlock(blockX, blockY, blockZ, new Block(blockX, blockY, blockZ, type, facing));
        return true;
    }

    public boolean placeBlock(final int blockX, final int blockY, final int blockZ, final ResourceId typeId) {
        return placeBlock(blockX, blockY, blockZ, typeId, BlockFacing.NORTH);
    }

    public boolean placeBlock(final int blockX, final int blockY, final int blockZ, final ResourceId typeId, final BlockFacing facing) {
        return registries.blocks()
                .find(typeId)
                .map(type -> placeBlock(blockX, blockY, blockZ, type, facing))
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
        if (!GameConfig.isInsideWorld(blockX, blockZ)) {
            return 0;
        }
        final int chunkX = Math.floorDiv(blockX, GameConfig.CHUNK_WIDTH);
        final int chunkZ = Math.floorDiv(blockZ, GameConfig.CHUNK_DEPTH);
        addChunk(chunkX, chunkZ);
        final Chunk chunk = getChunk(chunkX, chunkZ);
        if (chunk == null) {
            return 0;
        }
        final int localX = Math.floorMod(blockX, GameConfig.CHUNK_WIDTH);
        final int localZ = Math.floorMod(blockZ, GameConfig.CHUNK_DEPTH);
        return chunk.getSurfaceHeight(localX, localZ);
    }

    public Chunk.Biome getBiome(final int blockX, final int blockZ) {
        if (!GameConfig.isInsideWorld(blockX, blockZ)) {
            return Chunk.Biome.PLAINS;
        }
        final int chunkX = Math.floorDiv(blockX, GameConfig.CHUNK_WIDTH);
        final int chunkZ = Math.floorDiv(blockZ, GameConfig.CHUNK_DEPTH);
        final Chunk chunk = getChunk(chunkX, chunkZ);
        if (chunk == null) {
            return Chunk.Biome.PLAINS;
        }
        return chunk.getBiome(Math.floorMod(blockX, GameConfig.CHUNK_WIDTH), Math.floorMod(blockZ, GameConfig.CHUNK_DEPTH));
    }

    public Vector3f getSpawnPoint() {
        final SpawnCandidate spawn = findDrySpawnCandidate();
        final double blockSize = GameConfig.BLOCK_SIZE;
        return new Vector3f(
                (spawn.x() + 0.5) * blockSize,
                (spawn.surfaceY() + 1.05) * blockSize,
                (spawn.z() + 0.5) * blockSize
        );
    }

    private SpawnCandidate findDrySpawnCandidate() {
        for (int radius = 0; radius <= SPAWN_SEARCH_RADIUS_BLOCKS; radius++) {
            for (int x = -radius; x <= radius; x++) {
                final SpawnCandidate north = drySpawnCandidate(x, -radius);
                if (north != null) {
                    return north;
                }
                final SpawnCandidate south = drySpawnCandidate(x, radius);
                if (south != null) {
                    return south;
                }
            }
            for (int z = -radius + 1; z <= radius - 1; z++) {
                final SpawnCandidate west = drySpawnCandidate(-radius, z);
                if (west != null) {
                    return west;
                }
                final SpawnCandidate east = drySpawnCandidate(radius, z);
                if (east != null) {
                    return east;
                }
            }
        }

        return new SpawnCandidate(0, 0, Math.max(getSurfaceY(0, 0), GameConfig.SEA_LEVEL + 3));
    }

    private SpawnCandidate drySpawnCandidate(final int x, final int z) {
        final int surfaceY = getSurfaceY(x, z);
        final Chunk.Biome biome = getBiome(x, z);
        if (surfaceY <= GameConfig.SEA_LEVEL + 2 || biome == Chunk.Biome.OCEAN || biome == Chunk.Biome.BEACH) {
            return null;
        }
        return new SpawnCandidate(x, z, surfaceY);
    }

    public void forEachBlock(final Consumer<Block> consumer) {
        for (final Chunk chunk : chunks.values()) {
            chunk.forEachBlock(consumer);
        }
    }

    public long getRevision() {
        return revision;
    }

    private boolean isInsideWorld(final int blockX, final int blockY, final int blockZ) {
        return blockY >= 0 && blockY < GameConfig.CHUNK_HEIGHT && GameConfig.isInsideWorld(blockX, blockZ);
    }

    private int clampViewDistance(final int viewDistanceChunks) {
        return Math.max(GameConfig.MIN_VIEW_DISTANCE_CHUNKS, Math.min(GameConfig.MAX_VIEW_DISTANCE_CHUNKS, viewDistanceChunks));
    }

    private boolean chunkIntersectsWorld(final int chunkX, final int chunkZ) {
        final int minX = chunkX * GameConfig.CHUNK_WIDTH;
        final int minZ = chunkZ * GameConfig.CHUNK_DEPTH;
        final int maxX = minX + GameConfig.CHUNK_WIDTH - 1;
        final int maxZ = minZ + GameConfig.CHUNK_DEPTH - 1;
        return minX >= -GameConfig.WORLD_HALF_SIZE_BLOCKS
                && maxX < GameConfig.WORLD_HALF_SIZE_BLOCKS
                && minZ >= -GameConfig.WORLD_HALF_SIZE_BLOCKS
                && maxZ < GameConfig.WORLD_HALF_SIZE_BLOCKS;
    }

    private long chunkKey(final int x, final int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private record SpawnCandidate(int x, int z, int surfaceY) {
    }

    private void markNeighborChunksDirty(final int chunkX, final int chunkZ, final int localX, final int localZ) {
        if (localX == 0) {
            markChunkDirty(chunkX - 1, chunkZ);
        } else if (localX == GameConfig.CHUNK_WIDTH - 1) {
            markChunkDirty(chunkX + 1, chunkZ);
        }

        if (localZ == 0) {
            markChunkDirty(chunkX, chunkZ - 1);
        } else if (localZ == GameConfig.CHUNK_DEPTH - 1) {
            markChunkDirty(chunkX, chunkZ + 1);
        }
    }

    private void markAdjacentChunksDirty(final int chunkX, final int chunkZ) {
        markChunkDirty(chunkX - 1, chunkZ);
        markChunkDirty(chunkX + 1, chunkZ);
        markChunkDirty(chunkX, chunkZ - 1);
        markChunkDirty(chunkX, chunkZ + 1);
    }

    private void markChunkDirty(final int chunkX, final int chunkZ) {
        final Chunk chunk = getChunk(chunkX, chunkZ);
        if (chunk != null) {
            chunk.markDirty();
        }
    }
}
