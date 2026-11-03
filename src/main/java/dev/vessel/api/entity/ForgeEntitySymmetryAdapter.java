package dev.vessel.api.entity;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Direct bytecode transmutation opcode receptor for Forge {@code IForgeEntity} methods.
 * <p>
 * Bridges custom Forge entity extensions (multipart entity queries, rider interactions,
 * crop trampling, and custom NBT persistence) into zero-allocation native VESSEL handlers.
 *
 * @version 1.0.0
 */
public final class ForgeEntitySymmetryAdapter {

    public static final float DEFAULT_TRAMPLE_FALL_DISTANCE_THRESHOLD = 0.5f;

    private ForgeEntitySymmetryAdapter() {
        // Pure static opcode receptor facade
    }

    public static boolean isMultipartEntity(Object entity) {
        return MultipartEntityPartRouter.getInstance().isMultipartEntity(entity);
    }

    public static Object[] getParts(Object entity) {
        return MultipartEntityPartRouter.getInstance().getParts(entity);
    }

    public static boolean canRiderInteract(Object entity) {
        if (entity == null) {
            return false;
        }
        try {
            Method m = entity.getClass().getMethod("canRiderInteract");
            Object res = m.invoke(entity);
            if (res instanceof Boolean b) {
                return b;
            }
        } catch (ReflectiveOperationException ignored) {
            // Default Forge behavior allows rider interaction
        }
        return true;
    }

    public static boolean shouldRiderSit(Object entity) {
        if (entity == null) {
            return true;
        }
        try {
            Method m = entity.getClass().getMethod("shouldRiderSit");
            Object res = m.invoke(entity);
            if (res instanceof Boolean b) {
                return b;
            }
        } catch (ReflectiveOperationException ignored) {
            // Default rider sits
        }
        return true;
    }

    public static boolean canTrample(Object entity, Object state, Object pos, float fallDistance) {
        if (entity == null) {
            return false;
        }
        try {
            Method m = entity.getClass().getMethod("canTrample", Object.class, Object.class, float.class);
            Object res = m.invoke(entity, state, pos, fallDistance);
            if (res instanceof Boolean b) {
                return b;
            }
        } catch (ReflectiveOperationException ignored) {
            // Fallback default vanilla logic: fall distance > 0.5f
        }
        return fallDistance > DEFAULT_TRAMPLE_FALL_DISTANCE_THRESHOLD;
    }
}
