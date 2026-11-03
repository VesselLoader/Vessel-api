// ==============================================================================
// VESSEL-API — VesselNbtCompoundSnapshot.java
// Version: 1.0.0
// Changelog:
// - 1.0.0: Initial creation of immutable VesselNbtCompoundSnapshot record
//          bridging Vanilla CompoundTag, NeoForge/Fabric custom data, and VUSA.
// ==============================================================================

package dev.vessel.api.nbt;

import java.util.Map;
import java.util.Objects;

/**
 * Immutable snapshot of an NBT compound tag structure bridged across Vanilla
 * {@code CompoundTag}, NeoForge/Fabric custom data components, and plugin
 * NBT serialization pipelines within the VESSEL Universal Super-API (VUSA).
 *
 * @param rootTagName    name of the root compound tag (must not be {@code null}, may be empty)
 * @param stringEntries  immutable map of string tag keys to string values
 * @param numericEntries immutable map of numeric tag keys to 64-bit integer values
 * @version 1.0.0
 */
public record VesselNbtCompoundSnapshot(
        String rootTagName,
        Map<String, String> stringEntries,
        Map<String, Long> numericEntries) {

    /**
     * Validates {@code rootTagName} and defensively copies {@code stringEntries} and {@code numericEntries}.
     *
     * @throws NullPointerException if {@code rootTagName} or any map key/value is {@code null}
     */
    public VesselNbtCompoundSnapshot {
        Objects.requireNonNull(rootTagName, "rootTagName must not be null");
        stringEntries = stringEntries == null ? Map.of() : Map.copyOf(stringEntries);
        numericEntries = numericEntries == null ? Map.of() : Map.copyOf(numericEntries);
    }

    /**
     * Returns the total number of string and numeric tags stored in this compound snapshot.
     *
     * @return combined count of {@code stringEntries} and {@code numericEntries}
     */
    public int totalTagCount() {
        return this.stringEntries.size() + this.numericEntries.size();
    }

    /**
     * Checks whether the specified tag key exists in either {@code stringEntries} or {@code numericEntries}.
     *
     * @param key the tag key to check (may be {@code null})
     * @return {@code true} if the key exists in either map, {@code false} otherwise
     */
    public boolean containsKey(String key) {
        return key != null && (this.stringEntries.containsKey(key) || this.numericEntries.containsKey(key));
    }
}
