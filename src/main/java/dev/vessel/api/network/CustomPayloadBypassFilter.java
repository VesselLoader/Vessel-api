package dev.vessel.api.network;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Filters and absorbs incompatible modded packets to avoid packet decoder desynchronization
 * and client disconnects on Vanilla and Bedrock clients.
 *
 * <p>Implements Chapter 3.5 of the VESSEL Super-API specification: silently absorbs unrecognized
 * modded payloads while routing animation/motion channels toward Bedrock transpilation.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
public final class CustomPayloadBypassFilter {

    public static final String MINECRAFT_BRAND_CHANNEL = "minecraft:brand";
    public static final String MINECRAFT_REGISTER_CHANNEL = "minecraft:register";
    public static final String MINECRAFT_UNREGISTER_CHANNEL = "minecraft:unregister";

    private final Set<String> knownVanillaChannels;
    private final Set<String> transpilableAnimationChannels;

    private final AtomicLong absorbedPacketCount;
    private final AtomicLong forwardedPacketCount;
    private final AtomicLong transpiledPacketCount;
    private final AtomicLong absorbedBytesCount;

    /**
     * Constructs a default custom payload bypass filter with standard vanilla and animation channels.
     */
    public CustomPayloadBypassFilter() {
        this.knownVanillaChannels = ConcurrentHashMap.newKeySet();
        this.transpilableAnimationChannels = ConcurrentHashMap.newKeySet();

        this.absorbedPacketCount = new AtomicLong(0L);
        this.forwardedPacketCount = new AtomicLong(0L);
        this.transpiledPacketCount = new AtomicLong(0L);
        this.absorbedBytesCount = new AtomicLong(0L);

        // Register default standard vanilla-compatible channels
        this.knownVanillaChannels.add(MINECRAFT_BRAND_CHANNEL);
        this.knownVanillaChannels.add(MINECRAFT_REGISTER_CHANNEL);
        this.knownVanillaChannels.add(MINECRAFT_UNREGISTER_CHANNEL);
    }

    /**
     * Evaluates an outgoing payload against target client type.
     *
     * @param channelIdentifier channel identifier
     * @param clientType        target client category
     * @param payloadLength     byte length of the payload
     * @return filter action result
     */
    public BypassFilterResult evaluate(String channelIdentifier, NetworkClientType clientType, int payloadLength) {
        Objects.requireNonNull(channelIdentifier, "channelIdentifier cannot be null");
        Objects.requireNonNull(clientType, "clientType cannot be null");

        // Modded clients receive all modded and vanilla channels directly
        if (clientType.isModded()) {
            forwardedPacketCount.incrementAndGet();
            return BypassFilterResult.FORWARD;
        }

        // Vanilla channels are forwarded to vanilla and bedrock clients
        if (isKnownVanillaChannel(channelIdentifier)) {
            forwardedPacketCount.incrementAndGet();
            return BypassFilterResult.FORWARD;
        }

        // If client is Bedrock or Vanilla, check if channel can be transpiled into Display Entities
        if (isTranspilableAnimationChannel(channelIdentifier)) {
            transpiledPacketCount.incrementAndGet();
            return BypassFilterResult.TRANSPILE_BEDROCK;
        }

        // Incompatible modded payload: silently absorb to avoid kick
        absorbedPacketCount.incrementAndGet();
        absorbedBytesCount.addAndGet(payloadLength);
        return BypassFilterResult.ABSORB_SILENT;
    }

    /**
     * Evaluates a payload using a registered channel instance.
     *
     * @param channel    network channel instance
     * @param clientType target client category
     * @param payload    byte array payload
     * @return filter action result
     */
    public BypassFilterResult evaluate(VesselNetworkChannel channel, NetworkClientType clientType, byte[] payload) {
        Objects.requireNonNull(channel, "channel cannot be null");
        Objects.requireNonNull(payload, "payload cannot be null");

        if (!channel.requiresModdedClient()) {
            forwardedPacketCount.incrementAndGet();
            return BypassFilterResult.FORWARD;
        }

        return evaluate(channel.identifier(), clientType, payload.length);
    }

    /**
     * Registers a channel as known and safe for standard Vanilla clients.
     *
     * @param channelIdentifier channel identifier
     */
    public void registerKnownVanillaChannel(String channelIdentifier) {
        Objects.requireNonNull(channelIdentifier, "channelIdentifier cannot be null");
        knownVanillaChannels.add(channelIdentifier);
    }

    /**
     * Registers a channel as eligible for Bedrock Display Entity transpilation.
     *
     * @param channelIdentifier channel identifier
     */
    public void registerTranspilableAnimationChannel(String channelIdentifier) {
        Objects.requireNonNull(channelIdentifier, "channelIdentifier cannot be null");
        transpilableAnimationChannels.add(channelIdentifier);
    }

    /**
     * Unregisters a transpilable animation channel.
     *
     * @param channelIdentifier channel identifier
     * @return true if previously present
     */
    public boolean unregisterTranspilableAnimationChannel(String channelIdentifier) {
        Objects.requireNonNull(channelIdentifier, "channelIdentifier cannot be null");
        return transpilableAnimationChannels.remove(channelIdentifier);
    }

    /**
     * Checks if a channel is recognized as safe for vanilla clients.
     *
     * @param channelIdentifier channel identifier
     * @return true if vanilla-safe
     */
    public boolean isKnownVanillaChannel(String channelIdentifier) {
        Objects.requireNonNull(channelIdentifier, "channelIdentifier cannot be null");
        return knownVanillaChannels.contains(channelIdentifier);
    }

    /**
     * Checks if a channel is recognized as transpilable into display entities.
     *
     * @param channelIdentifier channel identifier
     * @return true if transpilable
     */
    public boolean isTranspilableAnimationChannel(String channelIdentifier) {
        Objects.requireNonNull(channelIdentifier, "channelIdentifier cannot be null");
        return transpilableAnimationChannels.contains(channelIdentifier);
    }

    /**
     * Resolves total count of packets silently absorbed.
     *
     * @return absorbed packet count
     */
    public long absorbedPacketCount() {
        return absorbedPacketCount.get();
    }

    /**
     * Resolves total count of packets forwarded.
     *
     * @return forwarded packet count
     */
    public long forwardedPacketCount() {
        return forwardedPacketCount.get();
    }

    /**
     * Resolves total count of packets routed to transpilation.
     *
     * @return transpiled packet count
     */
    public long transpiledPacketCount() {
        return transpiledPacketCount.get();
    }

    /**
     * Resolves total bytes saved / absorbed.
     *
     * @return absorbed byte count
     */
    public long absorbedBytesCount() {
        return absorbedBytesCount.get();
    }

    /**
     * Resets all filter metrics to zero.
     */
    public void resetMetrics() {
        absorbedPacketCount.set(0L);
        forwardedPacketCount.set(0L);
        transpiledPacketCount.set(0L);
        absorbedBytesCount.set(0L);
    }
}
