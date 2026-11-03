package dev.vessel.api.registry;

/**
 * Functional callback listener matching Fabric's RegistryEntryAddedCallback contract.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner or nested classes.</p>
 *
 * @param <T> registered entry object type
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
@FunctionalInterface
public interface FabricEntryAddedListener<T> {

    /**
     * Invoked when an entry is added to a Fabric-compatible registry.
     *
     * @param rawId  numerical or hash ID of the entry
     * @param id     namespaced identifier string
     * @param object registered object instance
     */
    void onEntryAdded(int rawId, String id, T object);
}
