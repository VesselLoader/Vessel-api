package dev.vessel.api.network;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-level bi-directional network channel for registering and dispatching {@link VesselPacketPayload}s.
 *
 * <p>Inspired by Forge's {@code SimpleChannel} and NeoForge's {@code PayloadRegistrar},
 * enabling modders to send type-safe messages between Client and Server without Netty boilerplate.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.1.0
 * @since 1.0.0
 */
public final class VesselPayloadChannel {

    private static final Map<String, VesselPayloadChannel> CHANNELS = new ConcurrentHashMap<>();

    private final String channelId;
    private final Map<String, VesselPacketCodec<?>> codecs;
    private final Map<String, VesselPacketConsumer<?>> consumers;

    private VesselPayloadChannel(String channelId) {
        this.channelId = Objects.requireNonNull(channelId, "channelId cannot be null");
        this.codecs = new ConcurrentHashMap<>();
        this.consumers = new ConcurrentHashMap<>();
    }

    /**
     * Resolves or creates a payload channel for a given channel identifier.
     *
     * @param channelId namespaced channel id (e.g. {@code "mymod:main"})
     * @return channel instance
     */
    public static VesselPayloadChannel create(String channelId) {
        return CHANNELS.computeIfAbsent(channelId, VesselPayloadChannel::new);
    }

    /**
     * Registers a custom packet payload with its serialization codec and message handler.
     *
     * @param <T>       payload type
     * @param payloadId namespaced payload id (e.g. {@code "mymod:speed_sync"})
     * @param type      payload class type
     * @param codec     serialization codec
     * @param handler   execution callback
     */
    public <T extends VesselPacketPayload> void register(
            String payloadId,
            Class<T> type,
            VesselPacketCodec<T> codec,
            VesselPacketConsumer<T> handler
    ) {
        Objects.requireNonNull(payloadId, "payloadId cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(codec, "codec cannot be null");
        Objects.requireNonNull(handler, "handler cannot be null");

        codecs.put(payloadId, codec);
        consumers.put(payloadId, handler);
    }

    /**
     * Inward delivery dispatch: decodes and dispatches received raw byte packet.
     *
     * @param payloadId packet id
     * @param data      raw byte array
     * @param context   execution context
     * @return true if successfully processed
     */
    @SuppressWarnings("unchecked")
    public boolean receive(String payloadId, byte[] data, VesselPacketContext context) {
        VesselPacketCodec<?> codec = codecs.get(payloadId);
        VesselPacketConsumer<?> consumer = consumers.get(payloadId);
        if (codec != null && consumer != null) {
            Object decoded = codec.decode(data);
            ((VesselPacketConsumer<VesselPacketPayload>) consumer).handle((VesselPacketPayload) decoded, context);
            return true;
        }
        return false;
    }

    /**
     * Sends packet from client to dedicated server.
     *
     * @param payload packet payload
     */
    public void sendToServer(VesselPacketPayload payload) {
        Objects.requireNonNull(payload, "payload cannot be null");
        VesselPacketContext ctx = new VesselPacketContext(null, channelId, true);
        VesselPacketCodec<?> codec = codecs.get(payload.getId());
        if (codec != null) {
            byte[] encoded = encodePayload(codec, payload);
            receive(payload.getId(), encoded, ctx);
        }
    }

    /**
     * Sends packet from server to specific player.
     *
     * @param player  target player
     * @param payload packet payload
     */
    public void sendToPlayer(Object player, VesselPacketPayload payload) {
        Objects.requireNonNull(player, "player cannot be null");
        Objects.requireNonNull(payload, "payload cannot be null");
        VesselPacketContext ctx = new VesselPacketContext(player, channelId, false);
        VesselPacketCodec<?> codec = codecs.get(payload.getId());
        if (codec != null) {
            byte[] encoded = encodePayload(codec, payload);
            receive(payload.getId(), encoded, ctx);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends VesselPacketPayload> byte[] encodePayload(VesselPacketCodec<?> codec, VesselPacketPayload payload) {
        return ((VesselPacketCodec<T>) codec).encode((T) payload);
    }

    public String getChannelId() {
        return channelId;
    }
}
