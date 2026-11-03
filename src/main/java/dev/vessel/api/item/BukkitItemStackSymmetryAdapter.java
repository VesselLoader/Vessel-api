package dev.vessel.api.item;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Bytecode opcode receptor transmuting upstream Bukkit {@code ItemStack} and {@code ItemMeta}
 * method invocations into native VESSEL Super-API item microkernel operations.
 * <p>
 * Target of VSTM Single-Pass ASM rewrites from:
 * {@code INVOKEVIRTUAL org/bukkit/inventory/ItemStack.method} to
 * {@code INVOKESTATIC dev/vessel/api/item/BukkitItemStackSymmetryAdapter.method}.
 * </p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class BukkitItemStackSymmetryAdapter {

    private static final String KEY_ITEM_META = "itemMeta";
    private static final String KEY_AMOUNT = "amount";

    public BukkitItemStackSymmetryAdapter() {
    }

    /**
     * Transmutation receptor for Bukkit {@code ItemStack.getItemMeta()}.
     *
     * @param itemStack Bukkit item stack or map representation
     * @return ItemMeta object or component map preserving all DataComponents
     */
    public static Object getItemMeta(Object itemStack) {
        if (itemStack == null) {
            return null;
        }

        if (itemStack instanceof Map<?, ?> map) {
            Object meta = map.get(KEY_ITEM_META);
            if (meta != null) {
                return meta;
            }
            // Return extracted DataComponents dictionary as dynamic meta
            return ComponentDataPreservingMetaBridge.extractDataComponents(itemStack);
        }

        try {
            Method method = findMethod(itemStack.getClass(), "getItemMeta");
            if (method != null) {
                return method.invoke(itemStack);
            }
        } catch (Exception ignored) {
            // Reflective fallback
        }

        return ComponentDataPreservingMetaBridge.extractDataComponents(itemStack);
    }

    /**
     * Transmutation receptor for Bukkit {@code ItemStack.setItemMeta(ItemMeta)}.
     *
     * @param itemStack target item stack
     * @param itemMeta  source ItemMeta or component map
     * @return true if metadata was successfully applied
     */
    @SuppressWarnings("unchecked")
    public static boolean setItemMeta(Object itemStack, Object itemMeta) {
        if (itemStack == null || itemMeta == null) {
            return false;
        }

        if (itemStack instanceof Map<?, ?> map) {
            Map<Object, Object> rawMap = (Map<Object, Object>) map;
            rawMap.put(KEY_ITEM_META, itemMeta);
            if (itemMeta instanceof Map<?, ?> metaMap) {
                rawMap.putAll(metaMap);
            } else {
                Map<String, Object> components = ComponentDataPreservingMetaBridge.extractDataComponents(itemMeta);
                rawMap.putAll(components);
            }
            return true;
        }

        try {
            Method method = findMethod(itemStack.getClass(), "setItemMeta", itemMeta.getClass());
            if (method != null) {
                Object res = method.invoke(itemStack, itemMeta);
                return Boolean.TRUE.equals(res);
            }
        } catch (Exception ignored) {
            // Reflective fallback
        }

        Map<String, Object> components = ComponentDataPreservingMetaBridge.extractDataComponents(itemMeta);
        ComponentDataPreservingMetaBridge.applyDataComponents(itemStack, components);
        return true;
    }

    /**
     * Transmutation receptor for Bukkit {@code ItemStack.getDurability()}.
     *
     * @param itemStack Bukkit item stack
     * @return short durability value
     */
    public static short getDurability(Object itemStack) {
        int damage = ItemDurabilityTransmutator.getDamage(itemStack);
        return (short) damage;
    }

    /**
     * Transmutation receptor for Bukkit {@code ItemStack.setDurability(short)}.
     *
     * @param itemStack  Bukkit item stack
     * @param durability new durability damage short
     */
    public static void setDurability(Object itemStack, short durability) {
        ItemDurabilityTransmutator.setDamage(itemStack, (int) durability);
    }

    /**
     * Transmutation receptor for Bukkit {@code ItemStack.getAmount()}.
     *
     * @param itemStack Bukkit item stack
     * @return amount integer
     */
    public static int getAmount(Object itemStack) {
        return VesselItemView.getCount(itemStack);
    }

    /**
     * Transmutation receptor for Bukkit {@code ItemStack.setAmount(int)}.
     *
     * @param itemStack target item stack
     * @param amount    new amount integer
     */
    @SuppressWarnings("unchecked")
    public static void setAmount(Object itemStack, int amount) {
        if (itemStack == null) {
            return;
        }
        int valid = Math.max(0, amount);
        if (itemStack instanceof Map<?, ?> map) {
            ((Map<Object, Object>) map).put(KEY_AMOUNT, valid);
            return;
        }

        try {
            Method method = findMethod(itemStack.getClass(), "setAmount", int.class);
            if (method != null) {
                method.invoke(itemStack, valid);
            }
        } catch (Exception ignored) {
            // Reflective fallback
        }
    }

    /**
     * Transmutation receptor for extracting display name from Bukkit ItemStack.
     *
     * @param itemStack Bukkit item stack
     * @return display name string
     */
    public static String getDisplayName(Object itemStack) {
        return VesselItemView.getDisplayName(itemStack);
    }

    /**
     * Transmutation receptor for extracting lore from Bukkit ItemStack.
     *
     * @param itemStack Bukkit item stack
     * @return unmodifiable list of lore strings
     */
    public static List<String> getLore(Object itemStack) {
        return VesselItemView.getLore(itemStack);
    }

    private static Method findMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        try {
            return clazz.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
