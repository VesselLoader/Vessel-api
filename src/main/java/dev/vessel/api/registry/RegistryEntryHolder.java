package dev.vessel.api.registry;

import java.util.Objects;

/**
 * Immutable holder representing a registered entry in the Vessel Universal Registry.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner or nested classes.</p>
 *
 * @param registryKey         target registry identifier (e.g., "minecraft:item")
 * @param entryKey            namespaced entry identifier (e.g., "create:wrench")
 * @param value               registered object instance or supplier reference
 * @param loaderKind          originating mod loader dialect (e.g., "FORGE", "NEOFORGE", "FABRIC")
 * @param registeredTimestamp creation timestamp in milliseconds
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
public record RegistryEntryHolder(
        String registryKey,
        String entryKey,
        Object value,
        String loaderKind,
        long registeredTimestamp
) {
    public RegistryEntryHolder {
        Objects.requireNonNull(registryKey, "registryKey cannot be null");
        Objects.requireNonNull(entryKey, "entryKey cannot be null");
        Objects.requireNonNull(value, "value cannot be null");
        Objects.requireNonNull(loaderKind, "loaderKind cannot be null");
    }

    /**
     * Resolves typed entry value.
     *
     * @param targetClass expected type class
     * @param <T>         target type
     * @return typed instance
     * @throws ClassCastException if entry value cannot be cast
     */
    @SuppressWarnings("unchecked")
    public <T> T asType(Class<T> targetClass) {
        Objects.requireNonNull(targetClass, "targetClass cannot be null");
        return (T) value;
    }
}
