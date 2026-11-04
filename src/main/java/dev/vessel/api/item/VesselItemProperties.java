package dev.vessel.api.item;

/**
 * Immutable specification record defining item properties in the VESSEL Super-API ecosystem.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @param id             canonical namespaced identifier (e.g. {@code "mymod:plasma_blade"})
 * @param maxStackSize   maximum item stack size (typically 1 to 64)
 * @param durability     maximum tool durability (0 for non-damageable items)
 * @param rarity         item rarity category (COMMON, UNCOMMON, RARE, EPIC)
 * @param fireResistant  whether item immune to fire/lava destruction
 * @param isFood         whether item is edible
 * @param nutrition      food nutrition points
 * @param saturation     food saturation modifier
 * @param alwaysEdible   whether can be eaten at full hunger
 * @param energyCapacity maximum FE / Fabric Energy capacity
 * @version 1.1.0
 * @since 1.0.0
 */
public record VesselItemProperties(
        String id,
        int maxStackSize,
        int durability,
        String rarity,
        boolean fireResistant,
        boolean isFood,
        int nutrition,
        float saturation,
        boolean alwaysEdible,
        long energyCapacity
) {
    public static VesselItemProperties ofDefault(String id) {
        return new VesselItemProperties(
                id,
                64,
                0,
                "COMMON",
                false,
                false,
                0,
                0.0f,
                false,
                0L
        );
    }
}
