// ==============================================================================
// VESSEL-API — VesselResourceReloadSnapshot.java
// Version: 1.0.0
// Changelog:
// - 1.0.0: Initial creation of immutable VesselResourceReloadSnapshot record
//          capturing server data-pack and resource reload telemetry in VUSA.
// ==============================================================================

package dev.vessel.api.resource;

import java.util.List;
import java.util.Objects;

/**
 * Immutable snapshot of a resource or server data-pack reload cycle executed across
 * Vanilla {@code PreparableReloadListener}, Fabric {@code IdentifiableResourceReloadListener},
 * and NeoForge {@code AddReloadListenerEvent} bridges.
 *
 * @param resourceType   resource pack type such as {@code SERVER_DATA} or {@code CLIENT_RESOURCES}
 *                       (must not be {@code null} or blank)
 * @param listenerIds    immutable list of registered reload listener identifiers
 * @param durationMicros elapsed reload execution time in microseconds (must be {@code >= 0})
 * @version 1.0.0
 */
public record VesselResourceReloadSnapshot(
        String resourceType,
        List<String> listenerIds,
        long durationMicros) {

    /**
     * Validates all components of {@link VesselResourceReloadSnapshot} and defensively copies {@code listenerIds}.
     *
     * @throws NullPointerException     if {@code resourceType} or any element in {@code listenerIds} is {@code null}
     * @throws IllegalArgumentException if {@code resourceType} is blank or {@code durationMicros < 0}
     */
    public VesselResourceReloadSnapshot {
        Objects.requireNonNull(resourceType, "resourceType must not be null");
        if (resourceType.isBlank()) {
            throw new IllegalArgumentException("resourceType must not be blank");
        }
        if (durationMicros < 0L) {
            throw new IllegalArgumentException("durationMicros must not be negative");
        }
        listenerIds = listenerIds == null ? List.of() : List.copyOf(listenerIds);
    }

    /**
     * Returns the number of reload listeners executed in this snapshot.
     *
     * @return listener count
     */
    public int listenerCount() {
        return this.listenerIds.size();
    }
}
