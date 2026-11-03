package dev.vessel.api.config;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable binary configuration synchronization frame transmitted across Netty during connection handshake.
 *
 * <p>Enables zero-desync server-to-client configuration replication across heterogeneous mod loaders.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner or nested classes.</p>
 *
 * @param modId                originating mod identifier
 * @param configName           configuration file name
 * @param format               serialization format type
 * @param payloadBytes         raw configuration byte data
 * @param timestampEpochMillis creation timestamp in milliseconds
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
public record ConfigurationSyncPayload(
        String modId,
        String configName,
        ConfigFormatType format,
        byte[] payloadBytes,
        long timestampEpochMillis
) {
    public static final int MAGIC_HEADER = 0x56434647; // 'VCFG'
    public static final byte PROTOCOL_VERSION = 0x01;

    public ConfigurationSyncPayload {
        Objects.requireNonNull(modId, "modId cannot be null");
        Objects.requireNonNull(configName, "configName cannot be null");
        Objects.requireNonNull(format, "format cannot be null");
        Objects.requireNonNull(payloadBytes, "payloadBytes cannot be null");
    }

    /**
     * Resolves the size of the payload in bytes.
     *
     * @return byte length
     */
    public int payloadSize() {
        return payloadBytes.length;
    }

    /**
     * Serializes this payload frame into a self-describing binary byte array.
     *
     * @return serialized binary array
     */
    public byte[] serialize() {
        byte[] modIdBytes = modId.getBytes(StandardCharsets.UTF_8);
        byte[] configNameBytes = configName.getBytes(StandardCharsets.UTF_8);

        // Header: Magic(4) + Version(1) + FormatOrdinal(1) + Timestamp(8) + ModIdLen(2) + ModId + NameLen(2) + Name + DataLen(4) + Data
        int totalSize = 4 + 1 + 1 + 8 + 2 + modIdBytes.length + 2 + configNameBytes.length + 4 + payloadBytes.length;
        ByteBuffer buffer = ByteBuffer.allocate(totalSize);

        buffer.putInt(MAGIC_HEADER);
        buffer.put(PROTOCOL_VERSION);
        buffer.put((byte) format.ordinal());
        buffer.putLong(timestampEpochMillis);

        buffer.putShort((short) modIdBytes.length);
        buffer.put(modIdBytes);

        buffer.putShort((short) configNameBytes.length);
        buffer.put(configNameBytes);

        buffer.putInt(payloadBytes.length);
        buffer.put(payloadBytes);

        return buffer.array();
    }

    /**
     * Deserializes a binary byte array back into a ConfigurationSyncPayload instance.
     *
     * @param data binary frame bytes
     * @return deserialized payload instance
     * @throws IllegalArgumentException if the buffer is malformed or invalid
     */
    public static ConfigurationSyncPayload deserialize(byte[] data) {
        Objects.requireNonNull(data, "data cannot be null");
        if (data.length < 22) { // Minimum possible header size
            throw new IllegalArgumentException("Payload byte array too short for header: " + data.length);
        }

        ByteBuffer buffer = ByteBuffer.wrap(data);
        int magic = buffer.getInt();
        if (magic != MAGIC_HEADER) {
            throw new IllegalArgumentException("Invalid magic header: " + Integer.toHexString(magic));
        }

        byte version = buffer.get();
        if (version != PROTOCOL_VERSION) {
            throw new IllegalArgumentException("Unsupported protocol version: " + version);
        }

        int formatOrd = buffer.get() & 0xFF;
        ConfigFormatType[] formats = ConfigFormatType.values();
        if (formatOrd >= formats.length) {
            throw new IllegalArgumentException("Invalid format ordinal: " + formatOrd);
        }
        ConfigFormatType format = formats[formatOrd];

        long timestamp = buffer.getLong();

        short modIdLen = buffer.getShort();
        byte[] modIdBytes = new byte[modIdLen];
        buffer.get(modIdBytes);
        String modId = new String(modIdBytes, StandardCharsets.UTF_8);

        short nameLen = buffer.getShort();
        byte[] nameBytes = new byte[nameLen];
        buffer.get(nameBytes);
        String configName = new String(nameBytes, StandardCharsets.UTF_8);

        int payloadLen = buffer.getInt();
        byte[] payload = new byte[payloadLen];
        buffer.get(payload);

        return new ConfigurationSyncPayload(modId, configName, format, payload, timestamp);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConfigurationSyncPayload that)) {
            return false;
        }
        return timestampEpochMillis == that.timestampEpochMillis
                && Objects.equals(modId, that.modId)
                && Objects.equals(configName, that.configName)
                && format == that.format
                && Arrays.equals(payloadBytes, that.payloadBytes);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(modId, configName, format, timestampEpochMillis);
        result = 31 * result + Arrays.hashCode(payloadBytes);
        return result;
    }

    @Override
    public String toString() {
        return "ConfigurationSyncPayload{" +
                "modId='" + modId + '\'' +
                ", configName='" + configName + '\'' +
                ", format=" + format +
                ", payloadSize=" + payloadBytes.length +
                ", timestampEpochMillis=" + timestampEpochMillis +
                '}';
    }
}
