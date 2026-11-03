package dev.vessel.api.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Optimizes network packet bandwidth through coalescing, delta compression, and threshold Deflate compression.
 *
 * <p>Implements Chapter 3.5 and 3.6 of the VESSEL Super-API specification: merges high-frequency physics/motion
 * updates (such as Create contraption delta motion across 128 ticks) into single atomic payloads.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
public final class PacketBandwidthOptimizer {

    public static final int DEFAULT_COMPRESSION_THRESHOLD_BYTES = 256;
    public static final int BUFFER_RECYCLE_SIZE = 1024;
    public static final byte OP_DELTA_MATCH = 0x00;
    public static final byte OP_DELTA_MUTATE = 0x01;

    private final int compressionThresholdBytes;

    /**
     * Constructs a bandwidth optimizer with default compression threshold (256 bytes).
     */
    public PacketBandwidthOptimizer() {
        this(DEFAULT_COMPRESSION_THRESHOLD_BYTES);
    }

    /**
     * Constructs a bandwidth optimizer with custom compression threshold.
     *
     * @param compressionThresholdBytes minimum payload byte length to trigger Deflate compression
     */
    public PacketBandwidthOptimizer(int compressionThresholdBytes) {
        if (compressionThresholdBytes < 0) {
            throw new IllegalArgumentException("compressionThresholdBytes cannot be negative: " + compressionThresholdBytes);
        }
        this.compressionThresholdBytes = compressionThresholdBytes;
    }

    /**
     * Coalesces multiple micro-packets destined for the same channel into an atomic batch.
     *
     * @param channelName destination channel
     * @param packets     list of raw packet payloads to merge
     * @return optimized packet batch
     */
    public OptimizedPacketBatch coalescePackets(String channelName, List<byte[]> packets) {
        Objects.requireNonNull(channelName, "channelName cannot be null");
        Objects.requireNonNull(packets, "packets cannot be null");

        if (packets.isEmpty()) {
            return new OptimizedPacketBatch(channelName, BandwidthOptimizationMode.PASSTHROUGH, 0, 0, 0, new byte[0]);
        }

        int totalOriginalBytes = 0;
        for (byte[] packet : packets) {
            totalOriginalBytes += packet.length;
        }

        // Format: PacketCount(4) + [Length(4) + Data(bytes)]*
        int coalescedHeaderSize = 4 + (packets.size() * 4);
        ByteBuffer buffer = ByteBuffer.allocate(coalescedHeaderSize + totalOriginalBytes);
        buffer.putInt(packets.size());

        for (byte[] packet : packets) {
            buffer.putInt(packet.length);
            buffer.put(packet);
        }

        byte[] coalescedData = buffer.array();

        if (coalescedData.length >= compressionThresholdBytes) {
            byte[] compressed = compressBytes(coalescedData);
            return new OptimizedPacketBatch(
                    channelName,
                    BandwidthOptimizationMode.FULL_COMPRESS,
                    packets.size(),
                    totalOriginalBytes,
                    compressed.length,
                    compressed
            );
        }

        return new OptimizedPacketBatch(
                channelName,
                BandwidthOptimizationMode.COALESCE_BATCH,
                packets.size(),
                totalOriginalBytes,
                coalescedData.length,
                coalescedData
        );
    }

    /**
     * Computes a delta compression diff between a base state and a new state payload.
     *
     * @param channelName destination channel
     * @param baseState   previous state payload
     * @param newState    new state payload
     * @return optimized delta batch
     */
    public OptimizedPacketBatch deltaCompress(String channelName, byte[] baseState, byte[] newState) {
        Objects.requireNonNull(channelName, "channelName cannot be null");
        Objects.requireNonNull(baseState, "baseState cannot be null");
        Objects.requireNonNull(newState, "newState cannot be null");

        int minLen = Math.min(baseState.length, newState.length);
        ByteArrayOutputStream deltaOut = new ByteArrayOutputStream(newState.length);

        // Header: new state total length
        deltaOut.write((newState.length >> 24) & 0xFF);
        deltaOut.write((newState.length >> 16) & 0xFF);
        deltaOut.write((newState.length >> 8) & 0xFF);
        deltaOut.write(newState.length & 0xFF);

        int i = 0;
        while (i < minLen) {
            if (baseState[i] == newState[i]) {
                // Match run
                int matchStart = i;
                while (i < minLen && baseState[i] == newState[i] && (i - matchStart) < 255) {
                    i++;
                }
                int matchLen = i - matchStart;
                deltaOut.write(OP_DELTA_MATCH);
                deltaOut.write(matchLen);
            } else {
                // Mutate run
                int diffStart = i;
                while (i < minLen && baseState[i] != newState[i] && (i - diffStart) < 255) {
                    i++;
                }
                int diffLen = i - diffStart;
                deltaOut.write(OP_DELTA_MUTATE);
                deltaOut.write(diffLen);
                deltaOut.write(newState, diffStart, diffLen);
            }
        }

        // Tail bytes if newState is longer than baseState
        if (i < newState.length) {
            int tailLen = newState.length - i;
            deltaOut.write(OP_DELTA_MUTATE);
            deltaOut.write(tailLen & 0xFF);
            deltaOut.write(newState, i, tailLen);
        }

        byte[] deltaBytes = deltaOut.toByteArray();
        return new OptimizedPacketBatch(
                channelName,
                BandwidthOptimizationMode.DELTA_COMPRESS,
                1,
                newState.length,
                deltaBytes.length,
                deltaBytes
        );
    }

