package dev.vessel.api.proxy;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable VUSA snapshot representing a proxy player session
 * ({@code dev.vessel.api.proxy.VesselProxySessionSnapshot}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param sessionId       unique session identifier
 * @param username        player username
 * @param proxyType       proxy architecture type (e.g., "VELOCITY", "BUNGEECORD")
 * @param connectedServer backend server identifier
 * @param pingMillis      client ping in milliseconds
 * @version 1.0.0
 */
public record VesselProxySessionSnapshot(
        UUID sessionId,
        String username,
        String proxyType,
        String connectedServer,
        long pingMillis) {

    public VesselProxySessionSnapshot {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(proxyType, "proxyType must not be null");
        if (username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        if (pingMillis < 0) {
            throw new IllegalArgumentException("pingMillis must be non-negative: " + pingMillis);
        }
    }

    public boolean isConnectedTo(String serverName) {
        return connectedServer != null && connectedServer.equalsIgnoreCase(serverName);
    }
}
