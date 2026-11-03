// ==============================================================================
// VESSEL-API — VesselCustomPayloadDescriptor.java
// Version: 1.0.0
// Changelog:
// - 1.0.0: Initial creation of immutable VesselCustomPayloadDescriptor record
//          describing cross-loader custom payload network channels in VUSA.
// ==============================================================================

package dev.vessel.api.network;

import java.util.Objects;

/**
 * Immutable descriptor of a custom payload network channel bridged across
 * Vanilla {@code CustomPacketPayload}, NeoForge {@code PayloadRegistrar},
 * Fabric Networking API, and Bukkit/Paper plugin messaging channels.
 *
 * @param channelKey      namespaced channel identifier (must not be {@code null} or blank)
 * @param direction       network bound direction such as {@code PLAY_C2S}, {@code PLAY_S2C},
 *                        {@code SERVERBOUND}, or {@code CLIENTBOUND} (must not be {@code null} or blank)
 * @param sourceLoader    originating loader identifier (must not be {@code null} or blank)
 * @param maxPayloadBytes maximum allowed payload size in bytes (must be {@code > 0})
 * @version 1.0.0
 */
public record VesselCustomPayloadDescriptor(
        String channelKey,
        String direction,
        String sourceLoader,
        int maxPayloadBytes) {

    /**
     * Validates all components of {@link VesselCustomPayloadDescriptor}.
     *
     * @throws NullPointerException     if any string component is {@code null}
     * @throws IllegalArgumentException if any string component is blank or {@code maxPayloadBytes <= 0}
     */
    public VesselCustomPayloadDescriptor {
        Objects.requireNonNull(channelKey, "channelKey must not be null");
        Objects.requireNonNull(direction, "direction must not be null");
        Objects.requireNonNull(sourceLoader, "sourceLoader must not be null");
        if (channelKey.isBlank()) {
            throw new IllegalArgumentException("channelKey must not be blank");
        }
        if (direction.isBlank()) {
            throw new IllegalArgumentException("direction must not be blank");
        }
        if (sourceLoader.isBlank()) {
            throw new IllegalArgumentException("sourceLoader must not be blank");
        }
        if (maxPayloadBytes <= 0) {
            throw new IllegalArgumentException("maxPayloadBytes must be positive");
        }
    }

    /**
     * Checks whether this payload channel is serverbound (client-to-server).
     *
     * @return {@code true} if {@code direction} equals {@code PLAY_C2S} or {@code SERVERBOUND} (case-insensitive)
     */
    public boolean isServerbound() {
        return "PLAY_C2S".equalsIgnoreCase(this.direction) || "SERVERBOUND".equalsIgnoreCase(this.direction);
    }

    /**
     * Checks whether this payload channel is clientbound (server-to-client).
     *
     * @return {@code true} if {@code direction} equals {@code PLAY_S2C} or {@code CLIENTBOUND} (case-insensitive)
     */
    public boolean isClientbound() {
        return "PLAY_S2C".equalsIgnoreCase(this.direction) || "CLIENTBOUND".equalsIgnoreCase(this.direction);
    }
}
