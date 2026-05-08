package de.paul.voxelgame.map;

public enum BlockFacing {
    NORTH,
    EAST,
    SOUTH,
    WEST;

    public BlockFacing opposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case EAST -> WEST;
            case SOUTH -> NORTH;
            case WEST -> EAST;
        };
    }

    public static BlockFacing fromHorizontalVector(final double x, final double z) {
        if (Math.abs(x) > Math.abs(z)) {
            return x >= 0.0 ? EAST : WEST;
        }
        return z >= 0.0 ? SOUTH : NORTH;
    }
}
