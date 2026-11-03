package dev.vessel.api.registry;

/**
 * Functional interface supplying a registry object lazily during deferred registration replay.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner or nested classes.</p>
 *
 * @param <T> supplied object type
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
@FunctionalInterface
public interface RegistryEntrySupplier<T> {

    /**
     * Supplies the registry object instance.
     *
     * @return non-null registered object
     */
    T get();
}
