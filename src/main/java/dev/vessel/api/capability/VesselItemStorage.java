package dev.vessel.api.capability;

/**
 * Universal item storage and inventory capability interface for the VESSEL Super-API ecosystem.
 *
 * <p>Emulates Forge {@code IItemHandler} and Fabric {@code Storage<ItemVariant>},
 * supporting slot-indexed access as well as two-phase transactional insertion and extraction
 * to prevent item duplication bugs.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.1.0
 * @since 1.0.0
 */
public interface VesselItemStorage {

    /**
     * Resolves the number of inventory slots available.
     *
     * @return slot count
     */
    int getSlotCount();

    /**
     * Resolves namespaced item identifier currently in the slot.
     *
     * @param slot slot index (0 to slotCount - 1)
     * @return item identifier, or empty string if slot is empty
     */
    String getItemInSlot(int slot);

    /**
     * Resolves item count in the slot.
     *
     * @param slot slot index
     * @return item stack size
     */
    int getCountInSlot(int slot);

    /**
     * Inserts an item stack into the specified slot.
     *
     * @param slot     target slot index
     * @param itemId   namespaced item id
     * @param count    count to insert
     * @param simulate if true, calculates insertion without modifying inventory
     * @return count remaining that could not be inserted (0 = full success)
     */
    int insertItem(int slot, String itemId, int count, boolean simulate);

    /**
     * Extracts items from the specified slot.
     *
     * @param slot     target slot index
     * @param maxCount maximum count to extract
     * @param simulate if true, calculates extraction without modifying inventory
     * @return actual count extracted
     */
    int extractItem(int slot, int maxCount, boolean simulate);
}
