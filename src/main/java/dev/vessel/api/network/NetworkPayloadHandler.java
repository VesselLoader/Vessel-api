package dev.vessel.api.network;

/**
 * Functional interface for receiving and processing inbound network payloads.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner or nested classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
@FunctionalInterface
public interface NetworkPayloadHandler {

    /**
     * Handles an incoming custom network payload.
     *
     * @param channel   associated network channel
     * @param sessionId connection session identifier
     * @param payload   raw byte data received
     */
    void handlePayload(VesselNetworkChannel channel, String sessionId, byte[] payload);
}
