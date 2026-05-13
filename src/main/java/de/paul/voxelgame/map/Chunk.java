package de.paul.voxelgame.map;

import de.paul.voxelgame.GameConfig;
import de.paul.voxelgame.objects.GameObject;
import de.paul.voxelgame.objects.RegistryManager;
import de.paul.voxelgame.objects.ResourceId;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class Chunk {
    private final RegistryManager registries;
    private final int chunkX;
    private final int chunkZ;
    private final int width;
    private final int height;
    private final int depth;
    private final int[][] surfaceHeights;
    private final Biome[][] biomes;
    private final Map<Integer, BlockState> generatedFeatures = new HashMap<>();
    private final Map<Integer, BlockState> overrides = new HashMap<>();
    private final GameObject bedrock;
    private final GameObject grassBlock;
    private final GameObject dirt;
    private final GameObject stone;
    private final GameObject sand;
    private final GameObject water;
    private final GameObject grass;
    private final GameObject oakLog;
    private final GameObject oakLeaves;
    private final GameObject birchLog;
    private final GameObject birchLeaves;
    private long revision;

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
        this.sand = blockOr("game:sand", dirt);
        this.water = blockOr("game:water", null);
        this.grass = blockOr("game:grass", null);
        this.oakLog = blockOr("game:oak_log", null);
        this.oakLeaves = blockOr("game:oak_leaves", null);
        this.birchLog = blockOr("game:birch_log", null);
        this.birchLeaves = blockOr("game:birch_leaves", null);
        this.surfaceHeights = new int[width][depth];
        this.biomes = new Biome[width][depth];
        generateChunk();
    }

    private void generateChunk() {
        for (int localX = 0; localX < width; localX++) {
            for (int localZ = 0; localZ < depth; localZ++) {
                final int worldX = toWorldX(localX);
                final int worldZ = toWorldZ(localZ);
                final int surfaceY = computeSurfaceHeight(worldX, worldZ);
                surfaceHeights[localX][localZ] = surfaceY;
                biomes[localX][localZ] = selectBiome(worldX, worldZ, surfaceY);
            }
        }
        generateVegetation();
    }

    private int computeSurfaceHeight(final int worldX, final int worldZ) {
        final double continents = octaveNoise(worldX * 0.0012, worldZ * 0.0012, 4);
        final double hills = octaveNoise(worldX * 0.0075 + 139.4, worldZ * 0.0075 - 54.1, 3);
        final double detail = octaveNoise(worldX * 0.035 - 23.7, worldZ * 0.035 + 84.9, 2);
        final double oceanBias = continents < -0.34 ? -16.0 : 0.0;
        final int surface = (int) Math.round(GameConfig.SEA_LEVEL + continents * 24.0 + hills * 10.0 + detail * 2.5 + oceanBias);
        final int spawnSafeSurface = applySpawnLandBias(worldX, worldZ, surface);
        return clamp(spawnSafeSurface, 8, height - 8);
    }

    private int applySpawnLandBias(final int worldX, final int worldZ, final int surface) {
        final int distanceFromSpawn = Math.max(Math.abs(worldX), Math.abs(worldZ));
        if (distanceFromSpawn > 96) {
            return surface;
        }
        if (distanceFromSpawn <= 32) {
            return Math.max(surface, GameConfig.SEA_LEVEL + 7);
        }

        final double falloff = 1.0 - ((distanceFromSpawn - 32) / 64.0);
        final int minimumSurface = GameConfig.SEA_LEVEL + 3 + (int) Math.round(falloff * 4.0);
        return Math.max(surface, minimumSurface);
    }

    private Biome selectBiome(final int worldX, final int worldZ, final int surfaceY) {
        if (surfaceY < GameConfig.SEA_LEVEL - 3) {
            return Biome.OCEAN;
        }
        if (surfaceY <= GameConfig.SEA_LEVEL + 2) {
            return Biome.BEACH;
        }

        final double moisture = octaveNoise(worldX * 0.0028 + 300.0, worldZ * 0.0028 - 90.0, 3);
        final double temperature = octaveNoise(worldX * 0.0021 - 170.0, worldZ * 0.0021 + 250.0, 3);
        if (moisture > 0.34) {
            return temperature < -0.12 ? Biome.BIRCH_FOREST : Biome.FOREST;
        }
        return Biome.PLAINS;
    }

    private void generateVegetation() {
        for (int localX = 0; localX < width; localX++) {
            for (int localZ = 0; localZ < depth; localZ++) {
                final int surfaceY = surfaceHeights[localX][localZ];
                if (surfaceY < GameConfig.SEA_LEVEL || surfaceY + 8 >= height) {
                    continue;
                }

                final Biome biome = biomes[localX][localZ];
                final int worldX = toWorldX(localX);
                final int worldZ = toWorldZ(localZ);
                if (biome == Biome.PLAINS) {
                    generateGrassPatch(localX, surfaceY, localZ, worldX, worldZ, 0.20);
                    maybeGenerateTree(localX, surfaceY, localZ, worldX, worldZ, TreeKind.OAK, 0.0026);
                } else if (biome == Biome.FOREST) {
                    generateGrassPatch(localX, surfaceY, localZ, worldX, worldZ, 0.10);
                    final TreeKind kind = random01(worldX, worldZ, 61) < 0.72 ? TreeKind.OAK : TreeKind.BIRCH;
                    maybeGenerateTree(localX, surfaceY, localZ, worldX, worldZ, kind, 0.010);
                } else if (biome == Biome.BIRCH_FOREST) {
                    generateGrassPatch(localX, surfaceY, localZ, worldX, worldZ, 0.08);
                    maybeGenerateTree(localX, surfaceY, localZ, worldX, worldZ, TreeKind.BIRCH, 0.012);
                }
            }
        }
    }

    private void generateGrassPatch(final int localX, final int surfaceY, final int localZ, final int worldX, final int worldZ, final double chance) {
        if (grass == null || random01(worldX, worldZ, 7) >= chance) {
            return;
        }
        putFeature(localX, surfaceY + 1, localZ, grass);
    }

    private void maybeGenerateTree(
            final int localX,
            final int surfaceY,
            final int localZ,
            final int worldX,
            final int worldZ,
            final TreeKind kind,
            final double chance
    ) {
        if (random01(worldX, worldZ, 19) >= chance || localX < 2 || localX > width - 3 || localZ < 2 || localZ > depth - 3) {
            return;
        }
        if (kind == TreeKind.OAK && (oakLog == null || oakLeaves == null)) {
            return;
        }
        if (kind == TreeKind.BIRCH && (birchLog == null || birchLeaves == null)) {
            return;
        }

        final boolean tallVariant = random01(worldX, worldZ, 23) > 0.55;
        final int trunkHeight = kind == TreeKind.BIRCH
                ? (tallVariant ? 6 : 5)
                : (tallVariant ? 5 : 4);
        final GameObject log = kind == TreeKind.BIRCH ? birchLog : oakLog;
        final GameObject leaves = kind == TreeKind.BIRCH ? birchLeaves : oakLeaves;

        generatedFeatures.remove(localKey(localX, surfaceY + 1, localZ));
        for (int y = 1; y <= trunkHeight; y++) {
            putFeature(localX, surfaceY + y, localZ, log);
        }

        if (kind == TreeKind.BIRCH) {
            generateBirchCrown(localX, surfaceY + trunkHeight, localZ, leaves);
        } else if (tallVariant) {
            generateTallOakCrown(localX, surfaceY + trunkHeight, localZ, leaves);
        } else {
            generateRoundOakCrown(localX, surfaceY + trunkHeight, localZ, leaves);
        }
    }

    private void generateRoundOakCrown(final int centerX, final int topY, final int centerZ, final GameObject leaves) {
        for (int y = topY - 2; y <= topY; y++) {
            final int radius = y == topY ? 1 : 2;
            placeLeafDisk(centerX, y, centerZ, radius, leaves);
        }
        placeLeafDisk(centerX, topY + 1, centerZ, 1, leaves);
    }

    private void generateTallOakCrown(final int centerX, final int topY, final int centerZ, final GameObject leaves) {
        for (int y = topY - 3; y <= topY - 1; y++) {
            placeLeafDisk(centerX, y, centerZ, 2, leaves);
        }
        placeLeafDisk(centerX, topY, centerZ, 1, leaves);
        putFeature(centerX, topY + 1, centerZ, leaves);
    }

    private void generateBirchCrown(final int centerX, final int topY, final int centerZ, final GameObject leaves) {
        placeLeafDisk(centerX, topY - 2, centerZ, 2, leaves);
        placeLeafDisk(centerX, topY - 1, centerZ, 2, leaves);
        placeLeafDisk(centerX, topY, centerZ, 1, leaves);
        putFeature(centerX, topY + 1, centerZ, leaves);
    }

    private void placeLeafDisk(final int centerX, final int y, final int centerZ, final int radius, final GameObject leaves) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (Math.abs(dx) == radius && Math.abs(dz) == radius && random01(toWorldX(centerX + dx), toWorldZ(centerZ + dz), y) < 0.35) {
                    continue;
                }
                putFeature(centerX + dx, y, centerZ + dz, leaves);
            }
        }
    }

    private void putFeature(final int localX, final int localY, final int localZ, final GameObject type) {
        if (!isInside(localX, localY, localZ) || type == null) {
            return;
        }
        final int key = localKey(localX, localY, localZ);
        generatedFeatures.putIfAbsent(key, new BlockState(type, BlockFacing.NORTH));
    }

    public Block getBlock(final int localX, final int localY, final int localZ) {
        if (!isInside(localX, localY, localZ)) {
            return null;
        }
        final BlockState state = blockStateAt(localX, localY, localZ);
        if (state == null || state.type() == null) {
            return null;
        }
        return new Block(toWorldX(localX), localY, toWorldZ(localZ), state.type(), state.facing());
    }

    private BlockState blockStateAt(final int localX, final int localY, final int localZ) {
        final int key = localKey(localX, localY, localZ);
        final BlockState override = overrides.get(key);
        if (override != null) {
            return override;
        }
        final BlockState feature = generatedFeatures.get(key);
        if (feature != null) {
            return feature;
        }
        final GameObject terrain = terrainBlockAt(localX, localY, localZ);
        return terrain == null ? null : new BlockState(terrain, BlockFacing.NORTH);
    }

    private GameObject terrainBlockAt(final int localX, final int localY, final int localZ) {
        final int surfaceY = surfaceHeights[localX][localZ];
        if (localY == 0) {
            return bedrock;
        }
        if (localY > surfaceY) {
            return localY <= GameConfig.SEA_LEVEL ? water : null;
        }
        if (localY == surfaceY) {
            return surfaceBlock(localX, localZ, surfaceY);
        }
        if (localY >= surfaceY - 3) {
            return surfaceY <= GameConfig.SEA_LEVEL + 2 ? sand : dirt;
        }
        return stone;
    }

    private GameObject surfaceBlock(final int localX, final int localZ, final int surfaceY) {
        final Biome biome = biomes[localX][localZ];
        if (surfaceY <= GameConfig.SEA_LEVEL + 2 || biome == Biome.BEACH || biome == Biome.OCEAN) {
            return sand;
        }
        return grassBlock;
    }

    public void setBlock(final int localX, final int localY, final int localZ, final Block block) {
        if (!isInside(localX, localY, localZ)) {
            return;
        }
        final int key = localKey(localX, localY, localZ);
        if (block == null) {
            overrides.put(key, BlockState.AIR);
        } else {
            overrides.put(key, new BlockState(block.getType(), block.getFacing()));
        }
        revision++;
    }

    public void forEachBlock(final Consumer<Block> consumer) {
        final Set<Integer> emitted = new HashSet<>();
        emitTerrainShell(consumer, emitted);
        emitFeatureBlocks(consumer, emitted);
        emitOverrideNeighborhoods(consumer, emitted);
    }

    private void emitTerrainShell(final Consumer<Block> consumer, final Set<Integer> emitted) {
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                final int surfaceY = surfaceHeights[x][z];
                final int bottomY = visibleTerrainBottom(x, z, surfaceY);
                for (int y = bottomY; y <= surfaceY; y++) {
                    emitBlock(x, y, z, consumer, emitted);
                }
                if (surfaceY < GameConfig.SEA_LEVEL && water != null) {
                    emitBlock(x, GameConfig.SEA_LEVEL, z, consumer, emitted);
                }
            }
        }
    }

    private int visibleTerrainBottom(final int localX, final int localZ, final int surfaceY) {
        int lowestNeighbor = surfaceY;
        boolean touchesWaterColumn = false;

        final int[][] neighbors = {
                {localX - 1, localZ},
                {localX + 1, localZ},
                {localX, localZ - 1},
                {localX, localZ + 1}
        };
        for (final int[] neighbor : neighbors) {
            final int neighborSurface = surfaceHeightAt(neighbor[0], neighbor[1]);
            lowestNeighbor = Math.min(lowestNeighbor, neighborSurface);
            if (neighborSurface < GameConfig.SEA_LEVEL) {
                touchesWaterColumn = true;
            }
        }

        int bottom = clamp(Math.min(surfaceY, lowestNeighbor + 1), 0, surfaceY);
        if (touchesWaterColumn && surfaceY > GameConfig.SEA_LEVEL) {
            bottom = Math.min(bottom, GameConfig.SEA_LEVEL);
        }
        return clamp(bottom, 0, surfaceY);
    }

    private int surfaceHeightAt(final int localX, final int localZ) {
        if (localX >= 0 && localX < width && localZ >= 0 && localZ < depth) {
            return surfaceHeights[localX][localZ];
        }
        return computeSurfaceHeight(toWorldX(localX), toWorldZ(localZ));
    }

    private void emitFeatureBlocks(final Consumer<Block> consumer, final Set<Integer> emitted) {
        for (final Integer key : generatedFeatures.keySet()) {
            final int[] pos = unpackLocalKey(key);
            emitBlock(pos[0], pos[1], pos[2], consumer, emitted);
        }
    }

    private void emitOverrideNeighborhoods(final Consumer<Block> consumer, final Set<Integer> emitted) {
        for (final Integer key : overrides.keySet()) {
            final int[] pos = unpackLocalKey(key);
            emitBlock(pos[0], pos[1], pos[2], consumer, emitted);
            emitBlock(pos[0] + 1, pos[1], pos[2], consumer, emitted);
            emitBlock(pos[0] - 1, pos[1], pos[2], consumer, emitted);
            emitBlock(pos[0], pos[1] + 1, pos[2], consumer, emitted);
            emitBlock(pos[0], pos[1] - 1, pos[2], consumer, emitted);
            emitBlock(pos[0], pos[1], pos[2] + 1, consumer, emitted);
            emitBlock(pos[0], pos[1], pos[2] - 1, consumer, emitted);
        }
    }

    private void emitBlock(final int localX, final int localY, final int localZ, final Consumer<Block> consumer, final Set<Integer> emitted) {
        if (!isInside(localX, localY, localZ)) {
            return;
        }
        final int key = localKey(localX, localY, localZ);
        if (!emitted.add(key)) {
            return;
        }
        final Block block = getBlock(localX, localY, localZ);
        if (block != null) {
            consumer.accept(block);
        }
    }

    private boolean isInside(final int localX, final int localY, final int localZ) {
        return localX >= 0 && localX < width
                && localY >= 0 && localY < height
                && localZ >= 0 && localZ < depth;
    }

    public void markDirty() {
        revision++;
    }

    public long getRevision() {
        return revision;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public int getSurfaceHeight(final int localX, final int localZ) {
        if (localX < 0 || localX >= width || localZ < 0 || localZ >= depth) {
            return 0;
        }
        return surfaceHeights[localX][localZ];
    }

    public Biome getBiome(final int localX, final int localZ) {
        if (localX < 0 || localX >= width || localZ < 0 || localZ >= depth) {
            return Biome.PLAINS;
        }
        return biomes[localX][localZ];
    }

    private int toWorldX(final int localX) {
        return chunkX * width + localX;
    }

    private int toWorldZ(final int localZ) {
        return chunkZ * depth + localZ;
    }

    private GameObject block(final String id) {
        return registries.blocks().get(ResourceId.of(id));
    }

    private GameObject blockOr(final String id, final GameObject fallback) {
        return registries.blocks().find(ResourceId.of(id)).orElse(fallback);
    }

    private int localKey(final int localX, final int localY, final int localZ) {
        return (localY << 8) | (localZ << 4) | localX;
    }

    private int[] unpackLocalKey(final int key) {
        return new int[]{
                key & 0xf,
                (key >>> 8) & 0xff,
                (key >>> 4) & 0xf
        };
    }

    private double octaveNoise(final double x, final double z, final int octaves) {
        double total = 0.0;
        double amplitude = 1.0;
        double frequency = 1.0;
        double max = 0.0;
        for (int i = 0; i < octaves; i++) {
            total += smoothNoise(x * frequency, z * frequency) * amplitude;
            max += amplitude;
            amplitude *= 0.5;
            frequency *= 2.0;
        }
        return max <= 0.0 ? 0.0 : total / max;
    }

    private double smoothNoise(final double x, final double z) {
        final int x0 = (int) Math.floor(x);
        final int z0 = (int) Math.floor(z);
        final double fx = x - x0;
        final double fz = z - z0;
        final double sx = smoothStep(fx);
        final double sz = smoothStep(fz);
        final double n00 = latticeNoise(x0, z0);
        final double n10 = latticeNoise(x0 + 1, z0);
        final double n01 = latticeNoise(x0, z0 + 1);
        final double n11 = latticeNoise(x0 + 1, z0 + 1);
        final double nx0 = lerp(n00, n10, sx);
        final double nx1 = lerp(n01, n11, sx);
        return lerp(nx0, nx1, sz);
    }

    private double latticeNoise(final int x, final int z) {
        return ((hash(x, z, 0) & 0xffff) / 32767.5) - 1.0;
    }

    private double random01(final int x, final int z, final int salt) {
        return (hash(x, z, salt) & 0xfffffff) / (double) 0x10000000;
    }

    private int hash(final int x, final int z, final int salt) {
        int h = x * 73428767 ^ z * 912931 ^ salt * 42349;
        h ^= h >>> 13;
        h *= 1274126177;
        h ^= h >>> 16;
        return h;
    }

    private double smoothStep(final double t) {
        return t * t * (3.0 - 2.0 * t);
    }

    private double lerp(final double a, final double b, final double t) {
        return a + (b - a) * t;
    }

    private int clamp(final int value, final int min, final int max) {
        return Math.max(min, Math.min(max, value));
    }

    public enum Biome {
        PLAINS,
        FOREST,
        BIRCH_FOREST,
        BEACH,
        OCEAN
    }

    private enum TreeKind {
        OAK,
        BIRCH
    }

    private record BlockState(GameObject type, BlockFacing facing) {
        private static final BlockState AIR = new BlockState(null, BlockFacing.NORTH);

        private BlockState {
            facing = facing == null ? BlockFacing.NORTH : facing;
        }
    }
}
