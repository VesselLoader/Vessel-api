// ==============================================================================
// VESSEL-API — VesselChunkStateSnapshot.java
// Version: 1.0.0
// Changelog:
// - 1.0.0: Initial creation of immutable VesselChunkStateSnapshot record
//          capturing chunk load state, ticket state, and packed coordinate key.
// ==============================================================================

package dev.vessel.api.world;

import java.util.Objects;

/**
 * Immutable point-in-time snapshot of a world chunk's residency, force-load ticket
 * status, and active entity count bridged across Vanilla {@code ServerChunkCache},
 * NeoForge/Fabric chunk ticket managers, and Bukkit {@code Chunk} contracts.
 *
 * @param worldName   canonical world or dimension identifier (must not be {@code null} or blank)
 * @param chunkX      chunk X coordinate
 * @param chunkZ      chunk Z coordinate
 * @param loaded      {@code true} if the chunk is currently loaded in memory
 * @param forceLoaded {@code true} if the chunk is pinned by a force-load ticket
 * @param entityCount non-negative count of entities residing in the chunk (must be {@code >= 0})
 * @version 1.0.0
 */
public record VesselChunkStateSnapshot(
        String worldName,
        int chunkX,
        int chunkZ,
        boolean loaded,
        boolean forceLoaded,
        int entityCount) {

    /**
     * Validates all components of {@link VesselChunkStateSnapshot}.
     *
     * @throws NullPointerException     if {@code worldName} is {@code null}
     * @throws IllegalArgumentException if {@code worldName} is blank or {@code entityCount < 0}
     */
    public VesselChunkStateSnapshot {
        Objects.requireNonNull(worldName, "worldName must not be null");
        if (worldName.isBlank()) {
            throw new IllegalArgumentException("worldName must not be blank");
        }
        if (entityCount < 0) {
            throw new IllegalArgumentException("entityCount must not be negative");
        }
    }

    /**
     * Computes the 64-bit packed chunk coordinate key matching Vanilla {@code ChunkPos.asLong(int, int)}.
     *
     * @return packed 64-bit chunk key
     */
    public long packedChunkKey() {
        return ((long) this.chunkX & 0xFFFFFFFFL) | (((long) this.chunkZ & 0xFFFFFFFFL) << 32);
    }
}
