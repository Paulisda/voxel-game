package de.paul.voxelgame.core;

import de.paul.voxelgame.map.World;
import de.paul.voxelgame.objects.RegistryManager;

public final class WorldSystem {
    private final RegistryManager registries;

    public WorldSystem(final RegistryManager registries) {
        this.registries = registries;
    }

    public World createWorld(final boolean debugCollision) {
        final World world = new World(registries);
        world.generateWorld(debugCollision);
        return world;
    }
}
