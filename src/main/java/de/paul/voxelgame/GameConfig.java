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
    public static final int CHUNK_HEIGHT = 24;
    public static final int WORLD_CHUNK_RADIUS = 1;

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
}
