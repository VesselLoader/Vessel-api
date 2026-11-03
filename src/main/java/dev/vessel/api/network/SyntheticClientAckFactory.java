package dev.vessel.api.network;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * Synthesizes instant acknowledgment responses for client handshake and configuration sequences.
 *
 * <p>Prevents modded client connection stall and timeout during multi-stage negotiation
 * across Forge, NeoForge, and Fabric protocol shims.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
public final class SyntheticClientAckFactory {

    public static final String FML_HANDSHAKE_CHANNEL = "fml:handshake";
    public static final String NEOFORGE_HANDSHAKE_CHANNEL = "neoforge:handshake";
    public static final String MINECRAFT_REGISTER_CHANNEL = "minecraft:register";
    public static final String VESSEL_HANDSHAKE_CHANNEL = "vessel:handshake";

    public static final byte ACK_SUCCESS_OPCODE = 0x01;
    public static final byte ACK_FAILURE_OPCODE = 0x00;
    public static final byte CONFIGURATION_COMPLETE_OPCODE = 0x02;

    /**
     * Constructs a synthetic ACK factory instance.
     */
    public SyntheticClientAckFactory() {
    }

    /**
     * Creates an initial handshake start acknowledgment payload.
     *
     * @param protocolVersion negotiated protocol version string
     * @return synthetic ACK payload
     */
    public SyntheticAckPayload createHandshakeStartAck(String protocolVersion) {
        Objects.requireNonNull(protocolVersion, "protocolVersion cannot be null");
        byte[] versionBytes = protocolVersion.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + versionBytes.length);
        buffer.put(ACK_SUCCESS_OPCODE);
        buffer.putInt(versionBytes.length);
        buffer.put(versionBytes);

        return new SyntheticAckPayload(
                FML_HANDSHAKE_CHANNEL,
                SyntheticAckType.HANDSHAKE_START,
                buffer.array(),
                System.nanoTime(),
                true
        );
    }

    /**
     * Creates a channel registration synchronization acknowledgment.
     *
     * @param channels list of negotiated channel identifiers
     * @return synthetic ACK payload
     */
    public SyntheticAckPayload createChannelRegistrationAck(List<String> channels) {
        Objects.requireNonNull(channels, "channels cannot be null");
        String concatenated = String.join("\0", channels);
        byte[] payloadBytes = concatenated.getBytes(StandardCharsets.UTF_8);

        return new SyntheticAckPayload(
                MINECRAFT_REGISTER_CHANNEL,
                SyntheticAckType.CHANNEL_REGISTRATION,
                payloadBytes,
                System.nanoTime(),
                true
        );
    }

    /**
     * Creates a mod list match acknowledgment payload confirming loader compatibility.
     *
     * @param clientModCount count of client-side mods matched
     * @return synthetic ACK payload
     */
    public SyntheticAckPayload createModListMatchAck(int clientModCount) {
        ByteBuffer buffer = ByteBuffer.allocate(5);
        buffer.put(ACK_SUCCESS_OPCODE);
        buffer.putInt(clientModCount);

        return new SyntheticAckPayload(
                NEOFORGE_HANDSHAKE_CHANNEL,
                SyntheticAckType.MOD_LIST_MATCH,
                buffer.array(),
                System.nanoTime(),
                true
        );
    }

    /**
     * Creates a configuration phase completion acknowledgment.
     *
     * @return synthetic ACK payload
     */
    public SyntheticAckPayload createConfigurationCompleteAck() {
        byte[] payload = new byte[]{CONFIGURATION_COMPLETE_OPCODE};
        return new SyntheticAckPayload(
                VESSEL_HANDSHAKE_CHANNEL,
                SyntheticAckType.CONFIGURATION_COMPLETE,
                payload,
                System.nanoTime(),
                true
        );
    }

    /**
     * Creates a login success synthetic ACK.
     *
     * @param playerUuid player unique identifier string
     * @param username   player display username
     * @return synthetic ACK payload
     */
    public SyntheticAckPayload createLoginSuccessAck(String playerUuid, String username) {
        Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
        Objects.requireNonNull(username, "username cannot be null");

        byte[] uuidBytes = playerUuid.getBytes(StandardCharsets.UTF_8);
        byte[] userBytes = username.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + uuidBytes.length + 4 + userBytes.length);
        buffer.put(ACK_SUCCESS_OPCODE);
        buffer.putInt(uuidBytes.length);
        buffer.put(uuidBytes);
        buffer.putInt(userBytes.length);
        buffer.put(userBytes);

        return new SyntheticAckPayload(
                VESSEL_HANDSHAKE_CHANNEL,
                SyntheticAckType.LOGIN_SUCCESS,
                buffer.array(),
                System.nanoTime(),
                true
        );
    }

    /**
     * Creates a generic custom ACK payload.
     *
     * @param channelName destination channel
     * @param type        ACK type
     * @param data        raw payload bytes
     * @param success     success indicator
     * @return synthetic ACK payload
     */
    public SyntheticAckPayload createGenericAck(String channelName, SyntheticAckType type, byte[] data, boolean success) {
        Objects.requireNonNull(channelName, "channelName cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(data, "data cannot be null");

        return new SyntheticAckPayload(
                channelName,
                type,
                data.clone(),
                System.nanoTime(),
                success
        );
    }
}
