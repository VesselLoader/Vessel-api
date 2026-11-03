package dev.vessel.api.network;

import java.util.Objects;

/**
 * Universal network channel representation for modded payloads across Forge, NeoForge, Fabric, and Bedrock.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
public final class VesselNetworkChannel {

    public static final int DEFAULT_MAX_PAYLOAD_SIZE = 1048576; // 1 MB default
    public static final int MIN_ALLOWED_PAYLOAD_SIZE = 1;
    public static final int ABSOLUTE_MAX_PAYLOAD_SIZE = 33554432; // 32 MB max
    public static final String DEFAULT_NAMESPACE = "vessel";

    private final String identifier;
    private final ChannelProtocolState state;
    private final ChannelDirection direction;
    private final int maxPayloadSizeBytes;
    private final boolean requiresModdedClient;

    /**
     * Constructs a channel with full parameters.
     *
     * @param identifier           scoped channel identifier (e.g., "create:contraption_motion")
     * @param state                protocol phase state
     * @param direction            traffic direction
     * @param maxPayloadSizeBytes  maximum permitted payload byte length
     * @param requiresModdedClient whether the channel requires a modded loader on client
     */
    public VesselNetworkChannel(
            String identifier,
            ChannelProtocolState state,
            ChannelDirection direction,
            int maxPayloadSizeBytes,
            boolean requiresModdedClient
    ) {
        this.identifier = Objects.requireNonNull(identifier, "identifier cannot be null");
        this.state = Objects.requireNonNull(state, "state cannot be null");
        this.direction = Objects.requireNonNull(direction, "direction cannot be null");
        if (maxPayloadSizeBytes < MIN_ALLOWED_PAYLOAD_SIZE || maxPayloadSizeBytes > ABSOLUTE_MAX_PAYLOAD_SIZE) {
            throw new IllegalArgumentException("maxPayloadSizeBytes out of range ["
                    + MIN_ALLOWED_PAYLOAD_SIZE + ", " + ABSOLUTE_MAX_PAYLOAD_SIZE + "]: " + maxPayloadSizeBytes);
        }
        this.maxPayloadSizeBytes = maxPayloadSizeBytes;
        this.requiresModdedClient = requiresModdedClient;
    }

    /**
     * Constructs a standard bidirectional play channel for modded payloads.
     *
     * @param identifier scoped channel identifier
     * @return constructed channel
     */
    public static VesselNetworkChannel ofModdedPlay(String identifier) {
        return new VesselNetworkChannel(
                identifier,
                ChannelProtocolState.PLAY,
                ChannelDirection.BIDIRECTIONAL,
                DEFAULT_MAX_PAYLOAD_SIZE,
                true
        );
    }

    /**
     * Constructs a standard bidirectional play channel for universal payloads (works on vanilla).
     *
     * @param identifier scoped channel identifier
     * @return constructed channel
     */
    public static VesselNetworkChannel ofUniversalPlay(String identifier) {
        return new VesselNetworkChannel(
                identifier,
                ChannelProtocolState.PLAY,
                ChannelDirection.BIDIRECTIONAL,
                DEFAULT_MAX_PAYLOAD_SIZE,
                false
        );
    }

    /**
     * Constructs a configuration or handshake phase channel.
     *
     * @param identifier scoped channel identifier
     * @param state      handshake or configuration state
     * @return constructed channel
     */
    public static VesselNetworkChannel ofHandshake(String identifier, ChannelProtocolState state) {
        return new VesselNetworkChannel(
                identifier,
                state,
                ChannelDirection.BIDIRECTIONAL,
                DEFAULT_MAX_PAYLOAD_SIZE,
                true
        );
    }

    /**
     * Validates if a proposed payload size is within permissible bounds.
     *
     * @param sizeInBytes size to validate
     * @return true if valid
     */
    public boolean isValidPayloadSize(int sizeInBytes) {
        return sizeInBytes >= 0 && sizeInBytes <= maxPayloadSizeBytes;
    }

    /**
     * Asserts that a payload size is valid, throwing an exception if exceeded.
     *
     * @param sizeInBytes size to assert
     */
    public void validatePayloadSize(int sizeInBytes) {
        if (!isValidPayloadSize(sizeInBytes)) {
            throw new IllegalArgumentException("Payload size " + sizeInBytes
                    + " exceeds maximum allowed " + maxPayloadSizeBytes + " for channel " + identifier);
        }
    }

    /**
     * Checks if this channel is compatible with a connected client type.
     *
     * @param clientType client classification
     * @return true if compatible
     */
    public boolean isCompatibleWith(NetworkClientType clientType) {
        Objects.requireNonNull(clientType, "clientType cannot be null");
        if (!requiresModdedClient) {
            return true;
        }
        return clientType.isModded();
    }

    /**
     * Resolves the scoped identifier.
     *
     * @return identifier string
     */
    public String identifier() {
        return identifier;
    }

    /**
     * Resolves protocol state.
     *
     * @return protocol state
     */
    public ChannelProtocolState state() {
        return state;
    }

    /**
     * Resolves allowed directional flow.
     *
     * @return direction
     */
    public ChannelDirection direction() {
        return direction;
    }

    /**
     * Resolves max payload size in bytes.
     *
     * @return max payload bytes
     */
    public int maxPayloadSizeBytes() {
        return maxPayloadSizeBytes;
    }

    /**
     * Indicates whether a modded client is required.
     *
     * @return true if modded client required
     */
    public boolean requiresModdedClient() {
        return requiresModdedClient;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VesselNetworkChannel that)) {
            return false;
        }
        return maxPayloadSizeBytes == that.maxPayloadSizeBytes
                && requiresModdedClient == that.requiresModdedClient
                && Objects.equals(identifier, that.identifier)
                && state == that.state
                && direction == that.direction;
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, state, direction, maxPayloadSizeBytes, requiresModdedClient);
    }

    @Override
    public String toString() {
        return "VesselNetworkChannel{" +
                "identifier='" + identifier + '\'' +
                ", state=" + state +
                ", direction=" + direction +
                ", maxPayloadSizeBytes=" + maxPayloadSizeBytes +
                ", requiresModdedClient=" + requiresModdedClient +
                '}';
    }
}
