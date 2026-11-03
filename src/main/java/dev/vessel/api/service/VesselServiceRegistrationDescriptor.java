// ==============================================================================
// VESSEL-API — VesselServiceRegistrationDescriptor.java
// Version: 1.0.0
// Changelog:
// - 1.0.0: Initial creation of immutable VesselServiceRegistrationDescriptor record
//          describing a Bukkit ServicesManager / VUSA service registration.
// ==============================================================================

package dev.vessel.api.service;

import java.util.Objects;

/**
 * Immutable descriptor of a service provider registered within the VESSEL
 * Universal Super-API (VUSA) and Bukkit {@code ServicesManager} bridge.
 *
 * @param serviceInterfaceName canonical or binary name of the service interface (must not be {@code null} or blank)
 * @param providerClassName    canonical or binary name of the provider implementation (must not be {@code null} or blank)
 * @param ownerPluginId        identifier of the owning plugin or mod (must not be {@code null} or blank)
 * @param priorityLevel        registration priority level such as {@code LOWEST}, {@code LOW}, {@code NORMAL},
 *                             {@code HIGH}, or {@code HIGHEST} (must not be {@code null} or blank)
 * @version 1.0.0
 */
public record VesselServiceRegistrationDescriptor(
        String serviceInterfaceName,
        String providerClassName,
        String ownerPluginId,
        String priorityLevel) {

    /**
     * Validates all components of {@link VesselServiceRegistrationDescriptor}.
     *
     * @throws NullPointerException     if any string component is {@code null}
     * @throws IllegalArgumentException if any string component is blank
     */
    public VesselServiceRegistrationDescriptor {
        Objects.requireNonNull(serviceInterfaceName, "serviceInterfaceName must not be null");
        Objects.requireNonNull(providerClassName, "providerClassName must not be null");
        Objects.requireNonNull(ownerPluginId, "ownerPluginId must not be null");
        Objects.requireNonNull(priorityLevel, "priorityLevel must not be null");
        if (serviceInterfaceName.isBlank()) {
            throw new IllegalArgumentException("serviceInterfaceName must not be blank");
        }
        if (providerClassName.isBlank()) {
            throw new IllegalArgumentException("providerClassName must not be blank");
        }
        if (ownerPluginId.isBlank()) {
            throw new IllegalArgumentException("ownerPluginId must not be blank");
        }
        if (priorityLevel.isBlank()) {
            throw new IllegalArgumentException("priorityLevel must not be blank");
        }
    }

    /**
     * Checks whether this service registration is configured with the {@code HIGHEST} priority level.
     *
     * @return {@code true} if {@code "HIGHEST".equalsIgnoreCase(priorityLevel)}
     */
    public boolean isHighestPriority() {
        return "HIGHEST".equalsIgnoreCase(this.priorityLevel);
    }
}
