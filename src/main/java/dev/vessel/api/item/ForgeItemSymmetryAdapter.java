package dev.vessel.api.item;

import java.util.Objects;

/**
 * Bytecode opcode receptor transmuting upstream Forge {@code IForgeItem} method invocations
 * into native VESSEL Super-API item microkernel operations.
 * <p>
 * Target of VSTM Single-Pass ASM rewrites from:
 * {@code INVOKEINTERFACE net/minecraftforge/common/extensions/IForgeItem.method} to
 * {@code INVOKESTATIC dev/vessel/api/item/ForgeItemSymmetryAdapter.method}.
 * </p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ForgeItemSymmetryAdapter {

    public ForgeItemSymmetryAdapter() {
    }

    /**
     * Transmutation receptor for Forge {@code IForgeItem.getBurnTime(ItemStack, RecipeType)}.
     *
     * @param itemStack  item stack to query
     * @param recipeType recipe type context (can be null)
     * @return furnace burn ticks
     */
    public static int getBurnTime(Object itemStack, Object recipeType) {
        if (itemStack == null) {
            return 0;
        }
        String itemId = VesselItemView.getItemId(itemStack);
        int burnTime = ItemBurnTimeTable.getBurnTime(itemId);
        if (burnTime > 0) {
            return burnTime;
        }
        return 0;
    }

    /**
     * Transmutation receptor for Forge {@code IForgeItem.isRepairable(ItemStack)}.
     *
     * @param itemStack item stack to check
     * @return true if item is damageable and repairable
     */
    public static boolean isRepairable(Object itemStack) {
        return ItemDurabilityTransmutator.isDamageable(itemStack);
    }

    /**
     * Transmutation receptor for Forge {@code IForgeItem.getMaxDamage(ItemStack)}.
     *
     * @param itemStack item stack
     * @return max durability damage
     */
    public static int getMaxDamage(Object itemStack) {
        return ItemDurabilityTransmutator.getMaxDamage(itemStack);
    }

    /**
     * Transmutation receptor for Forge {@code IForgeItem.getDamage(ItemStack)}.
     *
     * @param itemStack item stack
     * @return current damage integer
     */
    public static int getDamage(Object itemStack) {
        return ItemDurabilityTransmutator.getDamage(itemStack);
    }

    /**
     * Transmutation receptor for Forge {@code IForgeItem.setDamage(ItemStack, int)}.
     *
     * @param itemStack item stack
     * @param damage    new damage amount
     */
    public static void setDamage(Object itemStack, int damage) {
        ItemDurabilityTransmutator.setDamage(itemStack, damage);
    }

    /**
     * Transmutation receptor for Forge {@code IForgeItem.onItemUseFirst(ItemStack, UseOnContext)}.
     *
     * @param itemStack item stack
     * @param context   use context or player
     * @return interaction result status string (SUCCESS, PASS, CONSUME)
     */
    public static Object onItemUseFirst(Object itemStack, Object context) {
        return ItemInteractionDispatcher.onItemUseFirst(itemStack, context, null, null, null, null);
    }

    /**
     * Transmutation receptor for Forge {@code IForgeItem.doesSneakBypassUse(ItemStack, LevelReader, BlockPos, Player)}.
     *
     * @param itemStack item stack
     * @param level     level reader
     * @param pos       block position
     * @param player    player entity
     * @return true if sneak bypasses block use
     */
    public static boolean doesSneakBypassUse(Object itemStack, Object level, Object pos, Object player) {
        return false;
    }
}
