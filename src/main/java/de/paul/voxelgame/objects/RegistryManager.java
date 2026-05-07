package de.paul.voxelgame.objects;

public final class RegistryManager {
    private final Registry<GameObject> itemRegistry = new Registry<>();
    private final Registry<GameObject> blockRegistry = new Registry<>();
    private final Registry<GameObject> entityRegistry = new Registry<>();

    public Registry<GameObject> items() {
        return itemRegistry;
    }

    public Registry<GameObject> blocks() {
        return blockRegistry;
    }

    public Registry<GameObject> entities() {
        return entityRegistry;
    }
}
