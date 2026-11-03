package dev.vessel.api.network;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable synthetic acknowledgment payload generated during handshake negotiation.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner or nested classes.</p>
 *
 * @param channelName    network channel destination
 * @param ackType        category of acknowledgment
 * @param payload        serialized byte payload
 * @param timestampNanos timestamp of creation in nanoseconds
 * @param success        indicates handshake success status
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
public record SyntheticAckPayload(
        String channelName,
        SyntheticAckType ackType,
        byte[] payload,
        long timestampNanos,
        boolean success
) {
    public SyntheticAckPayload {
        Objects.requireNonNull(channelName, "channelName cannot be null");
        Objects.requireNonNull(ackType, "ackType cannot be null");
        Objects.requireNonNull(payload, "payload cannot be null");
    }

    /**
     * Resolves payload byte length.
     *
     * @return payload length in bytes
     */
    public int payloadLength() {
        return payload.length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SyntheticAckPayload that)) {
            return false;
        }
        return timestampNanos == that.timestampNanos
                && success == that.success
                && Objects.equals(channelName, that.channelName)
                && ackType == that.ackType
                && Arrays.equals(payload, that.payload);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(channelName, ackType, timestampNanos, success);
        result = 31 * result + Arrays.hashCode(payload);
        return result;
    }

    @Override
    public String toString() {
        return "SyntheticAckPayload{" +
                "channelName='" + channelName + '\'' +
                ", ackType=" + ackType +
                ", payloadLength=" + payload.length +
                ", success=" + success +
                '}';
    }
}
