package de.paul.voxelgame.core;

import de.paul.voxelgame.objects.GameObject;
import de.paul.voxelgame.objects.RegistryManager;
import de.paul.voxelgame.objects.ResourceId;

import java.util.ArrayList;
import java.util.List;

public final class InventorySystem {
    private static final String[] DEFAULT_HOTBAR_IDS = {
            "game:dirt", "game:stone", "game:wood",
            "game:grass", "game:water", "game:dirt",
            "game:stone", "game:wood", "game:grass"
    };

    private final RegistryManager registries;

    public InventorySystem(final RegistryManager registries) {
        this.registries = registries;
    }

    public GameObject[] createDefaultHotbar() {
        final GameObject[] hotbar = new GameObject[DEFAULT_HOTBAR_IDS.length];
        final GameObject fallback = firstRegisteredBlock();

        for (int i = 0; i < DEFAULT_HOTBAR_IDS.length; i++) {
            hotbar[i] = registries.blocks()
                    .find(ResourceId.of(DEFAULT_HOTBAR_IDS[i]))
                    .orElse(fallback);
        }

        return hotbar;
    }

    public List<GameObject> blocksWithTag(final String tag) {
        final List<GameObject> result = new ArrayList<>();
        for (final GameObject block : registries.blocks().values()) {
            if (block.hasTag(tag)) {
                result.add(block);
            }
        }
        return result;
    }

    private GameObject firstRegisteredBlock() {
        for (final GameObject block : registries.blocks().values()) {
            return block;
        }
        throw new IllegalStateException("No blocks registered");
    }
}
