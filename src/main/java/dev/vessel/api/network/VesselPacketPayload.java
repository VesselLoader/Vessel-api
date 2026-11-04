package dev.vessel.api.network;

/**
 * Marker and identification contract for type-safe network payloads in the VESSEL Super-API ecosystem.
 *
 * <p>Inspired by NeoForge's {@code CustomPacketPayload} and Fabric's {@code CustomPayload},
 * designed to be implemented directly by Java {@code record}s for clean, zero-boilerplate networking.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.1.0
 * @since 1.0.0
 */
public interface VesselPacketPayload {

    /**
     * Resolves the canonical namespaced packet identifier (e.g. {@code "mymod:speed_sync"}).
     *
     * @return namespaced packet id
     */
    String getId();
}
