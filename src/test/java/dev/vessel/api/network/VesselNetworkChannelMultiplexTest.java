package dev.vessel.api.network;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Enterprise test suite verifying Chapter 3.5 & 6 Universal Network Multiplexing
 * and custom payload bypass routing across Forge, NeoForge, Fabric, Bedrock, and Vanilla.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner or nested classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
public class VesselNetworkChannelMultiplexTest {

    @Test
    @DisplayName("VesselNetworkChannel: Validates channel initialization, states, boundaries and client compatibility")
    void testVesselNetworkChannelConfigurationAndValidation() {
        VesselNetworkChannel modChannel = VesselNetworkChannel.ofModdedPlay("create:contraption_motion");
        assertThat(modChannel.identifier()).isEqualTo("create:contraption_motion");
        assertThat(modChannel.state()).isEqualTo(ChannelProtocolState.PLAY);
        assertThat(modChannel.direction()).isEqualTo(ChannelDirection.BIDIRECTIONAL);
        assertThat(modChannel.maxPayloadSizeBytes()).isEqualTo(VesselNetworkChannel.DEFAULT_MAX_PAYLOAD_SIZE);
        assertThat(modChannel.requiresModdedClient()).isTrue();

        // Compatibility
        assertThat(modChannel.isCompatibleWith(NetworkClientType.MODDED_FORGE)).isTrue();
        assertThat(modChannel.isCompatibleWith(NetworkClientType.MODDED_NEOFORGE)).isTrue();
        assertThat(modChannel.isCompatibleWith(NetworkClientType.MODDED_FABRIC)).isTrue();
        assertThat(modChannel.isCompatibleWith(NetworkClientType.VANILLA)).isFalse();
        assertThat(modChannel.isCompatibleWith(NetworkClientType.BEDROCK)).isFalse();

        // Universal channel
        VesselNetworkChannel universalChannel = VesselNetworkChannel.ofUniversalPlay("minecraft:brand");
        assertThat(universalChannel.requiresModdedClient()).isFalse();
        assertThat(universalChannel.isCompatibleWith(NetworkClientType.VANILLA)).isTrue();
        assertThat(universalChannel.isCompatibleWith(NetworkClientType.BEDROCK)).isTrue();

        // Payload validation
        modChannel.validatePayloadSize(1024);
        assertThatThrownBy(() -> modChannel.validatePayloadSize(VesselNetworkChannel.DEFAULT_MAX_PAYLOAD_SIZE + 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("ChannelPipelineMultiplexer: Verifies channel registration, subscriptions, and fast-path payload routing")
    void testChannelPipelineMultiplexingAndPayloadRouting() {
        ChannelPipelineMultiplexer multiplexer = new ChannelPipelineMultiplexer();

        VesselNetworkChannel channel1 = VesselNetworkChannel.ofModdedPlay("vessel:sync");
        VesselNetworkChannel channel2 = VesselNetworkChannel.ofModdedPlay("create:contraption");

        multiplexer.registerChannel(channel1);
        multiplexer.registerChannel(channel2);

        assertThat(multiplexer.totalRegisteredChannels()).isEqualTo(2);
        assertThat(multiplexer.hasChannel("vessel:sync")).isTrue();
        assertThat(multiplexer.getChannel("vessel:sync")).isPresent();

        String sessionId = "player-session-101";
        multiplexer.subscribeSession(sessionId, "vessel:sync");

        assertThat(multiplexer.isSessionSubscribed(sessionId, "vessel:sync")).isTrue();
        assertThat(multiplexer.isSessionSubscribed(sessionId, "create:contraption")).isFalse();
        assertThat(multiplexer.canRouteClientbound("vessel:sync", sessionId)).isTrue();
        assertThat(multiplexer.canRouteClientbound("create:contraption", sessionId)).isFalse();

        // Payload handler registration and dispatch
        AtomicBoolean handlerInvoked = new AtomicBoolean(false);
        AtomicReference<String> receivedSession = new AtomicReference<>();
        AtomicReference<byte[]> receivedPayload = new AtomicReference<>();

        multiplexer.registerHandler("vessel:sync", (chan, sess, payload) -> {
            handlerInvoked.set(true);
            receivedSession.set(sess);
            receivedPayload.set(payload);
        });

        byte[] testData = "payload-binary-sync-matrix".getBytes(StandardCharsets.UTF_8);
        int dispatched = multiplexer.dispatchIncoming("vessel:sync", sessionId, testData);

        assertThat(dispatched).isEqualTo(1);
        assertThat(handlerInvoked.get()).isTrue();
        assertThat(receivedSession.get()).isEqualTo(sessionId);
        assertThat(receivedPayload.get()).isEqualTo(testData);

        // Unsubscribe and session removal
        multiplexer.unsubscribeSession(sessionId, "vessel:sync");
        assertThat(multiplexer.isSessionSubscribed(sessionId, "vessel:sync")).isFalse();

        multiplexer.removeSession(sessionId);
        assertThat(multiplexer.totalTrackedSessions()).isEqualTo(0);
    }

    @Test
    @DisplayName("CustomPayloadBypassFilter: Silently absorbs incompatible payloads avoiding kicks on Vanilla & Bedrock")
    void testCustomPayloadBypassFilterAbsorptionAndBypass() {
        CustomPayloadBypassFilter filter = new CustomPayloadBypassFilter();

        String modChannel = "botania:mana_sync";
        String animChannel = "geckolib:animation";
        String vanillaBrand = "minecraft:brand";

        filter.registerTranspilableAnimationChannel(animChannel);

        // Modded client receives modded packets directly
        BypassFilterResult moddedRes = filter.evaluate(modChannel, NetworkClientType.MODDED_NEOFORGE, 128);
        assertThat(moddedRes).isEqualTo(BypassFilterResult.FORWARD);
        assertThat(moddedRes.shouldForward()).isTrue();

        // Vanilla client receiving unrecognized modded channel -> silently absorbed
        BypassFilterResult vanillaRes = filter.evaluate(modChannel, NetworkClientType.VANILLA, 256);
        assertThat(vanillaRes).isEqualTo(BypassFilterResult.ABSORB_SILENT);
        assertThat(vanillaRes.shouldForward()).isFalse();

        // Bedrock client receiving animation channel -> transpile to Bedrock display entities
        BypassFilterResult bedrockRes = filter.evaluate(animChannel, NetworkClientType.BEDROCK, 512);
        assertThat(bedrockRes).isEqualTo(BypassFilterResult.TRANSPILE_BEDROCK);
        assertThat(bedrockRes.requiresTranspilation()).isTrue();

        // Vanilla channel forward
        BypassFilterResult brandRes = filter.evaluate(vanillaBrand, NetworkClientType.VANILLA, 32);
        assertThat(brandRes).isEqualTo(BypassFilterResult.FORWARD);

        // Verify metrics
        assertThat(filter.forwardedPacketCount()).isEqualTo(2); // moddedRes + brandRes
        assertThat(filter.absorbedPacketCount()).isEqualTo(1); // vanillaRes
        assertThat(filter.transpiledPacketCount()).isEqualTo(1); // bedrockRes
        assertThat(filter.absorbedBytesCount()).isEqualTo(256);

        filter.resetMetrics();
        assertThat(filter.absorbedPacketCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("SyntheticClientAckFactory: Generates zero-stall instant handshake and negotiation ACKs")
    void testSyntheticClientAckFactoryNegotiations() {
        SyntheticClientAckFactory factory = new SyntheticClientAckFactory();

        SyntheticAckPayload handshakeStart = factory.createHandshakeStartAck("1.21.4");
        assertThat(handshakeStart.ackType()).isEqualTo(SyntheticAckType.HANDSHAKE_START);
        assertThat(handshakeStart.success()).isTrue();
        assertThat(handshakeStart.payloadLength()).isGreaterThan(0);

        List<String> channels = List.of("vessel:sync", "create:contraption_motion");
        SyntheticAckPayload regAck = factory.createChannelRegistrationAck(channels);
        assertThat(regAck.ackType()).isEqualTo(SyntheticAckType.CHANNEL_REGISTRATION);
        assertThat(regAck.success()).isTrue();
        String decodedChannels = new String(regAck.payload(), StandardCharsets.UTF_8);
        assertThat(decodedChannels).contains("vessel:sync");

        SyntheticAckPayload modMatch = factory.createModListMatchAck(35);
        assertThat(modMatch.ackType()).isEqualTo(SyntheticAckType.MOD_LIST_MATCH);
        assertThat(modMatch.success()).isTrue();

        SyntheticAckPayload configComplete = factory.createConfigurationCompleteAck();
        assertThat(configComplete.ackType()).isEqualTo(SyntheticAckType.CONFIGURATION_COMPLETE);
        assertThat(configComplete.success()).isTrue();

        SyntheticAckPayload loginSuccess = factory.createLoginSuccessAck("00000000-0000-0000-0000-000000000001", "Shiroz");
        assertThat(loginSuccess.ackType()).isEqualTo(SyntheticAckType.LOGIN_SUCCESS);
        assertThat(loginSuccess.success()).isTrue();
    }

    @Test
    @DisplayName("BedrockDisplayEntityTranspiler: Transpiles 3D transforms & animation keyframes to Bedrock packets")
    void testBedrockDisplayEntityTranspilation() {
        BedrockDisplayEntityTranspiler transpiler = new BedrockDisplayEntityTranspiler();

        DisplayEntityKeyframe keyframe1 = new DisplayEntityKeyframe(
                5001,
                100.5, 65.0, -200.25,
                45.0f, 15.0f, 0.0f,
                1.5f, 1.5f, 1.5f,
                2,
                System.currentTimeMillis()
        );

        TranspiledBedrockPacket packet1 = transpiler.transpileKeyframe(keyframe1);
        assertThat(packet1.targetEntityId()).isEqualTo(5001);
        assertThat(packet1.packetType()).isEqualTo(BedrockDisplayEntityTranspiler.PACKET_TYPE_BEDROCK_DISPLAY);
        assertThat(packet1.isDirectRenderable()).isTrue();
        assertThat(packet1.payload()[0]).isEqualTo(BedrockDisplayEntityTranspiler.TRANSPILE_MAGIC);

        DisplayEntityKeyframe keyframe2 = new DisplayEntityKeyframe(
                5001,
                102.0, 65.5, -199.0,
                50.0f, 10.0f, 0.0f,
                1.5f, 1.5f, 1.5f,
                2,
                System.currentTimeMillis()
        );

        TranspiledBedrockPacket deltaPacket = transpiler.transpileKeyframeDelta(keyframe1, keyframe2);
        assertThat(deltaPacket.packetType()).isEqualTo(BedrockDisplayEntityTranspiler.PACKET_TYPE_BEDROCK_MOTION_DELTA);
        assertThat(deltaPacket.payloadLength()).isGreaterThan(0);

        List<TranspiledBedrockPacket> batch = transpiler.transpileAnimationBatch(List.of(keyframe1, keyframe2));
        assertThat(batch).hasSize(2);

        byte[] rawBytes = new byte[32];
        Arrays.fill(rawBytes, (byte) 0x7F);
        Optional<TranspiledBedrockPacket> rawTranspiled = transpiler.transpileRawModPayload(777, rawBytes);
        assertThat(rawTranspiled).isPresent();
    }

    @Test
    @DisplayName("PacketBandwidthOptimizer: Coalesces micro-packets and verifies lossless delta and Deflate compression")
    void testPacketBandwidthOptimizationAndDeltaCompression() {
        PacketBandwidthOptimizer optimizer = new PacketBandwidthOptimizer(128);

        // 1. Packet coalescing
        List<byte[]> microPackets = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            microPackets.add(("micro-packet-data-" + i).getBytes(StandardCharsets.UTF_8));
        }

        OptimizedPacketBatch batch = optimizer.coalescePackets("create:motion_batch", microPackets);
        assertThat(batch.packetCount()).isEqualTo(10);
        assertThat(batch.mode()).isIn(BandwidthOptimizationMode.COALESCE_BATCH, BandwidthOptimizationMode.FULL_COMPRESS);
        assertThat(batch.payload().length).isGreaterThan(0);

        // 2. Delta compression and decompression
        byte[] baseState = new byte[128];
        Arrays.fill(baseState, (byte) 0x20);

        byte[] newState = new byte[128];
        Arrays.fill(newState, (byte) 0x20);
        newState[10] = 0x55;
        newState[11] = 0x56;
        newState[50] = 0x77;

        OptimizedPacketBatch deltaBatch = optimizer.deltaCompress("vessel:state_delta", baseState, newState);
        assertThat(deltaBatch.mode()).isEqualTo(BandwidthOptimizationMode.DELTA_COMPRESS);

        byte[] reconstructed = optimizer.deltaDecompress(baseState, deltaBatch.payload());
        assertThat(reconstructed).isEqualTo(newState);

        // 3. Full stream Deflate compression and decompression
        byte[] largeData = new byte[1024];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 16);
        }

        OptimizedPacketBatch compBatch = optimizer.compressPayload("vessel:large_stream", largeData);
        assertThat(compBatch.mode()).isEqualTo(BandwidthOptimizationMode.FULL_COMPRESS);
        assertThat(compBatch.compressedBytes()).isLessThan(largeData.length);
        assertThat(compBatch.bytesSaved()).isGreaterThan(0);

        byte[] decompressed = optimizer.decompressPayload(compBatch.payload());
        assertThat(decompressed).isEqualTo(largeData);
    }
}
