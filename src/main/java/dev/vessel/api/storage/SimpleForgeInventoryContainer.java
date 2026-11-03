package dev.vessel.api.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Clean-room in-memory storage container implementing Forge / NeoForge {@code IItemHandler}
 * and {@code IFluidHandler} capability contracts for testing and runtime execution.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class SimpleForgeInventoryContainer {

    public static final int DEFAULT_SLOT_LIMIT = 64;

    private final SimpleSlotItem[] slots;
    private final int slotLimit;
    private final List<SimpleFluidTank> tanks = new ArrayList<>();

    /**
     * Constructs a container with the given number of slots and standard slot limit.
     *
     * @param slotCount number of inventory slots
     */
    public SimpleForgeInventoryContainer(int slotCount) {
        this(slotCount, DEFAULT_SLOT_LIMIT);
    }

    /**
     * Constructs a container with custom slot count and slot limit.
     *
     * @param slotCount number of inventory slots
     * @param slotLimit maximum stack size per slot
     */
    public SimpleForgeInventoryContainer(int slotCount, int slotLimit) {
        if (slotCount <= 0) {
            throw new IllegalArgumentException("slotCount must be positive: " + slotCount);
        }
        if (slotLimit <= 0) {
            throw new IllegalArgumentException("slotLimit must be positive: " + slotLimit);
        }
        this.slots = new SimpleSlotItem[slotCount];
        for (int i = 0; i < slotCount; i++) {
            this.slots[i] = SimpleSlotItem.EMPTY;
        }
        this.slotLimit = slotLimit;
    }

    /**
     * Adds a fluid tank to this container.
     *
     * @param tank fluid tank instance
     */
    public void addTank(SimpleFluidTank tank) {
        tanks.add(Objects.requireNonNull(tank, "tank cannot be null"));
    }

    public int getSlots() {
        return slots.length;
    }

    public SimpleSlotItem getStackInSlot(int slot) {
        if (slot < 0 || slot >= slots.length) {
            return SimpleSlotItem.EMPTY;
        }
        return slots[slot];
    }

    public int getSlotLimit(int slot) {
        return slotLimit;
    }

    public boolean isItemValid(int slot, SimpleSlotItem stack) {
        return slot >= 0 && slot < slots.length && stack != null && !stack.isEmpty();
    }

    /**
     * Inserts an item into the target slot with optional simulation.
     *
     * @param slot target slot
     * @param stack item stack to insert
     * @param simulate true to simulate without committing
     * @return remaining stack that could not be inserted (EMPTY if fully accepted)
     */
    public SimpleSlotItem insertItem(int slot, SimpleSlotItem stack, boolean simulate) {
        if (slot < 0 || slot >= slots.length || stack == null || stack.isEmpty()) {
            return stack == null ? SimpleSlotItem.EMPTY : stack;
        }

        SimpleSlotItem current = slots[slot];
        if (current.isEmpty()) {
            int accepted = Math.min(stack.count(), slotLimit);
            if (!simulate) {
                slots[slot] = stack.withCount(accepted);
            }
            int remainder = stack.count() - accepted;
            return remainder > 0 ? stack.withCount(remainder) : SimpleSlotItem.EMPTY;
        }

        if (!current.itemId().equals(stack.itemId())) {
            return stack;
        }

        int availableSpace = Math.max(0, slotLimit - current.count());
        int accepted = Math.min(stack.count(), availableSpace);
        if (!simulate && accepted > 0) {
            slots[slot] = current.withCount(current.count() + accepted);
        }

        int remainder = stack.count() - accepted;
        return remainder > 0 ? stack.withCount(remainder) : SimpleSlotItem.EMPTY;
    }

    /**
     * Extracts an item from the target slot with optional simulation.
     *
     * @param slot target slot
     * @param amount maximum amount to extract
     * @param simulate true to simulate without committing
     * @return extracted item stack
     */
    public SimpleSlotItem extractItem(int slot, int amount, boolean simulate) {
        if (slot < 0 || slot >= slots.length || amount <= 0) {
            return SimpleSlotItem.EMPTY;
        }

        SimpleSlotItem current = slots[slot];
        if (current.isEmpty()) {
            return SimpleSlotItem.EMPTY;
        }

        int extracted = Math.min(amount, current.count());
        if (!simulate) {
            int remaining = current.count() - extracted;
            slots[slot] = remaining > 0 ? current.withCount(remaining) : SimpleSlotItem.EMPTY;
        }

        return current.withCount(extracted);
    }

    public int getTanks() {
        return tanks.size();
    }

    public SimpleFluidTank getFluidInTank(int tankIndex) {
        if (tankIndex < 0 || tankIndex >= tanks.size()) {
            return SimpleFluidTank.EMPTY;
        }
        return tanks.get(tankIndex);
    }

    /**
     * Fills the first matching or empty fluid tank with the specified resource.
     *
     * @param resource fluid to insert
     * @param simulate true to simulate without committing
     * @return millibuckets accepted
     */
    public int fill(SimpleFluidTank resource, boolean simulate) {
        if (resource == null || resource.isEmpty()) {
            return 0;
        }

        for (int i = 0; i < tanks.size(); i++) {
            SimpleFluidTank tank = tanks.get(i);
            if (tank.isEmpty() || tank.fluidId().equals(resource.fluidId())) {
                long space = tank.getRemainingSpace();
                long accepted = Math.min(resource.millibuckets(), space);
                if (accepted > 0) {
                    if (!simulate) {
                        tanks.set(i, tank.isEmpty()
                                ? new SimpleFluidTank(resource.fluidId(), accepted, tank.capacityMb())
                                : tank.withVolume(tank.millibuckets() + accepted));
                    }
                    return (int) accepted;
                }
            }
        }
        return 0;
    }

    /**
     * Drains fluid from the first non-empty tank matching the requested quantity.
     *
     * @param maxDrain maximum millibuckets to extract
     * @param simulate true to simulate without committing
     * @return drained fluid tank snapshot
     */
    public SimpleFluidTank drain(int maxDrain, boolean simulate) {
        if (maxDrain <= 0) {
            return SimpleFluidTank.EMPTY;
        }

        for (int i = 0; i < tanks.size(); i++) {
            SimpleFluidTank tank = tanks.get(i);
            if (!tank.isEmpty()) {
                long drained = Math.min(maxDrain, tank.millibuckets());
                SimpleFluidTank result = new SimpleFluidTank(tank.fluidId(), drained, tank.capacityMb());
                if (!simulate) {
                    tanks.set(i, tank.withVolume(tank.millibuckets() - drained));
                }
                return result;
            }
        }
        return SimpleFluidTank.EMPTY;
    }
}