    /**
     * Reconstructs a full new state payload from a base state and a delta payload.
     *
     * @param baseState    reference base state
     * @param deltaPayload delta compressed payload
     * @return reconstructed full state payload
     */
    public byte[] deltaDecompress(byte[] baseState, byte[] deltaPayload) {
        Objects.requireNonNull(baseState, "baseState cannot be null");
        Objects.requireNonNull(deltaPayload, "deltaPayload cannot be null");

        ByteBuffer buf = ByteBuffer.wrap(deltaPayload);
        int expectedLength = buf.getInt();
        byte[] result = new byte[expectedLength];

        int destPos = 0;
        int basePos = 0;

        while (buf.hasRemaining() && destPos < expectedLength) {
            byte op = buf.get();
            int len = buf.get() & 0xFF;

            if (op == OP_DELTA_MATCH) {
                System.arraycopy(baseState, basePos, result, destPos, len);
                basePos += len;
                destPos += len;
            } else if (op == OP_DELTA_MUTATE) {
                buf.get(result, destPos, len);
                basePos += len;
                destPos += len;
            } else {
                throw new IllegalArgumentException("Unknown delta opcode: " + op);
            }
        }

        return result;
    }

    /**
     * Compresses raw payload bytes using Deflater if threshold is met.
     *
     * @param channelName destination channel
     * @param payload     raw byte array
     * @return optimized packet batch
     */
    public OptimizedPacketBatch compressPayload(String channelName, byte[] payload) {
        Objects.requireNonNull(channelName, "channelName cannot be null");
        Objects.requireNonNull(payload, "payload cannot be null");

        if (payload.length < compressionThresholdBytes) {
            return new OptimizedPacketBatch(
                    channelName,
                    BandwidthOptimizationMode.PASSTHROUGH,
                    1,
                    payload.length,
                    payload.length,
                    payload.clone()
            );
        }

        byte[] compressed = compressBytes(payload);
        return new OptimizedPacketBatch(
                channelName,
                BandwidthOptimizationMode.FULL_COMPRESS,
                1,
                payload.length,
                compressed.length,
                compressed
        );
    }

    /**
     * Decompresses an optimized Deflate byte payload.
     *
     * @param compressedPayload compressed bytes
     * @return decompressed raw bytes
     */
    public byte[] decompressPayload(byte[] compressedPayload) {
        Objects.requireNonNull(compressedPayload, "compressedPayload cannot be null");

        Inflater inflater = new Inflater();
        try {
            inflater.setInput(compressedPayload);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(compressedPayload.length * 2);
            byte[] buffer = new byte[BUFFER_RECYCLE_SIZE];

            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                if (count == 0 && inflater.needsInput()) {
                    break;
                }
                outputStream.write(buffer, 0, count);
            }

            return outputStream.toByteArray();
        } catch (DataFormatException e) {
            throw new IllegalArgumentException("Corrupt compressed payload format", e);
        } finally {
            inflater.end();
        }
    }

    private byte[] compressBytes(byte[] raw) {
        Deflater deflater = new Deflater(Deflater.BEST_SPEED);
        try {
            deflater.setInput(raw);
            deflater.finish();
            ByteArrayOutputStream out = new ByteArrayOutputStream(raw.length);
            byte[] buffer = new byte[BUFFER_RECYCLE_SIZE];

            while (!deflater.finished()) {
                int count = deflater.deflate(buffer);
                out.write(buffer, 0, count);
            }

            return out.toByteArray();
        } finally {
            deflater.end();
        }
    }

    /**
     * Resolves the configured compression threshold.
     *
     * @return threshold in bytes
     */
    public int compressionThresholdBytes() {
        return compressionThresholdBytes;
    }
}
