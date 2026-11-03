package dev.vessel.api.network;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable transpiled Bedrock/Vanilla display packet payload.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner or nested classes.</p>
 *
 * @param targetEntityId     runtime target entity ID
 * @param packetType         canonical packet opcode or identifier
 * @param payload            serialized binary packet content
 * @param interpolationTicks tick count for client-side smoothing
 * @param isDirectRenderable whether the packet can be rendered directly by client
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
public record TranspiledBedrockPacket(
        int targetEntityId,
        String packetType,
        byte[] payload,
        int interpolationTicks,
        boolean isDirectRenderable
) {
    public TranspiledBedrockPacket {
        Objects.requireNonNull(packetType, "packetType cannot be null");
        Objects.requireNonNull(payload, "payload cannot be null");
    }

    /**
     * Resolves byte length of the payload.
     *
     * @return byte count
     */
    public int payloadLength() {
        return payload.length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TranspiledBedrockPacket that)) {
            return false;
        }
        return targetEntityId == that.targetEntityId
                && interpolationTicks == that.interpolationTicks
                && isDirectRenderable == that.isDirectRenderable
                && Objects.equals(packetType, that.packetType)
                && Arrays.equals(payload, that.payload);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(targetEntityId, packetType, interpolationTicks, isDirectRenderable);
        result = 31 * result + Arrays.hashCode(payload);
        return result;
    }

    @Override
    public String toString() {
        return "TranspiledBedrockPacket{" +
                "targetEntityId=" + targetEntityId +
                ", packetType='" + packetType + '\'' +
                ", payloadLength=" + payload.length +
                ", interpolationTicks=" + interpolationTicks +
                ", isDirectRenderable=" + isDirectRenderable +
                '}';
    }
}
