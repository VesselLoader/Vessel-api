package dev.vessel.api.item;

import java.util.Objects;

/**
 * Fluent builder for creating high-performance {@link VesselItem} instances.
 *
 * <p>Provides a concise, intuitive Developer Experience modeled after Fabric's {@code FabricItemSettings}
 * and Forge's {@code Item.Properties}, without requiring vanilla Minecraft dependencies.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.1.0
 * @since 1.0.0
 */
public final class VesselItemBuilder {

    private final String id;
    private int maxStackSize;
    private int durability;
    private String rarity;
    private boolean fireResistant;
    private boolean isFood;
    private int nutrition;
    private float saturation;
    private boolean alwaysEdible;
    private long energyCapacity;
    private VesselItemRightClickHandler rightClickHandler;

    private VesselItemBuilder(String id) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.maxStackSize = 64;
        this.durability = 0;
        this.rarity = "COMMON";
        this.fireResistant = false;
        this.isFood = false;
        this.nutrition = 0;
        this.saturation = 0.0f;
        this.alwaysEdible = false;
        this.energyCapacity = 0L;
        this.rightClickHandler = null;
    }

    /**
     * Initializes a builder for a given namespaced item id.
     *
     * @param id namespaced id (e.g. {@code "mymod:plasma_blade"})
     * @return builder
     */
    public static VesselItemBuilder create(String id) {
        return new VesselItemBuilder(id);
    }

    public VesselItemBuilder maxStackSize(int maxStackSize) {
        this.maxStackSize = Math.max(1, maxStackSize);
        return this;
    }

    public VesselItemBuilder durability(int durability) {
        this.durability = Math.max(0, durability);
        if (durability > 0) {
            this.maxStackSize = 1;
        }
        return this;
    }

    public VesselItemBuilder rarity(String rarity) {
        this.rarity = Objects.requireNonNull(rarity, "rarity cannot be null");
        return this;
    }

    public VesselItemBuilder fireResistant(boolean fireResistant) {
        this.fireResistant = fireResistant;
        return this;
    }

    public VesselItemBuilder food(int nutrition, float saturation, boolean alwaysEdible) {
        this.isFood = true;
        this.nutrition = nutrition;
        this.saturation = saturation;
        this.alwaysEdible = alwaysEdible;
        return this;
    }

    public VesselItemBuilder energyCapacity(long energyCapacity) {
        this.energyCapacity = Math.max(0L, energyCapacity);
        return this;
    }

    public VesselItemBuilder onRightClick(VesselItemRightClickHandler handler) {
        this.rightClickHandler = handler;
        return this;
    }

    /**
     * Builds and returns the configured {@link VesselItem}.
     *
     * @return constructed item
     */
    public VesselItem build() {
        VesselItemProperties props = new VesselItemProperties(
                id,
                maxStackSize,
                durability,
                rarity,
                fireResistant,
                isFood,
                nutrition,
                saturation,
                alwaysEdible,
                energyCapacity
        );
        return new VesselItem(props, rightClickHandler);
    }
}
