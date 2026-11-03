package dev.vessel.api.registry;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Immutable holder for an enqueued deferred registration entry.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner or nested classes.</p>
 *
 * @param registryKey target registry key
 * @param entryKey    namespaced entry key
 * @param supplier    entry object supplier
 * @param loaderKind  loader dialect
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
public record DeferredRegistrationEntry(
        String registryKey,
        String entryKey,
        Supplier<?> supplier,
        String loaderKind
) {
    public DeferredRegistrationEntry {
        Objects.requireNonNull(registryKey, "registryKey cannot be null");
        Objects.requireNonNull(entryKey, "entryKey cannot be null");
        Objects.requireNonNull(supplier, "supplier cannot be null");
        Objects.requireNonNull(loaderKind, "loaderKind cannot be null");
    }
}
