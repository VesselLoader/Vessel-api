package dev.vessel.api.network;

import java.util.Arrays;
import java.util.Objects;

/**
 * Result record of an optimized packet batch containing compressed or coalesced payload data.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner or nested classes.</p>
 *
 * @param channelName     destination channel identifier
 * @param mode            optimization strategy mode applied
 * @param packetCount     number of individual packets bundled
 * @param originalBytes   size of raw payload before optimization in bytes
 * @param compressedBytes size of resulting payload in bytes
 * @param payload         optimized binary byte payload
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
public record OptimizedPacketBatch(
        String channelName,
        BandwidthOptimizationMode mode,
        int packetCount,
        int originalBytes,
        int compressedBytes,
        byte[] payload
) {
    public OptimizedPacketBatch {
        Objects.requireNonNull(channelName, "channelName cannot be null");
        Objects.requireNonNull(mode, "mode cannot be null");
        Objects.requireNonNull(payload, "payload cannot be null");
    }

    /**
     * Calculates the compression ratio (compressed / original).
     *
     * @return compression ratio, or 1.0 if originalBytes is zero
     */
    public double compressionRatio() {
        if (originalBytes <= 0) {
            return 1.0;
        }
        return (double) compressedBytes / (double) originalBytes;
    }

    /**
     * Computes the total bytes saved by this optimization.
     *
     * @return non-negative bytes saved count
     */
    public int bytesSaved() {
        return Math.max(0, originalBytes - compressedBytes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OptimizedPacketBatch that)) {
            return false;
        }
        return packetCount == that.packetCount
                && originalBytes == that.originalBytes
                && compressedBytes == that.compressedBytes
                && Objects.equals(channelName, that.channelName)
                && mode == that.mode
                && Arrays.equals(payload, that.payload);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(channelName, mode, packetCount, originalBytes, compressedBytes);
        result = 31 * result + Arrays.hashCode(payload);
        return result;
    }

    @Override
    public String toString() {
        return "OptimizedPacketBatch{" +
                "channelName='" + channelName + '\'' +
                ", mode=" + mode +
                ", packetCount=" + packetCount +
                ", originalBytes=" + originalBytes +
                ", compressedBytes=" + compressedBytes +
                ", bytesSaved=" + bytesSaved() +
                '}';
    }
}
