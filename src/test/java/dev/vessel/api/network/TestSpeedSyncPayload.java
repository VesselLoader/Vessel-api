package dev.vessel.api.network;

/**
 * Top-level immutable test record representing a network payload.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 */
public record TestSpeedSyncPayload(int speed, String mode) implements VesselPacketPayload {
    public static final String ID = "test:speed_sync";

    @Override
    public String getId() {
        return ID;
    }
}
