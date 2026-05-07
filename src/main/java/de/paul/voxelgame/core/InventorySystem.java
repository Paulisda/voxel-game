package de.paul.voxelgame.core;

import de.paul.voxelgame.objects.GameObject;
import de.paul.voxelgame.objects.RegistryManager;
import de.paul.voxelgame.objects.ResourceId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class InventorySystem {
    private static final String[] DEFAULT_HOTBAR_IDS = {
            "game:dirt", "game:stone", "game:wood",
            "game:grass", "game:water", "game:dirt",
            "game:stone", "game:wood", "game:grass"
    };

    private final RegistryManager registries;
    private boolean open;
    private int page;

    public InventorySystem(final RegistryManager registries) {
        this.registries = registries;
    }

    public GameObject[] createDefaultHotbar() {
        final GameObject[] hotbar = new GameObject[DEFAULT_HOTBAR_IDS.length];
        final GameObject fallback = firstRegisteredItem();

        for (int i = 0; i < DEFAULT_HOTBAR_IDS.length; i++) {
            hotbar[i] = registries.items()
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

    public List<GameObject> allInventoryEntries() {
        final List<GameObject> entries = new ArrayList<>();
        entries.addAll(registries.items().values());
        return Collections.unmodifiableList(entries);
    }

    public void toggle() {
        open = !open;
        if (open) {
            page = 0;
        }
    }

    public void open() {
        open = true;
        page = 0;
    }

    public void close() {
        open = false;
        page = 0;
    }

    public boolean isOpen() {
        return open;
    }

    public int page() {
        return page;
    }

    public void nextPage(final int pageCount) {
        page = clampPage(page + 1, pageCount);
    }

    public void previousPage(final int pageCount) {
        page = clampPage(page - 1, pageCount);
    }

    public void clampToPageCount(final int pageCount) {
        page = clampPage(page, pageCount);
    }

    private int clampPage(final int value, final int pageCount) {
        final int maxPage = Math.max(0, pageCount - 1);
        return Math.max(0, Math.min(maxPage, value));
    }

    private GameObject firstRegisteredItem() {
        for (final GameObject item : registries.items().values()) {
            return item;
        }
        throw new IllegalStateException("No items registered");
    }
}
