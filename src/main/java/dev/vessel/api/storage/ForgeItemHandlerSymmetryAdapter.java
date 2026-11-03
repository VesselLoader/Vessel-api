package dev.vessel.api.storage;

import java.util.Objects;

/**
 * Direct opcode-level symmetry receptor and adapter for Forge and NeoForge
 * {@code IItemHandler} and {@code IFluidHandler} capabilities.
 *
 * <p>Receives rewritten static method calls from transmutated Forge bytecode across Eras E1b to E4b,
 * eliminating intermediate wrapper proxy allocations and ensuring HotSpot C2 JIT inlining.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ForgeItemHandlerSymmetryAdapter {

    public static final String FORGE_ITEM_HANDLER_INTERNAL = "net/minecraftforge/items/IItemHandler";
    public static final String FORGE_FLUID_HANDLER_INTERNAL = "net/minecraftforge/fluids/capability/IFluidHandler";
    public static final String RECEPTOR_CLASS_INTERNAL = "dev/vessel/api/storage/ForgeItemHandlerSymmetryAdapter";

    public static final int ACTION_EXECUTE = 0;
    public static final int ACTION_SIMULATE = 1;

    /**
     * Constructs a new Forge item handler symmetry adapter instance.
     */
    public ForgeItemHandlerSymmetryAdapter() {
    }

    /**
     * Direct opcode receptor for {@code IItemHandler.getSlots()}.
     *
     * @param target target inventory object
     * @return slot count
     */
    public static int getSlots(Object target) {
        if (target instanceof SimpleForgeInventoryContainer container) {
            return container.getSlots();
        }
        return 0;
    }

    /**
     * Direct opcode receptor for {@code IItemHandler.getStackInSlot(int slot)}.
     *
     * @param target target inventory object
     * @param slot slot index
     * @return stack in slot or SimpleSlotItem.EMPTY
     */
    public static Object getStackInSlot(Object target, int slot) {
        if (target instanceof SimpleForgeInventoryContainer container) {
            return container.getStackInSlot(slot);
        }
        return SimpleSlotItem.EMPTY;
    }

    /**
     * Direct opcode receptor for {@code IItemHandler.insertItem(int slot, ItemStack stack, boolean simulate)}.
     *
     * @param target target inventory object
     * @param slot slot index
     * @param stack item stack object
     * @param simulate true to simulate
     * @return remaining stack
     */
    public static Object insertItem(Object target, int slot, Object stack, boolean simulate) {
        if (target instanceof SimpleForgeInventoryContainer container && stack instanceof SimpleSlotItem item) {
            return container.insertItem(slot, item, simulate);
        }
        return stack;
    }

    /**
     * Direct opcode receptor for {@code IItemHandler.extractItem(int slot, int amount, boolean simulate)}.
     *
     * @param target target inventory object
     * @param slot slot index
     * @param amount maximum amount to extract
     * @param simulate true to simulate
     * @return extracted item stack
     */
    public static Object extractItem(Object target, int slot, int amount, boolean simulate) {
        if (target instanceof SimpleForgeInventoryContainer container) {
            return container.extractItem(slot, amount, simulate);
        }
        return SimpleSlotItem.EMPTY;
    }

    /**
     * Direct opcode receptor for {@code IItemHandler.getSlotLimit(int slot)}.
     *
     * @param target target inventory object
     * @param slot slot index
     * @return maximum limit for slot
     */
    public static int getSlotLimit(Object target, int slot) {
        if (target instanceof SimpleForgeInventoryContainer container) {
            return container.getSlotLimit(slot);
        }
        return SimpleForgeInventoryContainer.DEFAULT_SLOT_LIMIT;
    }

    /**
     * Direct opcode receptor for {@code IItemHandler.isItemValid(int slot, ItemStack stack)}.
     *
     * @param target target inventory object
     * @param slot slot index
     * @param stack item stack object
     * @return true if valid
     */
    public static boolean isItemValid(Object target, int slot, Object stack) {
        if (target instanceof SimpleForgeInventoryContainer container && stack instanceof SimpleSlotItem item) {
            return container.isItemValid(slot, item);
        }
        return false;
    }

    /**
     * Direct opcode receptor for {@code IFluidHandler.getTanks()}.
     *
     * @param target target fluid container object
     * @return tank count
     */
    public static int getTanks(Object target) {
        if (target instanceof SimpleForgeInventoryContainer container) {
            return container.getTanks();
        }
        return 0;
    }

    /**
     * Direct opcode receptor for {@code IFluidHandler.getFluidInTank(int tank)}.
     *
     * @param target target fluid container object
     * @param tank tank index
     * @return fluid tank contents
     */
    public static Object getFluidInTank(Object target, int tank) {
        if (target instanceof SimpleForgeInventoryContainer container) {
            return container.getFluidInTank(tank);
        }
        return SimpleFluidTank.EMPTY;
    }

    /**
     * Direct opcode receptor for {@code IFluidHandler.fill(FluidStack resource, FluidAction action)}.
     *
     * @param target target fluid container object
     * @param resource fluid stack to insert
     * @param actionOrdinal 0 for EXECUTE, 1 for SIMULATE
     * @return millibuckets accepted
     */
    public static int fillFluid(Object target, Object resource, int actionOrdinal) {
        if (target instanceof SimpleForgeInventoryContainer container && resource instanceof SimpleFluidTank tank) {
            boolean simulate = (actionOrdinal == ACTION_SIMULATE);
            return container.fill(tank, simulate);
        }
        return 0;
    }

    /**
     * Direct opcode receptor for {@code IFluidHandler.drain(int maxDrain, FluidAction action)}.
     *
     * @param target target fluid container object
     * @param maxDrain maximum millibuckets to extract
     * @param actionOrdinal 0 for EXECUTE, 1 for SIMULATE
     * @return drained fluid stack
     */
    public static Object drainFluid(Object target, int maxDrain, int actionOrdinal) {
        if (target instanceof SimpleForgeInventoryContainer container) {
            boolean simulate = (actionOrdinal == ACTION_SIMULATE);
            return container.drain(maxDrain, simulate);
        }
        return SimpleFluidTank.EMPTY;
    }
}
