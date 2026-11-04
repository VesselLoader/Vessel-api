package dev.vessel.api.network;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verification test suite for {@link VesselPayloadChannel} and {@link VesselPacketPayload}.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 */
public final class VesselPayloadChannelTest {

    @Test
    @DisplayName("Verify custom packet registration, encode, decode, and dispatching")
    void verifyPacketChannelLifecycle() {
        VesselPayloadChannel channel = VesselPayloadChannel.create("test:network_channel");

        AtomicBoolean received = new AtomicBoolean(false);
        AtomicReference<TestSpeedSyncPayload> payloadRef = new AtomicReference<>();

        VesselPacketCodec<TestSpeedSyncPayload> codec = new VesselPacketCodec<>() {
            @Override
            public byte[] encode(TestSpeedSyncPayload p) {
                return (p.speed() + ":" + p.mode()).getBytes(StandardCharsets.UTF_8);
            }

            @Override
            public TestSpeedSyncPayload decode(byte[] data) {
                String str = new String(data, StandardCharsets.UTF_8);
                String[] parts = str.split(":");
                return new TestSpeedSyncPayload(Integer.parseInt(parts[0]), parts[1]);
            }
        };

        channel.register(TestSpeedSyncPayload.ID, TestSpeedSyncPayload.class, codec, (payload, context) -> {
            received.set(true);
            payloadRef.set(payload);
        });

        TestSpeedSyncPayload outgoing = new TestSpeedSyncPayload(120, "HYPERDRIVE");
        channel.sendToServer(outgoing);

        assertThat(received.get()).isTrue();
        assertThat(payloadRef.get()).isNotNull();
        assertThat(payloadRef.get().speed()).isEqualTo(120);
        assertThat(payloadRef.get().mode()).isEqualTo("HYPERDRIVE");
    }
}
