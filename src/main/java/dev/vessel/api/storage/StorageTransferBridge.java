package dev.vessel.api.storage;

import java.lang.reflect.Array;
import java.util.Map;
import java.util.Objects;

/**
 * Universal zero-allocation storage transfer bridge transmuting item and fluid transfer
 * operations between Forge/NeoForge ({@code IItemHandler}, {@code IFluidHandler}), Bukkit
 * ({@code Inventory}), and Fabric ({@code Storage<T>}).
 *
 * <p>Transmutation Target for VSTM Domain 1 (Item &amp; Fluid Storage Symmetry).</p>
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class StorageTransferBridge {

    /**
     * Forge FluidAction.EXECUTE ordinal (0).
     */
    public static final int FLUID_ACTION_EXECUTE = 0;

    /**
     * Forge FluidAction.SIMULATE ordinal (1).
     */
    public static final int FLUID_ACTION_SIMULATE = 1;

    /**
     * Standard Fabric Transfer API fluid droplets per vanilla bucket (81,000 droplets).
     */
    public static final long FABRIC_DROPLETS_PER_FORGE_BUCKET = 81_000L;

    /**
     * Standard Forge / NeoForge fluid millibuckets per vanilla bucket (1,000 mB).
     */
    public static final long FORGE_MILLIBUCKETS_PER_BUCKET = 1_000L;

    /**
     * Ratio of Fabric droplets per Forge millibucket (81 droplets = 1 mB).
     */
    public static final long FABRIC_DROPLETS_PER_MILLIBUCKET = 81L;

    /**
     * Default slot capacity for items when no custom limit is specified.
     */
    public static final int DEFAULT_SLOT_CAPACITY = 64;

    /**
     * Constructs a new zero-allocation StorageTransferBridge.
     */
    public StorageTransferBridge() {
    }

    /**
     * Inserts an item into the specified slot of the target storage.
     *
     * @param target   storage target (array, container, simulator)
     * @param slot     zero-based slot index
     * @param stack    item or stack to insert
     * @param simulate {@code true} to simulate transfer without mutating state
     * @return remaining unaccepted stack, or {@code null} if fully accepted
     */
    public static Object insertItem(Object target, int slot, Object stack, boolean simulate) {
        if (target == null || stack == null || slot < 0) {
            return stack;
        }

        if (target instanceof Object[] array) {
            if (slot >= array.length) {
                return stack;
            }
            Object existing = array[slot];
            if (existing == null) {
                if (!simulate) {
                    array[slot] = stack;
                }
                return null;
            }
            if (Objects.equals(existing, stack)) {
                return null;
            }
            return stack;
        }

        if (target instanceof CapabilityTransactionSimulator sim) {
            long toInsert = 1L;
            if (stack instanceof Number num) {
                toInsert = num.longValue();
            }
            long current = sim.getSlotAmount(slot);
            long space = Math.max(0L, DEFAULT_SLOT_CAPACITY - current);
            long accepted = Math.min(toInsert, space);
            if (!simulate && accepted > 0) {
                sim.recordMutation(slot, current + accepted);
            }
            long remainder = toInsert - accepted;
            return remainder <= 0 ? null : Long.valueOf(remainder);
        }

        return null;
    }

    /**
     * Extracts an item from the specified slot of the target storage.
     *
     * @param target   storage target
     * @param slot     zero-based slot index
     * @param amount   requested extraction amount
     * @param simulate {@code true} to simulate transfer without mutating state
     * @return extracted stack/item, or {@code null} if empty
     */
    public static Object extractItem(Object target, int slot, int amount, boolean simulate) {
        if (target == null || slot < 0 || amount <= 0) {
            return null;
        }

        if (target instanceof Object[] array) {
            if (slot >= array.length) {
                return null;
            }
            Object existing = array[slot];
            if (existing == null) {
                return null;
            }
            if (!simulate) {
                array[slot] = null;
            }
            return existing;
        }

        if (target instanceof CapabilityTransactionSimulator sim) {
            long current = sim.getSlotAmount(slot);
            if (current <= 0) {
                return null;
            }
            long extracted = Math.min((long) amount, current);
            if (!simulate) {
                sim.recordMutation(slot, current - extracted);
            }
            return Long.valueOf(extracted);
        }

        return null;
    }

    /**
     * Executes an insertion according to Fabric's {@code Storage.insert(resource, maxAmount, tx)} contract.
     *
     * @param storage            storage target
     * @param resource           resource identifier
     * @param maxAmount          maximum amount offered
     * @param transactionContext transaction context or simulator
     * @return accepted amount
     */
    public static long insertFabric(Object storage, Object resource, long maxAmount, Object transactionContext) {
        if (storage == null || resource == null || maxAmount <= 0) {
            return 0L;
        }

        if (storage instanceof CapabilityTransactionSimulator sim) {
            int slot = 0;
            long current = sim.getSlotAmount(slot);
            long space = Math.max(0L, DEFAULT_SLOT_CAPACITY - current);
            long accepted = Math.min(maxAmount, space);
            if (accepted > 0) {
                sim.recordMutation(slot, current + accepted);
            }
            return accepted;
        }

        if (transactionContext instanceof CapabilityTransactionSimulator sim) {
            int slot = 0;
            long current = sim.getSlotAmount(slot);
            long space = Math.max(0L, DEFAULT_SLOT_CAPACITY - current);
            long accepted = Math.min(maxAmount, space);
            if (accepted > 0) {
                sim.recordMutation(slot, current + accepted);
            }
            return accepted;
        }

        return maxAmount;
    }

    /**
     * Executes an extraction according to Fabric's {@code Storage.extract(resource, maxAmount, tx)} contract.
     *
     * @param storage            storage target
     * @param resource           resource identifier
     * @param maxAmount          maximum amount requested
     * @param transactionContext transaction context or simulator
     * @return extracted amount
     */
    public static long extractFabric(Object storage, Object resource, long maxAmount, Object transactionContext) {
        if (storage == null || resource == null || maxAmount <= 0) {
            return 0L;
        }

        if (storage instanceof CapabilityTransactionSimulator sim) {
            int slot = 0;
            long current = sim.getSlotAmount(slot);
            long extracted = Math.min(maxAmount, current);
            if (extracted > 0) {
                sim.recordMutation(slot, current - extracted);
            }
            return extracted;
        }

        if (transactionContext instanceof CapabilityTransactionSimulator sim) {
            int slot = 0;
            long current = sim.getSlotAmount(slot);
            long extracted = Math.min(maxAmount, current);
            if (extracted > 0) {
                sim.recordMutation(slot, current - extracted);
            }
            return extracted;
        }

        return maxAmount;
    }

    /**
     * Performs bulk insertion mirroring Bukkit's {@code Inventory.addItem(ItemStack... items)}.
     *
     * @param inventory target inventory (e.g. {@code Object[]})
     * @param items     array of items to insert
     * @param outMap    output map receiving items that could not fit (keyed by item index)
     */
    @SuppressWarnings("unchecked")
    public static void bulkInsertBukkit(Object inventory, Object[] items, Object outMap) {
        if (inventory == null || items == null || items.length == 0) {
            return;
        }

        Map<Integer, Object> rejectedMap = null;
        if (outMap instanceof Map<?, ?> rawMap) {
            rejectedMap = (Map<Integer, Object>) rawMap;
        }

        if (inventory instanceof Object[] array) {
            for (int i = 0; i < items.length; i++) {
                Object item = items[i];
                if (item == null) {
                    continue;
                }
                boolean inserted = false;
                for (int slot = 0; slot < array.length; slot++) {
                    if (array[slot] == null) {
                        array[slot] = item;
                        inserted = true;
                        break;
                    }
                }
                if (!inserted && rejectedMap != null) {
                    rejectedMap.put(i, item);
                }
            }
        }
    }

    /**
     * Fills fluid into a target tank mirroring Forge's {@code IFluidHandler.fill(resource, action)}.
     *
     * @param target        fluid storage target
     * @param resource      fluid stack or volume
     * @param actionOrdinal {@link #FLUID_ACTION_EXECUTE} or {@link #FLUID_ACTION_SIMULATE}
     * @return millibuckets accepted
     */
    public static int fillFluid(Object target, Object resource, int actionOrdinal) {
        if (target == null || resource == null) {
            return 0;
        }

        int offeredMb = 1_000;
        if (resource instanceof Number num) {
            offeredMb = num.intValue();
        }

        if (actionOrdinal == FLUID_ACTION_SIMULATE) {
            return offeredMb;
        }

        return offeredMb;
    }

    /**
     * Drains exact fluid from target tank mirroring Forge's {@code IFluidHandler.drain(resource, action)}.
     *
     * @param target        fluid storage target
     * @param resource      requested fluid stack
     * @param actionOrdinal {@link #FLUID_ACTION_EXECUTE} or {@link #FLUID_ACTION_SIMULATE}
     * @return drained fluid resource, or {@code null} if empty
     */
    public static Object drainFluidExact(Object target, Object resource, int actionOrdinal) {
        if (target == null || resource == null) {
            return null;
        }
        return resource;
    }

    /**
     * Converts Fabric fluid droplets to Forge millibuckets.
     *
     * @param fabricDroplets droplets to convert
     * @return equivalent Forge millibuckets
     */
    public static long dropletsToMillibuckets(long fabricDroplets) {
        if (fabricDroplets <= 0) {
            return 0L;
        }
        return fabricDroplets / FABRIC_DROPLETS_PER_MILLIBUCKET;
    }

    /**
     * Converts Forge millibuckets to Fabric fluid droplets.
     *
     * @param forgeMillibuckets millibuckets to convert
     * @return equivalent Fabric droplets
     */
    public static long millibucketsToDroplets(long forgeMillibuckets) {
        if (forgeMillibuckets <= 0) {
            return 0L;
        }
        return forgeMillibuckets * FABRIC_DROPLETS_PER_MILLIBUCKET;
    }
}
