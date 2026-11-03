// ==============================================================================
// VESSEL-API — VesselBuiltInRegistryEntrySnapshot.java
// Version: 1.0.0
// Changelog:
// - 1.0.0: Initial creation of immutable VesselBuiltInRegistryEntrySnapshot record
//          capturing a frozen Vanilla/Mod built-in registry entry and its bound tags.
// ==============================================================================

package dev.vessel.api.registry;

import java.util.Objects;
import java.util.Set;

/**
 * Immutable snapshot of an entry registered in a Vanilla or cross-loader built-in
 * registry, including its canonical registry key, namespaced entry key, raw numerical
 * protocol ID, and bound tag identifiers.
 *
 * @param registryKey    canonical registry identifier such as {@code minecraft:block} (must not be {@code null} or blank)
 * @param entryKey       namespaced entry identifier such as {@code minecraft:diamond_ore} (must not be {@code null} or blank)
 * @param rawNumericalId non-negative raw integer ID assigned in the registry id map (must be {@code >= 0})
 * @param boundTags      immutable set of tag keys bound to this registry entry
 * @version 1.0.0
 */
public record VesselBuiltInRegistryEntrySnapshot(
        String registryKey,
        String entryKey,
        int rawNumericalId,
        Set<String> boundTags) {

    /**
     * Validates all components of {@link VesselBuiltInRegistryEntrySnapshot} and defensively copies {@code boundTags}.
     *
     * @throws NullPointerException     if {@code registryKey}, {@code entryKey}, or any element in {@code boundTags} is {@code null}
     * @throws IllegalArgumentException if {@code registryKey} or {@code entryKey} is blank, or {@code rawNumericalId < 0}
     */
    public VesselBuiltInRegistryEntrySnapshot {
        Objects.requireNonNull(registryKey, "registryKey must not be null");
        Objects.requireNonNull(entryKey, "entryKey must not be null");
        if (registryKey.isBlank()) {
            throw new IllegalArgumentException("registryKey must not be blank");
        }
        if (entryKey.isBlank()) {
            throw new IllegalArgumentException("entryKey must not be blank");
        }
        if (rawNumericalId < 0) {
            throw new IllegalArgumentException("rawNumericalId must not be negative");
        }
        boundTags = boundTags == null ? Set.of() : Set.copyOf(boundTags);
    }

    /**
     * Checks whether this registry entry is bound to the specified tag key.
     *
     * @param tagKey the tag key to check (may be {@code null})
     * @return {@code true} if {@code tagKey} is non-null and present in {@code boundTags}
     */
    public boolean hasTag(String tagKey) {
        return tagKey != null && this.boundTags.contains(tagKey);
    }
}
