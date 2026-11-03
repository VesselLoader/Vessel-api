package dev.vessel.api.entity;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Direct bytecode transmutation opcode receptor for Bukkit {@code LivingEntity} contracts.
 * <p>
 * Bridges Bukkit health modification, damage processing, and location teleportation
 * into native {@link VesselEntityView} and {@link EntityTeleportationGovernor} invocations.
 *
 * @version 1.0.0
 */
public final class BukkitLivingEntitySymmetryAdapter {

    public static final double MIN_HEALTH = 0.0;
    public static final float DEFAULT_YAW = 0.0f;
    public static final float DEFAULT_PITCH = 0.0f;

    private BukkitLivingEntitySymmetryAdapter() {
        // Pure static opcode receptor facade
    }

    public static double getHealth(Object livingEntity) {
        return (double) VesselEntityView.getHealth(livingEntity);
    }

    public static void setHealth(Object livingEntity, double health) {
        if (livingEntity == null) {
            return;
        }
        double sanitized = Math.max(health, MIN_HEALTH);
        try {
            Method m = livingEntity.getClass().getMethod("setHealth", double.class);
            m.invoke(livingEntity, sanitized);
            return;
        } catch (ReflectiveOperationException ignored) {
            // Try float overload (vanilla / Mojmap)
        }
        try {
            Method m = livingEntity.getClass().getMethod("setHealth", float.class);
            m.invoke(livingEntity, (float) sanitized);
        } catch (ReflectiveOperationException ignored) {
            // Unmapped entity
        }
    }

    public static double getMaxHealth(Object livingEntity) {
        return (double) VesselEntityView.getMaxHealth(livingEntity);
    }

    public static void damage(Object livingEntity, double amount) {
        if (livingEntity == null) {
            return;
        }
        try {
            Method m = livingEntity.getClass().getMethod("damage", double.class);
            m.invoke(livingEntity, amount);
            return;
        } catch (ReflectiveOperationException ignored) {
            // Try float hurt
        }
        try {
            Method m = livingEntity.getClass().getMethod("hurt", float.class);
            m.invoke(livingEntity, (float) amount);
        } catch (ReflectiveOperationException ignored) {
            // Fallback setHealth
            double current = getHealth(livingEntity);
            setHealth(livingEntity, Math.max(0.0, current - amount));
        }
    }

    public static void damage(Object livingEntity, double amount, Object source) {
        if (livingEntity == null) {
            return;
        }
        try {
            Method m = livingEntity.getClass().getMethod("damage", double.class, Object.class);
            m.invoke(livingEntity, amount, source);
            return;
        } catch (ReflectiveOperationException ignored) {
            // Fallback single-arg damage
            damage(livingEntity, amount);
        }
    }

    public static boolean teleport(Object entity, Object location) {
        if (entity == null || location == null) {
            return false;
        }
        try {
            Method getWorld = location.getClass().getMethod("getWorld");
            Method getX = location.getClass().getMethod("getX");
            Method getY = location.getClass().getMethod("getY");
            Method getZ = location.getClass().getMethod("getZ");
            Method getYaw = location.getClass().getMethod("getYaw");
            Method getPitch = location.getClass().getMethod("getPitch");

            Object world = getWorld.invoke(location);
            double x = ((Number) getX.invoke(location)).doubleValue();
            double y = ((Number) getY.invoke(location)).doubleValue();
            double z = ((Number) getZ.invoke(location)).doubleValue();
            float yaw = ((Number) getYaw.invoke(location)).floatValue();
            float pitch = ((Number) getPitch.invoke(location)).floatValue();

            return EntityTeleportationGovernor.teleport(entity, world, x, y, z, yaw, pitch);
        } catch (ReflectiveOperationException ignored) {
            // Fallback direct teleport
            try {
                Method m = entity.getClass().getMethod("teleport", location.getClass());
                Object res = m.invoke(entity, location);
                return res instanceof Boolean b && b;
            } catch (ReflectiveOperationException ignoredAgain) {
                return false;
            }
        }
    }

    public static boolean teleport(Object entity, Object world, double x, double y, double z, float yaw, float pitch) {
        return EntityTeleportationGovernor.teleport(entity, world, x, y, z, yaw, pitch);
    }
}
