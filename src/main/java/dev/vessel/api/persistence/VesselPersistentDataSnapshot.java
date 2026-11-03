// ==============================================================================
// VESSEL-API — VesselPersistentDataSnapshot.java
// Version: 1.0.0
// Changelog:
// - 1.0.0: Initial creation of immutable VesselPersistentDataSnapshot record
//          bridging Bukkit PersistentDataContainer and Vanilla/Mod NBT data.
// ==============================================================================

package dev.vessel.api.persistence;

import java.util.Map;
import java.util.Objects;

/**
 * Immutable point-in-time snapshot of key-value persistent data entries bound
 * to a holder (entity, chunk, tile entity, or item stack) across Bukkit
 * {@code PersistentDataContainer} and Vanilla/Mod custom data storage.
 *
 * @param ownerIdentifier unique identifier of the holder owning the persistent data (must not be {@code null} or blank)
 * @param entries         immutable map of namespaced keys to serialized values
 * @version 1.0.0
 */
public record VesselPersistentDataSnapshot(
        String ownerIdentifier,
        Map<String, String> entries) {

    /**
     * Validates {@code ownerIdentifier} and defensively copies {@code entries}.
     *
     * @throws NullPointerException     if {@code ownerIdentifier} or any map key/value is {@code null}
     * @throws IllegalArgumentException if {@code ownerIdentifier} is blank
     */
    public VesselPersistentDataSnapshot {
        Objects.requireNonNull(ownerIdentifier, "ownerIdentifier must not be null");
        if (ownerIdentifier.isBlank()) {
            throw new IllegalArgumentException("ownerIdentifier must not be blank");
        }
        entries = entries == null ? Map.of() : Map.copyOf(entries);
    }

    /**
     * Checks whether the specified namespaced key exists in this snapshot.
     *
     * @param namespacedKey the namespaced key to check (may be {@code null})
     * @return {@code true} if present, {@code false} otherwise
     */
    public boolean hasKey(String namespacedKey) {
        return namespacedKey != null && this.entries.containsKey(namespacedKey);
    }

    /**
     * Retrieves the serialized value associated with the given namespaced key.
     *
     * @param namespacedKey the namespaced key to look up (may be {@code null})
     * @return the associated value, or {@code null} if not present
     */
    public String getValue(String namespacedKey) {
        return namespacedKey == null ? null : this.entries.get(namespacedKey);
    }
}
