package dev.vessel.api.item;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;

/**
 * Handles durability calculation, damage absorption, and repair transitions for items.
 * <p>
 * Unifies Forge's {@code IForgeItem.setDamage} / {@code IForgeItem.damageItem} and Bukkit's
 * {@code Damageable.setDamage} / {@code ItemStack.setDurability} into a cohesive microkernel pipeline.
 * </p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ItemDurabilityTransmutator {

    private static final String KEY_DAMAGE = "damage";
    private static final String KEY_MINECRAFT_DAMAGE = "minecraft:damage";
    private static final String KEY_MAX_DAMAGE = "maxDamage";
    private static final String KEY_MINECRAFT_MAX_DAMAGE = "minecraft:max_damage";
    private static final String KEY_DURABILITY = "durability";

    public ItemDurabilityTransmutator() {
    }

    /**
     * Applies damage to an item stack and returns true if the item was destroyed.
     *
     * @param stack  item stack to damage
     * @param amount durability amount to deplete
     * @param entity entity wielding the item (can be null)
     * @return true if the item stack was destroyed (damage >= maxDamage)
     */
    public static boolean damageItem(Object stack, int amount, Object entity) {
        if (stack == null || amount <= 0) {
            return false;
        }

        int maxDamage = getMaxDamage(stack);
        if (maxDamage <= 0) {
            return false;
        }

        int currentDamage = getDamage(stack);
        int newDamage = currentDamage + amount;

        if (newDamage >= maxDamage) {
            setDamage(stack, maxDamage);
            return true;
        } else {
            setDamage(stack, newDamage);
            return false;
        }
    }

    /**
     * Retrieves the maximum durability damage the item stack can sustain.
     *
     * @param stack item stack or item representation
     * @return maximum damage, or 0 if item is unbreakable/not damageable
     */
    public static int getMaxDamage(Object stack) {
        if (stack == null) {
            return 0;
        }
        if (stack instanceof Map<?, ?> map) {
            Object max = map.get(KEY_MINECRAFT_MAX_DAMAGE);
            if (max == null) {
                max = map.get(KEY_MAX_DAMAGE);
            }
            if (max instanceof Number num) {
                return num.intValue();
            }
        }

        try {
            Method method = findMethod(stack.getClass(), "getMaxDamage");
            if (method == null) {
                method = findMethod(stack.getClass(), "getType");
            }
            if (method != null) {
                Object result = method.invoke(stack);
                if (result instanceof Number num) {
                    return num.intValue();
                } else if (result != null) {
                    Method typeMax = findMethod(result.getClass(), "getMaxDurability");
                    if (typeMax != null) {
                        Object dur = typeMax.invoke(result);
                        if (dur instanceof Number num) {
                            return num.intValue();
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Reflective fallback
        }

        return 0;
    }

    /**
     * Retrieves the current damage value of the item stack.
     *
     * @param stack item stack or metadata object
     * @return current damage integer
     */
    public static int getDamage(Object stack) {
        if (stack == null) {
            return 0;
        }
        if (stack instanceof Map<?, ?> map) {
            Object dmg = map.get(KEY_MINECRAFT_DAMAGE);
            if (dmg == null) {
                dmg = map.get(KEY_DAMAGE);
            }
            if (dmg == null) {
                dmg = map.get(KEY_DURABILITY);
            }
            if (dmg instanceof Number num) {
                return num.intValue();
            }
        }

        try {
            Method method = findMethod(stack.getClass(), "getDamage");
            if (method == null) {
                method = findMethod(stack.getClass(), "getDurability");
            }
            if (method != null) {
                Object result = method.invoke(stack);
                if (result instanceof Number num) {
                    return num.intValue();
                }
            }
        } catch (Exception ignored) {
            // Reflective fallback
        }

        return 0;
    }

    /**
     * Sets the damage value on the item stack or metadata representation.
     *
     * @param stack  target item stack or map
     * @param damage new damage integer
     */
    @SuppressWarnings("unchecked")
    public static void setDamage(Object stack, int damage) {
        if (stack == null) {
            return;
        }
        int validDamage = Math.max(0, damage);

        if (stack instanceof Map<?, ?> map) {
            Map<Object, Object> rawMap = (Map<Object, Object>) map;
            rawMap.put(KEY_MINECRAFT_DAMAGE, validDamage);
            rawMap.put(KEY_DAMAGE, validDamage);
            return;
        }

        try {
            Method method = findMethod(stack.getClass(), "setDamage", int.class);
            if (method != null) {
                method.invoke(stack, validDamage);
                return;
            }
            method = findMethod(stack.getClass(), "setDurability", short.class);
            if (method != null) {
                method.invoke(stack, (short) validDamage);
            }
        } catch (Exception ignored) {
            // Reflective fallback
        }
    }

    /**
     * Checks if the item has sustained any durability loss.
     *
     * @param stack item stack
     * @return true if damage > 0
     */
    public static boolean isDamaged(Object stack) {
        return getDamage(stack) > 0;
    }

    /**
     * Checks whether the item is capable of sustaining durability loss.
     *
     * @param stack item stack
     * @return true if maxDamage > 0
     */
    public static boolean isDamageable(Object stack) {
        return getMaxDamage(stack) > 0;
    }

    /**
     * Repairs an item stack by decreasing its damage count.
     *
     * @param stack        item stack to repair
     * @param repairAmount durability points to restore
     */
    public static void repairItem(Object stack, int repairAmount) {
        if (stack == null || repairAmount <= 0) {
            return;
        }
        int currentDamage = getDamage(stack);
        int newDamage = Math.max(0, currentDamage - repairAmount);
        setDamage(stack, newDamage);
    }

    private static Method findMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        try {
            return clazz.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
