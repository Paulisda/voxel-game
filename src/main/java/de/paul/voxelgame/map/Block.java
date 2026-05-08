package de.paul.voxelgame.map;

import de.paul.voxelgame.objects.BlockComponent;
import de.paul.voxelgame.objects.GameObject;
import de.paul.voxelgame.objects.ResourceId;

public class Block {
    private final int worldX;
    private final int worldY;
    private final int worldZ;
    private final GameObject type;
    private final BlockFacing facing;

    public Block(final int worldX, final int worldY, final int worldZ, final GameObject type) {
        this(worldX, worldY, worldZ, type, BlockFacing.NORTH);
    }

    public Block(final int worldX, final int worldY, final int worldZ, final GameObject type, final BlockFacing facing) {
        this.worldX = worldX;
        this.worldY = worldY;
        this.worldZ = worldZ;
        this.type = type;
        this.facing = facing == null ? BlockFacing.NORTH : facing;
    }

    public boolean isSolid() {
        return type != null && type.has(BlockComponent.class) && type.get(BlockComponent.class).solid();
    }

    public GameObject getType() {
        return type;
    }

    public ResourceId getTypeId() {
        return type.id();
    }

    public BlockFacing getFacing() {
        return facing;
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
