package de.paul.voxelgame.objects;

public record EntityComponent(int maxHealth, float width, float height) implements Component {
}
