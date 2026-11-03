package dev.vessel.api.storage;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Direct opcode-level symmetry receptor and adapter for Bukkit, Spigot, and Paper
 * {@code org.bukkit.inventory.Inventory} contracts.
 *
 * <p>Receives rewritten static method calls from transmutated Bukkit bytecode across Eras E1b to E4b,
 * eliminating intermediate wrapper proxy allocations and ensuring HotSpot C2 JIT inlining.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class BukkitInventorySymmetryAdapter {

    public static final String BUKKIT_INVENTORY_INTERNAL = "org/bukkit/inventory/Inventory";
    public static final String RECEPTOR_CLASS_INTERNAL = "dev/vessel/api/storage/BukkitInventorySymmetryAdapter";

    /**
     * Constructs a new Bukkit inventory symmetry adapter instance.
     */
    public BukkitInventorySymmetryAdapter() {
    }

    /**
     * Direct opcode receptor for {@code Inventory.getSize()}.
     *
     * @param inventory target inventory object
     * @return slot count
     */
    public static int getSize(Object inventory) {
        if (inventory instanceof SimpleBukkitInventoryContainer container) {
            return container.getSize();
        }
        return 0;
    }

    /**
     * Direct opcode receptor for {@code Inventory.getItem(int index)}.
     *
     * @param inventory target inventory object
     * @param index slot index
     * @return item stack in slot or SimpleSlotItem.EMPTY
     */
    public static Object getItem(Object inventory, int index) {
        if (inventory instanceof SimpleBukkitInventoryContainer container) {
            return container.getItem(index);
        }
        return SimpleSlotItem.EMPTY;
    }

    /**
     * Direct opcode receptor for {@code Inventory.setItem(int index, ItemStack item)}.
     *
     * @param inventory target inventory object
     * @param index slot index
     * @param item item stack to place
     */
    public static void setItem(Object inventory, int index, Object item) {
        if (inventory instanceof SimpleBukkitInventoryContainer container) {
            SimpleSlotItem slotItem = (item instanceof SimpleSlotItem ssi) ? ssi : SimpleSlotItem.EMPTY;
            container.setItem(index, slotItem);
        }
    }

    /**
     * Direct opcode receptor for {@code Inventory.addItem(ItemStack... items)}.
     *
     * @param inventory target inventory object
     * @param items array of item stacks to insert
     * @return map of remaining items that did not fit
     */
    public static Map<Integer, Object> addItem(Object inventory, Object[] items) {
        if (inventory instanceof SimpleBukkitInventoryContainer container && items != null) {
            SimpleSlotItem[] slotItems = new SimpleSlotItem[items.length];
            for (int i = 0; i < items.length; i++) {
                slotItems[i] = (items[i] instanceof SimpleSlotItem ssi) ? ssi : SimpleSlotItem.EMPTY;
            }
            Map<Integer, SimpleSlotItem> leftover = container.addItem(slotItems);
            Map<Integer, Object> result = new HashMap<>();
            for (Map.Entry<Integer, SimpleSlotItem> entry : leftover.entrySet()) {
                result.put(entry.getKey(), entry.getValue());
            }
            return result;
        }
        return Collections.emptyMap();
    }

    /**
     * Direct opcode receptor for {@code Inventory.removeItem(ItemStack... items)}.
     *
     * @param inventory target inventory object
     * @param items array of item stacks to remove
     * @return map of items that could not be completely removed
     */
    public static Map<Integer, Object> removeItem(Object inventory, Object[] items) {
        if (inventory instanceof SimpleBukkitInventoryContainer container && items != null) {
            SimpleSlotItem[] slotItems = new SimpleSlotItem[items.length];
            for (int i = 0; i < items.length; i++) {
                slotItems[i] = (items[i] instanceof SimpleSlotItem ssi) ? ssi : SimpleSlotItem.EMPTY;
            }
            Map<Integer, SimpleSlotItem> leftover = container.removeItem(slotItems);
            Map<Integer, Object> result = new HashMap<>();
            for (Map.Entry<Integer, SimpleSlotItem> entry : leftover.entrySet()) {
                result.put(entry.getKey(), entry.getValue());
            }
            return result;
        }
        return Collections.emptyMap();
    }

    /**
     * Direct opcode receptor for {@code Inventory.clear()}.
     *
     * @param inventory target inventory object
     */
    public static void clear(Object inventory) {
        if (inventory instanceof SimpleBukkitInventoryContainer container) {
            container.clear();
        }
    }

    /**
     * Direct opcode receptor for {@code Inventory.clear(int index)}.
     *
     * @param inventory target inventory object
     * @param index slot index
     */
    public static void clear(Object inventory, int index) {
        if (inventory instanceof SimpleBukkitInventoryContainer container) {
            container.clear(index);
        }
    }

    /**
     * Direct opcode receptor for {@code Inventory.firstEmpty()}.
     *
     * @param inventory target inventory object
     * @return index of first empty slot or -1 if full
     */
    public static int firstEmpty(Object inventory) {
        if (inventory instanceof SimpleBukkitInventoryContainer container) {
            return container.firstEmpty();
        }
        return -1;
    }
}
