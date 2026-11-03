package dev.vessel.api.entity;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Coordinates complex multipart entity hierarchies (e.g. Ender Dragon, modded bosses, Create contraptions).
 * <p>
 * Routes damage events from external hitboxes/sub-parts back to the root parent entity, maintaining
 * bi-directional parent-child registry mappings without mutating underlying entity instances.
 *
 * @version 1.0.0
 */
public final class MultipartEntityPartRouter {

    public static final Object[] EMPTY_PARTS = new Object[0];
    public static final float DEFAULT_DAMAGE_MULTIPLIER = 1.0f;

    private static final MultipartEntityPartRouter INSTANCE = new MultipartEntityPartRouter();

    private final Map<Object, Object[]> parentToParts = new ConcurrentHashMap<>();
    private final Map<Object, Object> partToParent = new ConcurrentHashMap<>();
    private final Map<Object, Float> partDamageMultipliers = new ConcurrentHashMap<>();

    public MultipartEntityPartRouter() {
    }

    public static MultipartEntityPartRouter getInstance() {
        return INSTANCE;
    }

    public boolean isMultipartEntity(Object entity) {
        if (entity == null) {
            return false;
        }
        if (parentToParts.containsKey(entity)) {
            return true;
        }
        try {
            Method m = entity.getClass().getMethod("isMultipartEntity");
            Object res = m.invoke(entity);
            if (res instanceof Boolean b) {
                return b;
            }
        } catch (ReflectiveOperationException ignored) {
            // Check parts method
        }
        try {
            Method m = entity.getClass().getMethod("getParts");
            Object res = m.invoke(entity);
            return res instanceof Object[] arr && arr.length > 0;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    public Object[] getParts(Object parent) {
        if (parent == null) {
            return EMPTY_PARTS;
        }
        Object[] registered = parentToParts.get(parent);
        if (registered != null) {
            return Arrays.copyOf(registered, registered.length);
        }
        try {
            Method m = parent.getClass().getMethod("getParts");
            Object res = m.invoke(parent);
            if (res instanceof Object[] arr) {
                return arr;
            }
        } catch (ReflectiveOperationException ignored) {
            // Unregistered or single-part entity
        }
        return EMPTY_PARTS;
    }

    public void registerMultipart(Object parent, Object[] parts) {
        Objects.requireNonNull(parent, "parent cannot be null");
        Objects.requireNonNull(parts, "parts cannot be null");

        Object[] copied = Arrays.copyOf(parts, parts.length);
        parentToParts.put(parent, copied);
        for (Object part : copied) {
            if (part != null) {
                partToParent.put(part, parent);
            }
        }
    }

    public void setPartDamageMultiplier(Object part, float multiplier) {
        Objects.requireNonNull(part, "part cannot be null");
        partDamageMultipliers.put(part, multiplier);
    }

    public Object getParent(Object part) {
        if (part == null) {
            return null;
        }
        Object parent = partToParent.get(part);
        if (parent != null) {
            return parent;
        }
        try {
            Method m = part.getClass().getMethod("getParent");
            return m.invoke(part);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    public float routePartDamage(Object parent, Object part, float damage) {
        Objects.requireNonNull(parent, "parent cannot be null");
        float multiplier = part != null ? partDamageMultipliers.getOrDefault(part, DEFAULT_DAMAGE_MULTIPLIER) : DEFAULT_DAMAGE_MULTIPLIER;
        float effectiveDamage = damage * multiplier;

        try {
            Method hurt = parent.getClass().getMethod("hurt", float.class);
            hurt.invoke(parent, effectiveDamage);
        } catch (ReflectiveOperationException ignored) {
            try {
                Method damageMethod = parent.getClass().getMethod("damage", double.class);
                damageMethod.invoke(parent, (double) effectiveDamage);
            } catch (ReflectiveOperationException ignoredAgain) {
                // Not reflective damageable
            }
        }
        return effectiveDamage;
    }

    public void unregisterMultipart(Object parent) {
        if (parent == null) {
            return;
        }
        Object[] parts = parentToParts.remove(parent);
        if (parts != null) {
            for (Object part : parts) {
                if (part != null) {
                    partToParent.remove(part);
                    partDamageMultipliers.remove(part);
                }
            }
        }
    }

    public void clear() {
        parentToParts.clear();
        partToParent.clear();
        partDamageMultipliers.clear();
    }
}
