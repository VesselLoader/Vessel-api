// ==============================================================================
// VESSEL-API — VesselItemStackSnapshot.java
// Version: 1.0.0
// Changelog:
// - 1.0.0: Initial creation of immutable VesselItemStackSnapshot contract
//          representing a cross-loader ItemStack state in VUSA.
// ==============================================================================

package dev.vessel.api.inventory;

import java.util.Objects;

/**
 * Immutable snapshot of an item stack bridged across Vanilla DataComponents,
 * NeoForge/Fabric item registries, and Bukkit/Paper {@code ItemStack} representations.
 *
 * @version 1.0.0
 */
public final class VesselItemStackSnapshot {

    private final String materialKey;
    private final int amount;
    private final String displayName;
    private final int customModelData;

    /**
     * Constructs a new {@link VesselItemStackSnapshot}.
     *
     * @param materialKey     namespaced material/item identifier (must not be {@code null} or blank)
     * @param amount          stack count (must be {@code >= 0})
     * @param displayName     custom display name or empty string if none (must not be {@code null})
     * @param customModelData custom model data integer value
     * @throws NullPointerException     if {@code materialKey} or {@code displayName} is {@code null}
     * @throws IllegalArgumentException if {@code materialKey} is blank or {@code amount < 0}
     */
    public VesselItemStackSnapshot(
            String materialKey,
            int amount,
            String displayName,
            int customModelData) {
        Objects.requireNonNull(materialKey, "materialKey must not be null");
        Objects.requireNonNull(displayName, "displayName must not be null");
        if (materialKey.isBlank()) {
            throw new IllegalArgumentException("materialKey must not be blank");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
        this.materialKey = materialKey;
        this.amount = amount;
        this.displayName = displayName;
        this.customModelData = customModelData;
    }

    /**
     * Returns the namespaced material/item key (e.g., {@code minecraft:diamond_sword}).
     *
     * @return the material key
     */
    public String getMaterialKey() {
        return this.materialKey;
    }

    /**
     * Returns the item stack count.
     *
     * @return non-negative stack count
     */
    public int getAmount() {
        return this.amount;
    }

    /**
     * Returns the custom display name of the item stack (or empty string if default).
     *
     * @return display name string
     */
    public String getDisplayName() {
        return this.displayName;
    }

    /**
     * Returns the custom model data value associated with the item stack.
     *
     * @return custom model data integer
     */
    public int getCustomModelData() {
        return this.customModelData;
    }

    /**
     * Determines whether this snapshot represents an empty stack ({@code amount == 0})
     * or an air item ({@code minecraft:air}).
     *
     * @return {@code true} if {@code amount == 0 || "minecraft:air".equalsIgnoreCase(materialKey)}
     */
    public boolean isAirOrEmpty() {
        return this.amount == 0 || "minecraft:air".equalsIgnoreCase(this.materialKey);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof VesselItemStackSnapshot other)) {
            return false;
        }
        return this.amount == other.amount
                && this.customModelData == other.customModelData
                && this.materialKey.equals(other.materialKey)
                && this.displayName.equals(other.displayName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.materialKey, this.amount, this.displayName, this.customModelData);
    }

    @Override
    public String toString() {
        return "VesselItemStackSnapshot{"
                + "materialKey='" + this.materialKey + '\''
                + ", amount=" + this.amount
                + ", displayName='" + this.displayName + '\''
                + ", customModelData=" + this.customModelData
                + '}';
    }
}
