package dev.vessel.api.network;

/**
 * Types of synthetic handshake acknowledgement responses.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner or nested classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
public enum SyntheticAckType {

    /**
     * Initial handshake acknowledgment signaling server readiness.
     */
    HANDSHAKE_START("HANDSHAKE_START"),

    /**
     * Channel registration synchronization acknowledgment.
     */
    CHANNEL_REGISTRATION("CHANNEL_REGISTRATION"),

    /**
     * Mod list matching acknowledgment.
     */
    MOD_LIST_MATCH("MOD_LIST_MATCH"),

    /**
     * Configuration phase completion acknowledgment.
     */
    CONFIGURATION_COMPLETE("CONFIGURATION_COMPLETE"),

    /**
     * Final login success acknowledgment.
     */
    LOGIN_SUCCESS("LOGIN_SUCCESS"),

    /**
     * Generic custom acknowledgment response.
     */
    GENERIC_ACK("GENERIC_ACK");

    private final String typeName;

    SyntheticAckType(String typeName) {
        this.typeName = typeName;
    }

    /**
     * Resolves the type name.
     *
     * @return ACK type name
     */
    public String typeName() {
        return typeName;
    }
}
