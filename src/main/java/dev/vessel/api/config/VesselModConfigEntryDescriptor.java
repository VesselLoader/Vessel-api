// ==============================================================================
// VESSEL-API — VesselModConfigEntryDescriptor.java
// Version: 1.0.0
// Changelog:
// - 1.0.0: Initial creation of immutable VesselModConfigEntryDescriptor record
//          describing a NeoForge/Fabric/Paper configuration entry in VUSA.
// ==============================================================================

package dev.vessel.api.config;

import java.util.Objects;

/**
 * Immutable descriptor of a mod or plugin configuration entry bridged across
 * NeoForge {@code ModConfigSpec}, Fabric configuration files, and Bukkit/Paper
 * YAML configurations within the VESSEL Universal Super-API (VUSA).
 *
 * @param modId        owning mod or plugin identifier (must not be {@code null} or blank)
 * @param configType   configuration scope such as {@code COMMON}, {@code SERVER}, or {@code CLIENT}
 *                     (must not be {@code null} or blank)
 * @param dotPath      hierarchical dot-separated configuration key path (must not be {@code null} or blank)
 * @param defaultValue serialized default value of the entry (must not be {@code null})
 * @param currentValue serialized active value of the entry (must not be {@code null})
 * @version 1.0.0
 */
public record VesselModConfigEntryDescriptor(
        String modId,
        String configType,
        String dotPath,
        String defaultValue,
        String currentValue) {

    /**
     * Validates all components of {@link VesselModConfigEntryDescriptor}.
     *
     * @throws NullPointerException     if any component is {@code null}
     * @throws IllegalArgumentException if {@code modId}, {@code configType}, or {@code dotPath} is blank
     */
    public VesselModConfigEntryDescriptor {
        Objects.requireNonNull(modId, "modId must not be null");
        Objects.requireNonNull(configType, "configType must not be null");
        Objects.requireNonNull(dotPath, "dotPath must not be null");
        Objects.requireNonNull(defaultValue, "defaultValue must not be null");
        Objects.requireNonNull(currentValue, "currentValue must not be null");
        if (modId.isBlank()) {
            throw new IllegalArgumentException("modId must not be blank");
        }
        if (configType.isBlank()) {
            throw new IllegalArgumentException("configType must not be blank");
        }
        if (dotPath.isBlank()) {
            throw new IllegalArgumentException("dotPath must not be blank");
        }
    }

    /**
     * Indicates whether the current configuration value differs from the default value.
     *
     * @return {@code true} if {@code !defaultValue.equals(currentValue)}
     */
    public boolean isModified() {
        return !this.defaultValue.equals(this.currentValue);
    }
}
