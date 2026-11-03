package dev.vessel.api.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Clean-room in-memory inventory container implementing Bukkit / Spigot / Paper
 * {@code org.bukkit.inventory.Inventory} contracts for testing and runtime execution.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class SimpleBukkitInventoryContainer {

    public static final int DEFAULT_MAX_STACK_SIZE = 64;

    private final SimpleSlotItem[] contents;
    private final int maxStackSize;

    /**
     * Constructs a Bukkit inventory with the given size and standard max stack size.
     *
     * @param size slot count (e.g. 27 for single chest, 54 for double chest)
     */
    public SimpleBukkitInventoryContainer(int size) {
        this(size, DEFAULT_MAX_STACK_SIZE);
    }

    /**
     * Constructs a Bukkit inventory with custom size and max stack size.
     *
     * @param size slot count
     * @param maxStackSize maximum stack size
     */
    public SimpleBukkitInventoryContainer(int size, int maxStackSize) {
        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive: " + size);
        }
        if (maxStackSize <= 0) {
            throw new IllegalArgumentException("maxStackSize must be positive: " + maxStackSize);
        }
        this.contents = new SimpleSlotItem[size];
        for (int i = 0; i < size; i++) {
            this.contents[i] = SimpleSlotItem.EMPTY;
        }
        this.maxStackSize = maxStackSize;
    }

    public int getSize() {
        return contents.length;
    }

    public int getMaxStackSize() {
        return maxStackSize;
    }

    public SimpleSlotItem getItem(int index) {
        if (index < 0 || index >= contents.length) {
            return SimpleSlotItem.EMPTY;
        }
        return contents[index];
    }

    public void setItem(int index, SimpleSlotItem item) {
        if (index >= 0 && index < contents.length) {
            contents[index] = (item == null) ? SimpleSlotItem.EMPTY : item;
        }
    }

    /**
     * Finds the index of the first empty slot.
     *
     * @return index or -1 if full
     */
    public int firstEmpty() {
        for (int i = 0; i < contents.length; i++) {
            if (contents[i].isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Stores the given ItemStacks in the inventory, filling partially-full slots first
     * and placing remaining items in empty slots.
     *
     * @param items array of items to insert
     * @return map of remaining items that did not fit, indexed by their position in input array
     */
    public Map<Integer, SimpleSlotItem> addItem(SimpleSlotItem... items) {
        Map<Integer, SimpleSlotItem> leftover = new HashMap<>();
        if (items == null) {
            return leftover;
        }

        for (int i = 0; i < items.length; i++) {
            SimpleSlotItem item = items[i];
            if (item == null || item.isEmpty()) {
                continue;
            }

            int toAdd = item.count();

            // 1. Fill existing matching stacks
            for (int slot = 0; slot < contents.length && toAdd > 0; slot++) {
                SimpleSlotItem current = contents[slot];
                if (!current.isEmpty() && current.itemId().equals(item.itemId())) {
                    int space = maxStackSize - current.count();
                    if (space > 0) {
                        int added = Math.min(toAdd, space);
                        contents[slot] = current.withCount(current.count() + added);
                        toAdd -= added;
                    }
                }
            }

            // 2. Fill empty slots
            for (int slot = 0; slot < contents.length && toAdd > 0; slot++) {
                if (contents[slot].isEmpty()) {
                    int added = Math.min(toAdd, maxStackSize);
                    contents[slot] = item.withCount(added);
                    toAdd -= added;
                }
            }

            if (toAdd > 0) {
                leftover.put(i, item.withCount(toAdd));
            }
        }

        return leftover;
    }

    /**
     * Removes the specified ItemStacks from the inventory.
     *
     * @param items array of items to remove
     * @return map of items that could not be completely removed
     */
    public Map<Integer, SimpleSlotItem> removeItem(SimpleSlotItem... items) {
        Map<Integer, SimpleSlotItem> leftover = new HashMap<>();
        if (items == null) {
            return leftover;
        }

        for (int i = 0; i < items.length; i++) {
            SimpleSlotItem item = items[i];
            if (item == null || item.isEmpty()) {
                continue;
            }

            int toRemove = item.count();
            for (int slot = 0; slot < contents.length && toRemove > 0; slot++) {
                SimpleSlotItem current = contents[slot];
                if (!current.isEmpty() && current.itemId().equals(item.itemId())) {
                    if (current.count() <= toRemove) {
                        toRemove -= current.count();
                        contents[slot] = SimpleSlotItem.EMPTY;
                    } else {
                        contents[slot] = current.withCount(current.count() - toRemove);
                        toRemove = 0;
                    }
                }
            }

            if (toRemove > 0) {
                leftover.put(i, item.withCount(toRemove));
            }
        }

        return leftover;
    }

    /**
     * Clears the entire inventory.
     */
    public void clear() {
        for (int i = 0; i < contents.length; i++) {
            contents[i] = SimpleSlotItem.EMPTY;
        }
    }

    /**
     * Clears a specific slot index.
     *
     * @param index slot index
     */
    public void clear(int index) {
        if (index >= 0 && index < contents.length) {
            contents[index] = SimpleSlotItem.EMPTY;
        }
    }
}
