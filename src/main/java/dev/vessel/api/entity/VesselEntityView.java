package dev.vessel.api.entity;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * High-performance primitive read accessor for entity state.
 * <p>
 * Eliminates heap allocations and boxing overhead during high-frequency entity property queries,
 * providing direct primitive float, double, and int view operations across Bukkit and Forge entities.
 *
 * @version 1.0.0
 */
public final class VesselEntityView {

    public static final int DEFAULT_ENTITY_ID = -1;
    public static final float DEFAULT_HEALTH = 0.0f;
    public static final float DEFAULT_MAX_HEALTH = 20.0f;
    public static final double DEFAULT_VELOCITY = 0.0;

    private VesselEntityView() {
        // Pure static utility accessor
    }

    public static int getEntityId(Object entity) {
        if (entity == null) {
            return DEFAULT_ENTITY_ID;
        }
        try {
            Method m = entity.getClass().getMethod("getId");
            Object res = m.invoke(entity);
            if (res instanceof Number num) {
                return num.intValue();
            }
        } catch (ReflectiveOperationException ignored) {
            // Check Bukkit alternative
        }
        try {
            Method m = entity.getClass().getMethod("getEntityId");
            Object res = m.invoke(entity);
            if (res instanceof Number num) {
                return num.intValue();
            }
        } catch (ReflectiveOperationException ignored) {
            // Unresolved ID
        }
        return System.identityHashCode(entity);
    }

    public static float getHealth(Object entity) {
        if (entity == null) {
            return DEFAULT_HEALTH;
        }
        try {
            Method m = entity.getClass().getMethod("getHealth");
            Object res = m.invoke(entity);
            if (res instanceof Number num) {
                return num.floatValue();
            }
        } catch (ReflectiveOperationException ignored) {
            // Non-living or unmapped entity
        }
        return DEFAULT_HEALTH;
    }

    public static float getMaxHealth(Object entity) {
        if (entity == null) {
            return DEFAULT_MAX_HEALTH;
        }
        try {
            Method m = entity.getClass().getMethod("getMaxHealth");
            Object res = m.invoke(entity);
            if (res instanceof Number num) {
                return num.floatValue();
            }
        } catch (ReflectiveOperationException ignored) {
            // Fallback to default
        }
        return DEFAULT_MAX_HEALTH;
    }

    public static boolean isDead(Object entity) {
        if (entity == null) {
            return true;
        }
        try {
            Method m = entity.getClass().getMethod("isDead");
            Object res = m.invoke(entity);
            if (res instanceof Boolean b) {
                return b;
            }
        } catch (ReflectiveOperationException ignored) {
            // Check isAlive
        }
        try {
            Method m = entity.getClass().getMethod("isAlive");
            Object res = m.invoke(entity);
            if (res instanceof Boolean b) {
                return !b;
            }
        } catch (ReflectiveOperationException ignored) {
            // Check health
        }
        return getHealth(entity) <= 0.0f;
    }

    public static double getVelocityX(Object entity) {
        if (entity == null) {
            return DEFAULT_VELOCITY;
        }
        try {
            Method m = entity.getClass().getMethod("getDeltaMovementX");
            Object res = m.invoke(entity);
            if (res instanceof Number num) {
                return num.doubleValue();
            }
        } catch (ReflectiveOperationException ignored) {
            // Try getVelocity
        }
        try {
            Method m = entity.getClass().getMethod("getVelocity");
            Object vec = m.invoke(entity);
            if (vec != null) {
                Method getX = vec.getClass().getMethod("getX");
                Object x = getX.invoke(vec);
                if (x instanceof Number num) {
                    return num.doubleValue();
                }
            }
        } catch (ReflectiveOperationException ignored) {
            // Fallback
        }
        return DEFAULT_VELOCITY;
    }

    public static double getVelocityY(Object entity) {
        if (entity == null) {
            return DEFAULT_VELOCITY;
        }
        try {
            Method m = entity.getClass().getMethod("getDeltaMovementY");
            Object res = m.invoke(entity);
            if (res instanceof Number num) {
                return num.doubleValue();
            }
        } catch (ReflectiveOperationException ignored) {
            // Try getVelocity
        }
        try {
            Method m = entity.getClass().getMethod("getVelocity");
            Object vec = m.invoke(entity);
            if (vec != null) {
                Method getY = vec.getClass().getMethod("getY");
                Object y = getY.invoke(vec);
                if (y instanceof Number num) {
                    return num.doubleValue();
                }
            }
        } catch (ReflectiveOperationException ignored) {
            // Fallback
        }
        return DEFAULT_VELOCITY;
    }

    public static double getVelocityZ(Object entity) {
        if (entity == null) {
            return DEFAULT_VELOCITY;
        }
        try {
            Method m = entity.getClass().getMethod("getDeltaMovementZ");
            Object res = m.invoke(entity);
            if (res instanceof Number num) {
                return num.doubleValue();
            }
        } catch (ReflectiveOperationException ignored) {
            // Try getVelocity
        }
        try {
            Method m = entity.getClass().getMethod("getVelocity");
            Object vec = m.invoke(entity);
            if (vec != null) {
                Method getZ = vec.getClass().getMethod("getZ");
                Object z = getZ.invoke(vec);
                if (z instanceof Number num) {
                    return num.doubleValue();
                }
            }
        } catch (ReflectiveOperationException ignored) {
            // Fallback
        }
        return DEFAULT_VELOCITY;
    }
}
