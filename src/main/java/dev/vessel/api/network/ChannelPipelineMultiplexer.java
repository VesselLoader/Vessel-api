package dev.vessel.api.network;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Multiplexes custom packet channels over the server network pipeline.
 *
 * <p>Enables concurrent channel registration, session subscription routing, and payload dispatching
 * with zero GC allocation on fast paths.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
public final class ChannelPipelineMultiplexer {

    public static final int DEFAULT_INITIAL_CHANNEL_CAPACITY = 64;
    public static final int DEFAULT_INITIAL_SESSION_CAPACITY = 128;

    private final Map<String, VesselNetworkChannel> registeredChannels;
    private final Map<String, Set<String>> sessionSubscriptions;
    private final Map<String, List<NetworkPayloadHandler>> channelHandlers;

    /**
     * Constructs a multiplexer with default initial capacity.
     */
    public ChannelPipelineMultiplexer() {
        this(DEFAULT_INITIAL_CHANNEL_CAPACITY, DEFAULT_INITIAL_SESSION_CAPACITY);
    }

    /**
     * Constructs a multiplexer with specified capacities.
     *
     * @param channelCapacity initial channel map capacity
     * @param sessionCapacity initial session map capacity
     */
    public ChannelPipelineMultiplexer(int channelCapacity, int sessionCapacity) {
        this.registeredChannels = new ConcurrentHashMap<>(channelCapacity);
        this.sessionSubscriptions = new ConcurrentHashMap<>(sessionCapacity);
        this.channelHandlers = new ConcurrentHashMap<>(channelCapacity);
    }

    /**
     * Registers a network channel into the pipeline.
     *
     * @param channel channel to register
     * @throws NullPointerException if channel is null
     */
    public void registerChannel(VesselNetworkChannel channel) {
        Objects.requireNonNull(channel, "channel cannot be null");
        registeredChannels.put(channel.identifier(), channel);
    }

    /**
     * Unregisters a channel from the pipeline.
     *
     * @param identifier channel identifier to remove
     * @return true if channel was previously present
     */
    public boolean unregisterChannel(String identifier) {
        Objects.requireNonNull(identifier, "identifier cannot be null");
        channelHandlers.remove(identifier);
        return registeredChannels.remove(identifier) != null;
    }

    /**
     * Retrieves a registered channel by identifier.
     *
     * @param identifier channel identifier
     * @return Optional containing the channel if registered
     */
    public Optional<VesselNetworkChannel> getChannel(String identifier) {
        Objects.requireNonNull(identifier, "identifier cannot be null");
        return Optional.ofNullable(registeredChannels.get(identifier));
    }

    /**
     * Checks if a channel identifier is currently registered.
     *
     * @param identifier channel identifier
     * @return true if registered
     */
    public boolean hasChannel(String identifier) {
        Objects.requireNonNull(identifier, "identifier cannot be null");
        return registeredChannels.containsKey(identifier);
    }

