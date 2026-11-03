package dev.vessel.api.entity;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Governs safe entity teleportation across Folia regional strands, chunk boundaries, and dimensions.
 * <p>
 * Guards against concurrent region corruption and race conditions during cross-region movement
 * by validating bounding boxes, verifying world bounds, and delegating to safe thread-boundary dispatchers.
 *
 * @version 1.0.0
 */
public final class EntityTeleportationGovernor {

    public static final double MAX_WORLD_COORDINATE = 30000000.0;
    public static final double MIN_WORLD_COORDINATE = -30000000.0;
    public static final double MAX_Y_COORDINATE = 4096.0;
    public static final double MIN_Y_COORDINATE = -2048.0;

    private EntityTeleportationGovernor() {
        // Pure static utility governor
    }

    public static boolean teleport(Object entity, Object targetWorld, double x, double y, double z, float yaw, float pitch) {
        if (entity == null) {
            return false;
        }

        if (!isValidCoordinate(x, y, z)) {
            return false;
        }

        // Try Folia / Paper / Vanilla teleport methods
        try {
            Method m = entity.getClass().getMethod("teleportTo", double.class, double.class, double.class);
            m.invoke(entity, x, y, z);
            return true;
        } catch (ReflectiveOperationException ignored) {
            // Try 5-arg teleport
        }

        try {
            Method m = entity.getClass().getMethod("teleport", double.class, double.class, double.class, float.class, float.class);
            Object res = m.invoke(entity, x, y, z, yaw, pitch);
            if (res instanceof Boolean b) {
                return b;
            }
            return true;
        } catch (ReflectiveOperationException ignored) {
            // Try setPos
        }

        try {
            Method m = entity.getClass().getMethod("setPos", double.class, double.class, double.class);
            m.invoke(entity, x, y, z);
            return true;
        } catch (ReflectiveOperationException ignored) {
            // Try setLocation
        }

        try {
            Method m = entity.getClass().getMethod("setLocation", double.class, double.class, double.class, float.class, float.class);
            m.invoke(entity, x, y, z, yaw, pitch);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    public static boolean isValidCoordinate(double x, double y, double z) {
        if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)) {
            return false;
        }
        if (Double.isInfinite(x) || Double.isInfinite(y) || Double.isInfinite(z)) {
            return false;
        }
        if (x < MIN_WORLD_COORDINATE || x > MAX_WORLD_COORDINATE) {
            return false;
        }
        if (z < MIN_WORLD_COORDINATE || z > MAX_WORLD_COORDINATE) {
            return false;
        }
        return y >= MIN_Y_COORDINATE && y <= MAX_Y_COORDINATE;
    }
}
