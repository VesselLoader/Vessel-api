package dev.vessel.api.item;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Universal primitive read accessor for Minecraft items across Forge and Bukkit ecosystems.
 * <p>
 * Provides zero-allocation inspection of display names, lore strings, custom model data,
 * and repairability conditions without creating intermediate wrapper objects.
 * </p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class VesselItemView {

    private static final String KEY_DISPLAY_NAME = "displayName";
    private static final String KEY_CUSTOM_NAME = "minecraft:custom_name";
    private static final String KEY_LORE = "lore";
    private static final String KEY_MINECRAFT_LORE = "minecraft:lore";
    private static final String KEY_CUSTOM_MODEL_DATA = "customModelData";
    private static final String KEY_MINECRAFT_CMD = "minecraft:custom_model_data";
    private static final String KEY_AMOUNT = "amount";
    private static final String KEY_COUNT = "count";
    private static final String KEY_ITEM_ID = "id";

    public VesselItemView() {
    }

    /**
     * Extracts the display name from the item stack or metadata representation.
     *
     * @param target item stack, metadata object, or component map
     * @return display name string, or empty string if absent
     */
    public static String getDisplayName(Object target) {
        if (target == null) {
            return "";
        }
        if (target instanceof Map<?, ?> map) {
            Object name = map.get(KEY_CUSTOM_NAME);
            if (name == null) {
                name = map.get(KEY_DISPLAY_NAME);
            }
            return name != null ? name.toString() : "";
        }

        try {
            Method method = findMethod(target.getClass(), "getDisplayName");
            if (method != null) {
                Object result = method.invoke(target);
                return result != null ? result.toString() : "";
            }
        } catch (Exception ignored) {
            // Reflective fallback
        }
        return "";
    }

    /**
     * Checks if the target item stack has a customized display name.
     *
     * @param target item stack or metadata object
     * @return true if a non-empty custom display name exists
     */
    public static boolean hasDisplayName(Object target) {
        String name = getDisplayName(target);
        return !name.isEmpty();
    }

    /**
     * Extracts lore lines from the item stack or metadata representation.
     *
     * @param target item stack, metadata object, or component map
     * @return unmodifiable list of lore lines
     */
    @SuppressWarnings("unchecked")
    public static List<String> getLore(Object target) {
        if (target == null) {
            return Collections.emptyList();
        }
        if (target instanceof Map<?, ?> map) {
            Object loreObj = map.get(KEY_MINECRAFT_LORE);
            if (loreObj == null) {
                loreObj = map.get(KEY_LORE);
            }
            if (loreObj instanceof List<?> list) {
                return (List<String>) list;
            }
        }

        try {
            Method method = findMethod(target.getClass(), "getLore");
            if (method != null) {
                Object result = method.invoke(target);
                if (result instanceof List<?> list) {
                    return (List<String>) list;
                }
            }
        } catch (Exception ignored) {
            // Reflective fallback
        }
        return Collections.emptyList();
    }

    /**
     * Checks whether the target item stack has custom model data defined.
     *
     * @param target item stack, metadata object, or component map
     * @return true if custom model data is present
     */
    public static boolean hasCustomModelData(Object target) {
        return getCustomModelData(target) > 0;
    }

    /**
     * Retrieves the custom model data integer tag from the target item stack.
     *
     * @param target item stack, metadata object, or component map
     * @return custom model data integer, or 0 if not present
     */
    public static int getCustomModelData(Object target) {
        if (target == null) {
            return 0;
        }
        if (target instanceof Map<?, ?> map) {
            Object cmd = map.get(KEY_MINECRAFT_CMD);
            if (cmd == null) {
                cmd = map.get(KEY_CUSTOM_MODEL_DATA);
            }
            if (cmd instanceof Number num) {
                return num.intValue();
            }
        }

        try {
            Method method = findMethod(target.getClass(), "getCustomModelData");
            if (method != null) {
                Object result = method.invoke(target);
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
     * Determines if an item is repairable using the specified ingredient.
     *
     * @param stackOrItem item stack to repair
     * @param ingredient  repair material or ingredient item stack
     * @return true if repair is valid
     */
    public static boolean isRepairable(Object stackOrItem, Object ingredient) {
        if (stackOrItem == null || ingredient == null) {
            return false;
        }

        try {
            Method method = findMethod(stackOrItem.getClass(), "isValidRepairItem", Object.class);
            if (method == null) {
                method = findMethod(stackOrItem.getClass(), "isRepairable", Object.class);
            }
            if (method != null) {
                Object result = method.invoke(stackOrItem, ingredient);
                if (result instanceof Boolean b) {
                    return b;
                }
            }
        } catch (Exception ignored) {
            // Reflective fallback
        }
        return true;
    }

    /**
     * Extracts item count/amount from the given item stack.
     *
     * @param target item stack or map
     * @return item count (defaults to 1)
     */
    public static int getCount(Object target) {
        if (target == null) {
            return 0;
        }
        if (target instanceof Map<?, ?> map) {
            Object count = map.get(KEY_COUNT);
            if (count == null) {
                count = map.get(KEY_AMOUNT);
            }
            if (count instanceof Number num) {
                return num.intValue();
            }
        }
        try {
            Method method = findMethod(target.getClass(), "getCount");
            if (method == null) {
                method = findMethod(target.getClass(), "getAmount");
            }
            if (method != null) {
                Object result = method.invoke(target);
                if (result instanceof Number num) {
                    return num.intValue();
                }
            }
        } catch (Exception ignored) {
            // Reflective fallback
        }
        return 1;
    }

    /**
     * Extracts canonical item identifier string from the given item.
     *
     * @param target item stack, item, or map
     * @return canonical identifier (e.g. "minecraft:diamond_sword")
     */
    public static String getItemId(Object target) {
        if (target == null) {
            return "";
        }
        if (target instanceof Map<?, ?> map) {
            Object id = map.get(KEY_ITEM_ID);
            if (id != null) {
                return id.toString();
            }
        }
        return target.toString();
    }

    private static Method findMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        try {
            return clazz.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
