package de.paul.voxelgame.map;

public enum BlockType {
    GRASS(
            true,
            new String[]{
                    "grass_block_side",
                    "grass_block_side0",
                    "grass_block_side1",
                    "grass_block_side2",
                    "grass_block_side3",
                    "grass_block_side4",
                    "grass_side"
            },
            new String[]{"grass_block_top", "grass_top", "grass"},
            new String[]{"dirt", "dirt0", "dirt1", "dirt2", "dirt3", "dirt4"}
    ),
    DIRT(true, "dirt", "dirt0", "dirt1", "dirt2", "dirt3", "dirt4"),
    STONE(true, "stone", "stone0", "stone1", "stone2", "stone3"),
    BEDROCK(true, "bedrock"),
    WATER(false, "water_still", "water"),
    WOOD(true, "oak_planks", "planks_oak", "planks");

    private final boolean solid;
    private final String[] sideTextureCandidates;
    private final String[] topTextureCandidates;
    private final String[] bottomTextureCandidates;

    BlockType(boolean solid, String... textureCandidates) {
        this(solid, textureCandidates, textureCandidates, textureCandidates);
    }

    BlockType(boolean solid, String[] sideTextureCandidates, String[] topTextureCandidates, String[] bottomTextureCandidates) {
        this.solid = solid;
        this.sideTextureCandidates = sideTextureCandidates;
        this.topTextureCandidates = topTextureCandidates;
        this.bottomTextureCandidates = bottomTextureCandidates;
    }

    public boolean isSolid() {
        return solid;
    }

    public String[] getSideTextureCandidates() {
        return sideTextureCandidates;
    }

    public String[] getTopTextureCandidates() {
        return topTextureCandidates;
    }

    public String[] getBottomTextureCandidates() {
        return bottomTextureCandidates;
    }
}
