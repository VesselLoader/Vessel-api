package dev.vessel.api.network;

/**
 * Protocol states in which a network channel operates.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner or nested classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
public enum ChannelProtocolState {

    /**
     * Handshake phase prior to connection login.
     */
    HANDSHAKE("HANDSHAKE", true),

    /**
     * Configuration and negotiation phase.
     */
    CONFIGURATION("CONFIGURATION", true),

    /**
     * Active gameplay phase.
     */
    PLAY("PLAY", true),

    /**
     * Login authentication phase.
     */
    LOGIN("LOGIN", false);

    private final String stateName;
    private final boolean payloadAllowed;

    ChannelProtocolState(String stateName, boolean payloadAllowed) {
        this.stateName = stateName;
        this.payloadAllowed = payloadAllowed;
    }

    /**
     * Resolves the canonical state name.
     *
     * @return state identifier string
     */
    public String stateName() {
        return stateName;
    }

    /**
     * Indicates whether arbitrary custom payloads are permitted in this state.
     *
     * @return true if payloads are allowed
     */
    public boolean isPayloadAllowed() {
        return payloadAllowed;
    }
}
