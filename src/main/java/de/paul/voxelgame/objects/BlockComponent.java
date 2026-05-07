package de.paul.voxelgame.objects;

public record BlockComponent(boolean solid, float hardness, float resistance, boolean breakable) implements Component {
    public BlockComponent(final float hardness, final float resistance) {
        this(true, hardness, resistance, true);
    }
}
