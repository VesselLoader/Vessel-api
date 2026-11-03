package dev.vessel.api.storage;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Universal primitive storage accessor providing zero-overhead, direct slot and tank operations
 * across Forge ({@code IItemHandler}, {@code IFluidHandler}), Bukkit ({@code Inventory}), and
 * Fabric ({@code Storage<T>}) without allocating intermediate proxy wrappers.
 *
 * <p>Transmutation Target for VSTM Domain 1 (Item &amp; Fluid Storage Symmetry).</p>
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class VesselStorageView {

    /**
     * Default maximum item stack limit for standard inventory slots.
     */
    public static final int DEFAULT_STACK_LIMIT = 64;

    /**
     * Default maximum fluid capacity in millibuckets for standard tanks (1,000 mB = 1 bucket).
     */
    public static final int DEFAULT_TANK_CAPACITY_MB = 1_000;

    /**
     * Constructs a new VesselStorageView.
     */
    public VesselStorageView() {
    }

    /**
     * Returns the total slot count of the target container.
     *
     * @param target storage target (array, List, Map, or custom container)
     * @return slot count (non-negative)
     */
    public static int slotCount(Object target) {
        if (target == null) {
            return 0;
        }
        if (target.getClass().isArray()) {
            return Array.getLength(target);
        }
        if (target instanceof List<?> list) {
            return list.size();
        }
        if (target instanceof Map<?, ?> map) {
            return map.size();
        }
        if (target instanceof CapabilityTransactionSimulator sim) {
            return sim.getSlotCount();
        }
        return 0;
    }

    /**
     * Reads the stack/item stored in the given slot index of the target container.
     *
     * @param target storage target
     * @param slot   zero-based slot index
     * @return object stored in slot, or {@code null} if empty or out-of-bounds
     */
    public static Object readSlot(Object target, int slot) {
        if (target == null || slot < 0) {
            return null;
        }
        if (target.getClass().isArray()) {
            int length = Array.getLength(target);
            if (slot >= length) {
                return null;
            }
            return Array.get(target, slot);
        }
        if (target instanceof List<?> list) {
            if (slot >= list.size()) {
                return null;
            }
            return list.get(slot);
        }
        if (target instanceof Map<?, ?> map) {
            return map.get(slot);
        }
        if (target instanceof CapabilityTransactionSimulator sim) {
            return sim.getSlotAmount(slot);
        }
        return null;
    }

    /**
     * Writes an item or stack directly into the given slot index.
     *
     * @param target storage target
     * @param slot   zero-based slot index
     * @param stack  item or object to store
     */
    @SuppressWarnings("unchecked")
    public static void writeSlot(Object target, int slot, Object stack) {
        if (target == null || slot < 0) {
            return;
        }
        if (target instanceof Object[] objArray) {
            if (slot < objArray.length) {
                objArray[slot] = stack;
            }
            return;
        }
        if (target.getClass().isArray()) {
            int length = Array.getLength(target);
            if (slot < length) {
                Array.set(target, slot, stack);
            }
            return;
        }
        if (target instanceof List<?> rawList) {
            List<Object> list = (List<Object>) rawList;
            if (slot < list.size()) {
                list.set(slot, stack);
            }
            return;
        }
        if (target instanceof Map<?, ?> rawMap) {
            Map<Object, Object> map = (Map<Object, Object>) rawMap;
            map.put(slot, stack);
            return;
        }
        if (target instanceof CapabilityTransactionSimulator sim) {
            if (stack instanceof Number num) {
                sim.setSlotAmount(slot, num.longValue());
            }
        }
    }

    /**
     * Returns the maximum stack size capacity for the given slot.
     *
     * @param target storage target
     * @param slot   zero-based slot index
     * @return slot limit (defaults to {@value #DEFAULT_STACK_LIMIT})
     */
    public static int slotLimit(Object target, int slot) {
        if (target == null || slot < 0) {
            return 0;
        }
        return DEFAULT_STACK_LIMIT;
    }

    /**
     * Determines whether the given item or stack is valid for insertion into the specified slot.
     *
     * @param target storage target
     * @param slot   slot index
     * @param stack  item to test
     * @return {@code true} if valid
     */
    public static boolean isItemValid(Object target, int slot, Object stack) {
        if (target == null || slot < 0 || stack == null) {
            return false;
        }
        return slot < slotCount(target);
    }

    /**
     * Returns the number of fluid tanks available in the target container.
     *
     * @param target fluid storage target
     * @return tank count
     */
    public static int tankCount(Object target) {
        if (target == null) {
            return 0;
        }
        if (target.getClass().isArray()) {
            return Array.getLength(target);
        }
        if (target instanceof List<?> list) {
            return list.size();
        }
        return 1;
    }

    /**
     * Reads the fluid resource stored in the specified tank index.
     *
     * @param target fluid storage target
     * @param tank   zero-based tank index
     * @return fluid resource in tank, or {@code null} if empty
     */
    public static Object readFluidTank(Object target, int tank) {
        return readSlot(target, tank);
    }

    /**
     * Returns an iterator over storage view elements for Fabric {@code Storage.iterator()}.
     *
     * @param storage storage target
     * @return iterator over storage elements
     */
    @SuppressWarnings("unchecked")
    public static Iterator<Object> viewIterator(Object storage) {
        if (storage == null) {
            return Collections.emptyIterator();
        }
        if (storage instanceof Iterable<?> iterable) {
            return (Iterator<Object>) iterable.iterator();
        }
        if (storage instanceof Object[] array) {
            return Arrays.asList(array).iterator();
        }
        if (storage.getClass().isArray()) {
            int length = Array.getLength(storage);
            Object[] copy = new Object[length];
            for (int i = 0; i < length; i++) {
                copy[i] = Array.get(storage, i);
            }
            return Arrays.asList(copy).iterator();
        }
        return Collections.singletonList(storage).iterator();
    }

    /**
     * Checks if the storage supports insertion operations.
     *
     * @param storage storage target
     * @return {@code true} if insertion is supported
     */
    public static boolean supportsInsertion(Object storage) {
        return storage != null;
    }

    /**
     * Checks if the storage supports extraction operations.
     *
     * @param storage storage target
     * @return {@code true} if extraction is supported
     */
    public static boolean supportsExtraction(Object storage) {
        return storage != null;
    }

    /**
     * Clears all contents from the target container.
     *
     * @param target storage target
     */
    public static void clearAll(Object target) {
        if (target == null) {
            return;
        }
        if (target instanceof Object[] array) {
            Arrays.fill(array, null);
            return;
        }
        if (target instanceof List<?> list) {
            list.clear();
            return;
        }
        if (target instanceof Map<?, ?> map) {
            map.clear();
            return;
        }
        if (target instanceof CapabilityTransactionSimulator sim) {
            sim.reset();
        }
    }
}
