package dev.vessel.api.registry;

/**
 * Callback handler invoked when a new entry is mounted into the universal registry.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner or nested classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
@FunctionalInterface
public interface RegistryCallbackHandler {

    /**
     * Handles notification of a newly registered entry.
     *
     * @param registryKey canonical registry identifier
     * @param entryKey    namespaced entry identifier
     * @param entry       registered instance or supplier
     */
    void onEntryAdded(String registryKey, String entryKey, Object entry);
}
