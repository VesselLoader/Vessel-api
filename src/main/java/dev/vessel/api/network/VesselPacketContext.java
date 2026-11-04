package dev.vessel.api.network;

/**
 * Execution context provided when a {@link VesselPacketPayload} is received.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @param player       sender or target player reference (null if server console)
 * @param channelId    channel identifier through which packet was transported
 * @param isServerSide true if processing on dedicated server or server strand
 * @version 1.1.0
 * @since 1.0.0
 */
public record VesselPacketContext(
        Object player,
        String channelId,
        boolean isServerSide
) {
}
