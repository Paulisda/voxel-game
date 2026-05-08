package de.paul.voxelgame.core;

import de.paul.voxelgame.objects.GameObject;

public final class InventoryStack {
    private final GameObject item;
    private int count;

    public InventoryStack(final GameObject item, final int count) {
        if (item == null || count <= 0) {
            throw new IllegalArgumentException("Inventory stack needs an item and a positive count");
        }
        this.item = item;
        this.count = Math.min(count, maxStackSize(item));
    }

    public GameObject item() {
        return item;
    }

    public int count() {
        return count;
    }

    public boolean isSameItem(final InventoryStack other) {
        return other != null && item.id().equals(other.item().id());
    }

    public boolean canMerge(final InventoryStack other) {
        return isSameItem(other) && count < maxStackSize();
    }

    public int mergeFrom(final InventoryStack other) {
        if (!canMerge(other)) {
            return 0;
        }
        final int amount = Math.min(other.count, maxStackSize() - count);
        count += amount;
        other.count -= amount;
        return amount;
    }

    public void remove(final int amount) {
        count = Math.max(0, count - Math.max(0, amount));
    }

    public boolean isEmpty() {
        return count <= 0;
    }

    public int maxStackSize() {
        return maxStackSize(item);
    }

    public InventoryStack copy() {
        return new InventoryStack(item, count);
    }

    public static InventoryStack fullStack(final GameObject item) {
        return new InventoryStack(item, maxStackSize(item));
    }

    public static int maxStackSize(final GameObject item) {
        return 64;
    }
}
