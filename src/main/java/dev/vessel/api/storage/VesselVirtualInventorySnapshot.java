// ==============================================================================
// VESSEL-API — VesselVirtualInventorySnapshot.java
// Version: 1.0.0
// Changelog:
// - 1.0.0: Initial creation of immutable VesselVirtualInventorySnapshot record
//          capturing virtual GUI/Menu container state in VUSA.
// ==============================================================================

package dev.vessel.api.storage;

import java.util.Map;
import java.util.Objects;

/**
 * Immutable snapshot of a virtual or custom container inventory bridged across
 * Vanilla {@code AbstractContainerMenu}, NeoForge/Fabric menu screens, and
 * Bukkit/Paper {@code Bukkit.createInventory(...)} virtual GUIs.
 *
 * @param inventoryTypeId canonical container or inventory type identifier (must not be {@code null} or blank)
 * @param title           rendered title of the inventory view (must not be {@code null})
 * @param size            total number of slots in the inventory (must be {@code > 0})
 * @param occupiedSlots   immutable map of slot indices to item material identifiers
 * @version 1.0.0
 */
public record VesselVirtualInventorySnapshot(
        String inventoryTypeId,
        String title,
        int size,
        Map<Integer, String> occupiedSlots) {

    /**
     * Validates all components of {@link VesselVirtualInventorySnapshot} and defensively copies {@code occupiedSlots}.
     *
     * @throws NullPointerException     if {@code inventoryTypeId}, {@code title}, or any map key/value is {@code null}
     * @throws IllegalArgumentException if {@code inventoryTypeId} is blank or {@code size <= 0}
     */
    public VesselVirtualInventorySnapshot {
        Objects.requireNonNull(inventoryTypeId, "inventoryTypeId must not be null");
        Objects.requireNonNull(title, "title must not be null");
        if (inventoryTypeId.isBlank()) {
            throw new IllegalArgumentException("inventoryTypeId must not be blank");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive");
        }
        occupiedSlots = occupiedSlots == null ? Map.of() : Map.copyOf(occupiedSlots);
    }

    /**
     * Returns the number of non-empty slots recorded in this inventory snapshot.
     *
     * @return count of occupied slots
     */
    public int occupiedSlotCount() {
        return this.occupiedSlots.size();
    }

    /**
     * Checks whether the specified slot index is occupied in this inventory snapshot.
     *
     * @param slot zero-based slot index
     * @return {@code true} if {@code occupiedSlots.containsKey(slot)}
     */
    public boolean isSlotOccupied(int slot) {
        return this.occupiedSlots.containsKey(slot);
    }
}
