package dev.vessel.api.item;

import java.util.Objects;
import java.util.Optional;

/**
 * Universal custom item representation for the VESSEL Super-API ecosystem.
 *
 * <p>Enables cross-loader item creation, supporting custom durability, food stats,
 * internal energy storage, and interaction handlers across Fabric, Forge, NeoForge, and Paper.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.1.0
 * @since 1.0.0
 */
public class VesselItem {

    private final VesselItemProperties properties;
    private final VesselItemRightClickHandler rightClickHandler;

    /**
     * Constructs a custom item.
     *
     * @param properties        item properties
     * @param rightClickHandler optional action handler (can be null)
     */
    public VesselItem(VesselItemProperties properties, VesselItemRightClickHandler rightClickHandler) {
        this.properties = Objects.requireNonNull(properties, "properties cannot be null");
        this.rightClickHandler = rightClickHandler;
    }

    /**
     * Resolves the canonical namespaced identifier of this item.
     *
     * @return namespaced id
     */
    public String getId() {
        return properties.id();
    }

    /**
     * Resolves item properties record.
     *
     * @return properties
     */
    public VesselItemProperties getProperties() {
        return properties;
    }

    /**
     * Maximum stack size.
     *
     * @return stack size
     */
    public int getMaxStackSize() {
        return properties.maxStackSize();
    }

    /**
     * Maximum durability.
     *
     * @return durability
     */
    public int getDurability() {
        return properties.durability();
    }

    /**
     * Rarity string name.
     *
     * @return rarity
     */
    public String getRarity() {
        return properties.rarity();
    }

    /**
     * Checks if item is fire resistant.
     *
     * @return fire resistant flag
     */
    public boolean isFireResistant() {
        return properties.fireResistant();
    }

    /**
     * Checks if item is food.
     *
     * @return food flag
     */
    public boolean isFood() {
        return properties.isFood();
    }

    /**
     * Checks if item stores energy.
     *
     * @return true if energy capacity > 0
     */
    public boolean hasEnergyStorage() {
        return properties.energyCapacity() > 0L;
    }

    /**
     * Triggers the item's right-click behavior.
     *
     * @param player player context
     * @param world  world context
     * @param hand   hand identifier
     * @return true if handled
     */
    public boolean use(Object player, Object world, String hand) {
        if (rightClickHandler != null) {
            return rightClickHandler.onRightClick(player, world, hand);
        }
        return false;
    }

    public Optional<VesselItemRightClickHandler> getRightClickHandler() {
        return Optional.ofNullable(rightClickHandler);
    }

    @Override
    public String toString() {
        return "VesselItem[" + properties.id() + "]";
    }
}
