package de.paul.voxelgame;

public final class GameConfig {
    public static final int WIDTH = 1280;
    public static final int HEIGHT = 720;

    // 0 = survival, 1 = creative, 2 = spectator
    public static final int GAMEMODE = Integer.getInteger("voxel.gamemode", 0);

    public static final int BLOCK_SIZE = 1;
    public static final int CHUNK_WIDTH = 16;
    public static final int CHUNK_DEPTH = 16;
    public static final int CHUNK_HEIGHT = 24;
    public static final int WORLD_CHUNK_RADIUS = 1;

    private GameConfig() {
    }
}
