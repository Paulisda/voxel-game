package de.paul.voxelgame.core;

import de.paul.voxelgame.objects.GameObject;
import de.paul.voxelgame.objects.RegistryManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class InventorySystem {
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int INVENTORY_SLOT_COUNT = 27;

    private final RegistryManager registries;
    private final InventoryStack[] inventorySlots = new InventoryStack[INVENTORY_SLOT_COUNT];
    private boolean open;
    private int page;
    private String searchQuery = "";
    private boolean searchFocused;
    private InventoryStack carriedStack;

    public InventorySystem(final RegistryManager registries) {
        this.registries = registries;
    }

    public InventoryStack[] createDefaultHotbar() {
        return new InventoryStack[HOTBAR_SLOT_COUNT];
    }

    public int inventorySlotCount() {
        return inventorySlots.length;
    }

    public InventoryStack inventorySlot(final int slotIndex) {
        if (slotIndex < 0 || slotIndex >= inventorySlots.length) {
            return null;
        }
        return inventorySlots[slotIndex];
    }

    public void setInventorySlot(final int slotIndex, final InventoryStack stack) {
        if (slotIndex < 0 || slotIndex >= inventorySlots.length) {
            return;
        }
        inventorySlots[slotIndex] = stack == null || stack.isEmpty() ? null : stack;
    }

    public InventoryStack carriedStack() {
        return carriedStack;
    }

    public void setCarriedStack(final InventoryStack carriedStack) {
        this.carriedStack = carriedStack == null || carriedStack.isEmpty() ? null : carriedStack;
    }

    public boolean isSearchFocused() {
        return searchFocused;
    }

    public void setSearchFocused(final boolean searchFocused) {
        this.searchFocused = searchFocused;
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

    public List<GameObject> filteredInventoryEntries(final LocalizationManager localization) {
        final String query = searchQuery.trim();
        if (query.isEmpty()) {
            return allInventoryEntries();
        }

        final String normalizedQuery = query.toLowerCase(Locale.ROOT);
        final List<GameObject> entries = new ArrayList<>();
        for (final GameObject item : registries.items().values()) {
            if (localization.objectNameMatches(item, normalizedQuery)) {
                entries.add(item);
            }
        }
        return Collections.unmodifiableList(entries);
    }

    public void toggle() {
        open = !open;
        if (open) {
            page = 0;
            searchQuery = "";
        }
    }

    public void open() {
        open = true;
        page = 0;
        searchQuery = "";
        searchFocused = false;
    }

    public void close() {
        open = false;
        page = 0;
        searchQuery = "";
        searchFocused = false;
        carriedStack = null;
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

    public String searchQuery() {
        return searchQuery;
    }

    public void appendSearchText(final String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        searchQuery += text;
        page = 0;
    }

    public void removeLastSearchCharacter() {
        if (searchQuery.isEmpty()) {
            return;
        }
        searchQuery = searchQuery.substring(0, searchQuery.offsetByCodePoints(searchQuery.length(), -1));
        page = 0;
    }

    public void clearSearch() {
        if (searchQuery.isEmpty()) {
            return;
        }
        searchQuery = "";
        page = 0;
    }

    private int clampPage(final int value, final int pageCount) {
        final int maxPage = Math.max(0, pageCount - 1);
        return Math.max(0, Math.min(maxPage, value));
    }
}
