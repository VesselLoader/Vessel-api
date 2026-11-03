package dev.vessel.api.network;

/**
 * Directional flow allowed for a network channel.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner or nested classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
public enum ChannelDirection {

    /**
     * Outgoing payloads sent from server to client.
     */
    CLIENTBOUND(true, false),

    /**
     * Incoming payloads sent from client to server.
     */
    SERVERBOUND(false, true),

    /**
     * Bidirectional flow between server and client.
     */
    BIDIRECTIONAL(true, true);

    private final boolean clientbound;
    private final boolean serverbound;

    ChannelDirection(boolean clientbound, boolean serverbound) {
        this.clientbound = clientbound;
        this.serverbound = serverbound;
    }

    /**
     * Checks if clientbound traffic is permitted.
     *
     * @return true if allowed to send to client
     */
    public boolean allowsClientbound() {
        return clientbound;
    }

    /**
     * Checks if serverbound traffic is permitted.
     *
     * @return true if allowed to receive from client
     */
    public boolean allowsServerbound() {
        return serverbound;
    }
}
