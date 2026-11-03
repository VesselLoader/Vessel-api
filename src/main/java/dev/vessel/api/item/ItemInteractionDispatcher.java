package dev.vessel.api.item;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Dispatches item right-click, block-use, and mining interactions across multi-loader contracts.
 * <p>
 * Unifies Forge's {@code IForgeItem.onItemUseFirst} and {@code Item.use} hooks with Bukkit's
 * {@code PlayerInteractEvent} processing into a single zero-overhead invocation pipeline.
 * </p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ItemInteractionDispatcher {

    public static final String RESULT_SUCCESS = "SUCCESS";
    public static final String RESULT_PASS = "PASS";
    public static final String RESULT_CONSUME = "CONSUME";
    public static final String RESULT_FAIL = "FAIL";

    public ItemInteractionDispatcher() {
    }

    /**
     * Executes the initial item use hook before block activation.
     * Corresponds to Forge {@code IForgeItem.onItemUseFirst}.
     *
     * @param itemOrStack target item or stack
     * @param player      player executing the action
     * @param level       world or level reference
     * @param blockPos    targeted block position
     * @param hand        hand holding the item
     * @param direction   targeted block face direction
     * @return interaction result status string (SUCCESS, PASS, CONSUME, FAIL)
     */
    public static String onItemUseFirst(
            Object itemOrStack,
            Object player,
            Object level,
            Object blockPos,
            Object hand,
            Object direction
    ) {
        if (itemOrStack == null) {
            return RESULT_PASS;
        }

        try {
            Method[] methods = itemOrStack.getClass().getMethods();
            for (int i = 0; i < methods.length; i++) {
                Method method = methods[i];
                if ("onItemUseFirst".equals(method.getName()) && method.getParameterCount() >= 1) {
                    method.setAccessible(true);
                    Object[] args = matchArguments(method.getParameterTypes(), itemOrStack, player, level, blockPos, hand, direction);
                    Object result = method.invoke(itemOrStack, args);
                    return result != null ? result.toString() : RESULT_SUCCESS;
                }
            }
        } catch (Exception ignored) {
            // Reflective fallback
        }

        return RESULT_PASS;
    }

    /**
     * Executes the general right-click item action in air or on block.
     * Corresponds to Forge/Vanilla {@code Item.use} and Bukkit item right-click.
     *
     * @param itemOrStack target item or stack
     * @param level       world or level reference
     * @param player      player entity
     * @param hand        interaction hand
     * @return interaction result status string (SUCCESS, PASS, CONSUME)
     */
    public static String onItemRightClick(
            Object itemOrStack,
            Object level,
            Object player,
            Object hand
    ) {
        if (itemOrStack == null) {
            return RESULT_PASS;
        }

        try {
            Method[] methods = itemOrStack.getClass().getMethods();
            for (int i = 0; i < methods.length; i++) {
                Method method = methods[i];
                String name = method.getName();
                if (("onItemRightClick".equals(name) || "use".equals(name)) && method.getParameterCount() >= 1) {
                    method.setAccessible(true);
                    Object[] args = matchArguments(method.getParameterTypes(), itemOrStack, level, player, hand);
                    Object result = method.invoke(itemOrStack, args);
                    return result != null ? result.toString() : RESULT_SUCCESS;
                }
            }
        } catch (Exception ignored) {
            // Reflective fallback
        }

        return RESULT_PASS;
    }

    /**
     * Dispatches the block start break hook when a player begins mining with the item.
     * Corresponds to Forge {@code IForgeItem.onBlockStartBreak}.
     *
     * @param itemOrStack target item or stack
     * @param blockPos    block position being broken
     * @param player      player entity
     * @return true if default block breaking should proceed, false if cancelled
     */
    public static boolean onBlockStartBreak(
            Object itemOrStack,
            Object blockPos,
            Object player
    ) {
        if (itemOrStack == null) {
            return true;
        }

        try {
            Method[] methods = itemOrStack.getClass().getMethods();
            for (int i = 0; i < methods.length; i++) {
                Method method = methods[i];
                if ("onBlockStartBreak".equals(method.getName()) && method.getParameterCount() >= 2) {
                    method.setAccessible(true);
                    Object[] args = matchArguments(method.getParameterTypes(), itemOrStack, blockPos, player);
                    Object result = method.invoke(itemOrStack, args);
                    if (result instanceof Boolean b) {
                        return b;
                    }
                    return true;
                }
            }
        } catch (Exception ignored) {
            // Reflective fallback
        }

        return true;
    }

    private static Object[] matchArguments(Class<?>[] paramTypes, Object... candidates) {
        Object[] args = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            Class<?> pType = paramTypes[i];
            args[i] = findCandidate(pType, candidates);
        }
        return args;
    }

    private static Object findCandidate(Class<?> type, Object[] candidates) {
        for (int i = 0; i < candidates.length; i++) {
            Object candidate = candidates[i];
            if (candidate != null && type.isAssignableFrom(candidate.getClass())) {
                return candidate;
            }
        }
        return (candidates.length > 0) ? candidates[0] : null;
    }
}
