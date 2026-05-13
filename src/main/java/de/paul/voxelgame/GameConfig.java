package de.paul.voxelgame;

public final class GameConfig {
    public static final int WIDTH = 1280;
    public static final int HEIGHT = 720;

    public static final int GAMEMODE_SURVIVAL = 0;
    public static final int GAMEMODE_CREATIVE = 1;
    public static final int GAMEMODE_SPECTATOR = 2;

    // 0 = survival, 1 = creative, 2 = spectator
    private static int gameMode = Integer.getInteger("voxel.gamemode", GAMEMODE_SURVIVAL);

    public static final int BLOCK_SIZE = 1;
    public static final int CHUNK_WIDTH = 16;
    public static final int CHUNK_DEPTH = 16;
    public static final int CHUNK_HEIGHT = 256;
    public static final int SEA_LEVEL = 63;
    public static final int WORLD_HALF_SIZE_BLOCKS = 30_000_000;
    public static final int MIN_VIEW_DISTANCE_CHUNKS = 2;
    public static final int MAX_VIEW_DISTANCE_CHUNKS = 64;
    public static final int DEFAULT_VIEW_DISTANCE_CHUNKS = 4;

    private static int viewDistanceChunks = clampViewDistance(Integer.getInteger("voxel.viewDistance", DEFAULT_VIEW_DISTANCE_CHUNKS));

    private GameConfig() {
    }

    public static int getGameMode() {
        return gameMode;
    }

    public static void setGameMode(final int newGameMode) {
        gameMode = switch (newGameMode) {
            case GAMEMODE_CREATIVE -> GAMEMODE_CREATIVE;
            case GAMEMODE_SPECTATOR -> GAMEMODE_SPECTATOR;
            default -> GAMEMODE_SURVIVAL;
        };
    }

    public static boolean isSurvival() {
        return gameMode == GAMEMODE_SURVIVAL;
    }

    public static boolean isCreative() {
        return gameMode == GAMEMODE_CREATIVE;
    }

    public static int getViewDistanceChunks() {
        return viewDistanceChunks;
    }

    public static void setViewDistanceChunks(final int chunks) {
        viewDistanceChunks = clampViewDistance(chunks);
    }

    public static void adjustViewDistanceChunks(final int delta) {
        setViewDistanceChunks(viewDistanceChunks + delta);
    }

    public static boolean isInsideWorld(final int blockX, final int blockZ) {
        return blockX >= -WORLD_HALF_SIZE_BLOCKS && blockX < WORLD_HALF_SIZE_BLOCKS
                && blockZ >= -WORLD_HALF_SIZE_BLOCKS && blockZ < WORLD_HALF_SIZE_BLOCKS;
    }

    private static int clampViewDistance(final int chunks) {
        return Math.max(MIN_VIEW_DISTANCE_CHUNKS, Math.min(MAX_VIEW_DISTANCE_CHUNKS, chunks));
    }
}
