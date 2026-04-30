package de.paul.voxelgame.map;

public class Block {
    private final int worldX;
    private final int worldY;
    private final int worldZ;
    private final BlockType type;

    public Block(final int worldX, final int worldY, final int worldZ, final BlockType type) {
        this.worldX = worldX;
        this.worldY = worldY;
        this.worldZ = worldZ;
        this.type = type;
    }

    public boolean isSolid() {
        return type.isSolid();
    }

    public BlockType getType() {
        return type;
    }

    public int getWorldX() {
        return worldX;
    }

    public int getWorldY() {
        return worldY;
    }

    public int getWorldZ() {
        return worldZ;
    }
}
