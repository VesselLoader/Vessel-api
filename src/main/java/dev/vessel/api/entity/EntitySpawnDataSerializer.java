package dev.vessel.api.entity;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Serializes and deserializes custom entity network spawn payloads.
 * <p>
 * Bridges Forge {@code IEntityAdditionalSpawnData} network contracts into universal byte streams
 * and Netty/NIO buffers without heap thrashing during chunk loading and entity spawning.
 *
 * @version 1.0.0
 */
public final class EntitySpawnDataSerializer {

    public static final int MAX_SPAWN_PAYLOAD_BYTES = 1048576;

    private EntitySpawnDataSerializer() {
        // Pure static utility serializer
    }

    public static boolean writeSpawnData(Object entity, Object buffer) {
        if (entity == null || buffer == null) {
            return false;
        }

        Method[] methods = entity.getClass().getMethods();
        for (Method method : methods) {
            if ("writeSpawnData".equals(method.getName()) && method.getParameterCount() == 1) {
                Class<?> paramType = method.getParameterTypes()[0];
                if (paramType.isAssignableFrom(buffer.getClass())) {
                    try {
                        method.invoke(entity, buffer);
                        return true;
                    } catch (ReflectiveOperationException ignored) {
                        return false;
                    }
                }
            }
        }
        return false;
    }

    public static boolean readSpawnData(Object entity, Object buffer) {
        if (entity == null || buffer == null) {
            return false;
        }

        Method[] methods = entity.getClass().getMethods();
        for (Method method : methods) {
            if ("readSpawnData".equals(method.getName()) && method.getParameterCount() == 1) {
                Class<?> paramType = method.getParameterTypes()[0];
                if (paramType.isAssignableFrom(buffer.getClass())) {
                    try {
                        method.invoke(entity, buffer);
                        return true;
                    } catch (ReflectiveOperationException ignored) {
                        return false;
                    }
                }
            }
        }
        return false;
    }
}
