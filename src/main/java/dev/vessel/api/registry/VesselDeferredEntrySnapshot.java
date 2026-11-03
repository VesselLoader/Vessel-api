// ==============================================================================
// VESSEL-API — VesselDeferredEntrySnapshot.java
// Version: 1.0.0
// Changelog:
// - 1.0.0: Initial creation of immutable VesselDeferredEntrySnapshot contract
//          representing a point-in-time view of a deferred registry entry.
// ==============================================================================

package dev.vessel.api.registry;

import java.util.Objects;

/**
 * Immutable snapshot of a deferred registry entry registered through the VESSEL
 * Universal Super-API (VUSA) or cross-loader symmetry bridges.
 *
 * <p>Captures the target registry key, entry identifier, originating loader namespace,
 * and whether the entry has been bound to a frozen vanilla registry instance.</p>
 *
 * @version 1.0.0
 */
public final class VesselDeferredEntrySnapshot {

    private final String registryKey;
    private final String entryId;
    private final String sourceLoader;
    private final boolean bound;

    /**
     * Constructs a new {@link VesselDeferredEntrySnapshot}.
     *
     * @param registryKey  the canonical registry key (must not be {@code null} or blank)
     * @param entryId      the namespaced entry identifier (must not be {@code null} or blank)
     * @param sourceLoader the originating mod/plugin loader identifier (must not be {@code null} or blank)
     * @param bound        {@code true} if the entry is bound to its target registry, {@code false} otherwise
     * @throws NullPointerException     if any string parameter is {@code null}
     * @throws IllegalArgumentException if any string parameter is blank
     */
    public VesselDeferredEntrySnapshot(
            String registryKey,
            String entryId,
            String sourceLoader,
            boolean bound) {
        Objects.requireNonNull(registryKey, "registryKey must not be null");
        Objects.requireNonNull(entryId, "entryId must not be null");
        Objects.requireNonNull(sourceLoader, "sourceLoader must not be null");
        if (registryKey.isBlank()) {
            throw new IllegalArgumentException("registryKey must not be blank");
        }
        if (entryId.isBlank()) {
            throw new IllegalArgumentException("entryId must not be blank");
        }
        if (sourceLoader.isBlank()) {
            throw new IllegalArgumentException("sourceLoader must not be blank");
        }
        this.registryKey = registryKey;
        this.entryId = entryId;
        this.sourceLoader = sourceLoader;
        this.bound = bound;
    }

    /**
     * Returns the canonical registry key (e.g., {@code minecraft:block}).
     *
     * @return the registry key
     */
    public String getRegistryKey() {
        return this.registryKey;
    }

    /**
     * Returns the namespaced entry identifier (e.g., {@code examplemod:titanium_ore}).
     *
     * @return the entry identifier
     */
    public String getEntryId() {
        return this.entryId;
    }

    /**
     * Returns the originating loader identifier (e.g., {@code neoforge}, {@code fabric}, {@code paper}).
     *
     * @return the source loader name
     */
    public String getSourceLoader() {
        return this.sourceLoader;
    }

    /**
     * Indicates whether the deferred entry has been bound and frozen into the backing registry.
     *
     * @return {@code true} if bound, {@code false} otherwise
     */
    public boolean isBound() {
        return this.bound;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof VesselDeferredEntrySnapshot other)) {
            return false;
        }
        return this.bound == other.bound
                && this.registryKey.equals(other.registryKey)
                && this.entryId.equals(other.entryId)
                && this.sourceLoader.equals(other.sourceLoader);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.registryKey, this.entryId, this.sourceLoader, this.bound);
    }

    @Override
    public String toString() {
        return "VesselDeferredEntrySnapshot{"
                + "registryKey='" + this.registryKey + '\''
                + ", entryId='" + this.entryId + '\''
                + ", sourceLoader='" + this.sourceLoader + '\''
                + ", bound=" + this.bound
                + '}';
    }
}
