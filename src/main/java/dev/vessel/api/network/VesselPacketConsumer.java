package dev.vessel.api.network;

/**
 * Functional message handler callback for processing incoming network payloads.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @param <T> payload type
 * @version 1.1.0
 * @since 1.0.0
 */
@FunctionalInterface
public interface VesselPacketConsumer<T extends VesselPacketPayload> {

    /**
     * Handles received payload within given context.
     *
     * @param payload received payload
     * @param context network execution context
     */
    void handle(T payload, VesselPacketContext context);
}