    /**
     * Subscribes a client session to a channel.
     *
     * @param sessionId         unique session identifier
     * @param channelIdentifier channel to subscribe
     */
    public void subscribeSession(String sessionId, String channelIdentifier) {
        Objects.requireNonNull(sessionId, "sessionId cannot be null");
        Objects.requireNonNull(channelIdentifier, "channelIdentifier cannot be null");
        sessionSubscriptions.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet())
                .add(channelIdentifier);
    }

    /**
     * Unsubscribes a client session from a channel.
     *
     * @param sessionId         unique session identifier
     * @param channelIdentifier channel to unsubscribe
     * @return true if subscription existed and was removed
     */
    public boolean unsubscribeSession(String sessionId, String channelIdentifier) {
        Objects.requireNonNull(sessionId, "sessionId cannot be null");
        Objects.requireNonNull(channelIdentifier, "channelIdentifier cannot be null");
        Set<String> channels = sessionSubscriptions.get(sessionId);
        if (channels != null) {
            return channels.remove(channelIdentifier);
        }
        return false;
    }

    /**
     * Cleans up all channel subscriptions for a disconnected session.
     *
     * @param sessionId unique session identifier
     */
    public void removeSession(String sessionId) {
        Objects.requireNonNull(sessionId, "sessionId cannot be null");
        sessionSubscriptions.remove(sessionId);
    }

    /**
     * Checks whether a session is subscribed to a specific channel.
     *
     * @param sessionId         unique session identifier
     * @param channelIdentifier channel identifier
     * @return true if subscribed
     */
    public boolean isSessionSubscribed(String sessionId, String channelIdentifier) {
        Objects.requireNonNull(sessionId, "sessionId cannot be null");
        Objects.requireNonNull(channelIdentifier, "channelIdentifier cannot be null");
        Set<String> channels = sessionSubscriptions.get(sessionId);
        return channels != null && channels.contains(channelIdentifier);
    }

    /**
     * Resolves all channels subscribed by a specific session.
     *
     * @param sessionId unique session identifier
     * @return unmodifiable set of subscribed channel identifiers
     */
    public Set<String> getSubscribedChannels(String sessionId) {
        Objects.requireNonNull(sessionId, "sessionId cannot be null");
        Set<String> channels = sessionSubscriptions.get(sessionId);
        if (channels == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(channels);
    }

    /**
     * Registers a payload handler listener for incoming packets on a channel.
     *
     * @param channelIdentifier target channel identifier
     * @param handler           handler implementation
     */
    public void registerHandler(String channelIdentifier, NetworkPayloadHandler handler) {
        Objects.requireNonNull(channelIdentifier, "channelIdentifier cannot be null");
        Objects.requireNonNull(handler, "handler cannot be null");
        channelHandlers.computeIfAbsent(channelIdentifier, k -> new CopyOnWriteArrayList<>())
                .add(handler);
    }

    /**
     * Unregisters all handlers for a given channel.
     *
     * @param channelIdentifier channel identifier
     */
    public void unregisterHandlers(String channelIdentifier) {
        Objects.requireNonNull(channelIdentifier, "channelIdentifier cannot be null");
        channelHandlers.remove(channelIdentifier);
    }

    /**
     * Dispatches an incoming custom payload to all registered handlers for the channel.
     *
     * @param channelIdentifier channel identifier
     * @param sessionId         source session identifier
     * @param payload           raw byte payload
     * @return number of handlers that processed the payload
     */
    public int dispatchIncoming(String channelIdentifier, String sessionId, byte[] payload) {
        Objects.requireNonNull(channelIdentifier, "channelIdentifier cannot be null");
        Objects.requireNonNull(sessionId, "sessionId cannot be null");
        Objects.requireNonNull(payload, "payload cannot be null");

        VesselNetworkChannel channel = registeredChannels.get(channelIdentifier);
        if (channel == null) {
            return 0;
        }

        channel.validatePayloadSize(payload.length);

        List<NetworkPayloadHandler> handlers = channelHandlers.get(channelIdentifier);
        if (handlers == null || handlers.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (NetworkPayloadHandler handler : handlers) {
            handler.handlePayload(channel, sessionId, payload);
            count++;
        }
        return count;
    }

    /**
     * Checks if a clientbound packet can be routed to a session.
     *
     * @param channelIdentifier channel identifier
     * @param sessionId         target session identifier
     * @return true if channel exists and session is subscribed (or universal)
     */
    public boolean canRouteClientbound(String channelIdentifier, String sessionId) {
        Objects.requireNonNull(channelIdentifier, "channelIdentifier cannot be null");
        Objects.requireNonNull(sessionId, "sessionId cannot be null");

        VesselNetworkChannel channel = registeredChannels.get(channelIdentifier);
        if (channel == null || !channel.direction().allowsClientbound()) {
            return false;
        }

        return isSessionSubscribed(sessionId, channelIdentifier);
    }

    /**
     * Total number of currently registered channels.
     *
     * @return channel count
     */
    public int totalRegisteredChannels() {
        return registeredChannels.size();
    }

    /**
     * Total active sessions tracked.
     *
     * @return session count
     */
    public int totalTrackedSessions() {
        return sessionSubscriptions.size();
    }

    /**
     * Clears all channels, subscriptions, and handlers.
     */
    public void clearAll() {
        registeredChannels.clear();
        sessionSubscriptions.clear();
        channelHandlers.clear();
    }
}
