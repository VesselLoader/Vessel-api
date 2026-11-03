package dev.vessel.api.registry;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Immutability verification barrier guarding against unauthorized post-boot mutations
 * across dynamic and frozen registries under VUSA Chapter 3.4.
 *
 * <p>Strictly enforces immutability by asserting writable state before any registry mutation
 * and requires cryptographic UUID security tokens for authorized dynamic datapack reload phases.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0
 */
public final class RegistryFreezeVerificationGuard {

    private final Map<String, Boolean> freezeStates = new ConcurrentHashMap<>();
    private final Map<String, UUID> securityTokens = new ConcurrentHashMap<>();
    private final Map<String, Integer> frozenEntryCounts = new ConcurrentHashMap<>();

    /**
     * Constructs a new registry freeze verification guard.
     */
    public RegistryFreezeVerificationGuard() {
    }

    /**
     * Freezes the specified registry, recording its sealed entry count and sealing it against
     * further modifications.
     *
     * @param registryName namespaced registry identifier (e.g. "minecraft:item")
     * @param sealedCount  total number of entries at moment of freeze
     */
    public void freezeRegistry(String registryName, int sealedCount) {
        Objects.requireNonNull(registryName, "registryName cannot be null");
        if (sealedCount < 0) {
            throw new IllegalArgumentException("sealedCount cannot be negative");
        }

        this.freezeStates.put(registryName, Boolean.TRUE);
        this.frozenEntryCounts.put(registryName, sealedCount);
        this.securityTokens.remove(registryName);
    }

    /**
     * Asserts that the specified registry is in an unlocked, writable state.
     *
     * @param registryName namespaced registry identifier
     * @throws IllegalStateException if registry is frozen and immutable
     */
    public void assertWritable(String registryName) {
        Objects.requireNonNull(registryName, "registryName cannot be null");

        if (Boolean.TRUE.equals(this.freezeStates.get(registryName))) {
            throw new IllegalStateException(
                    "Registry '" + registryName + "' is FROZEN and immutable. "
                            + "Unauthorized modification attempt blocked by RegistryFreezeVerificationGuard."
            );
        }
    }

    /**
     * Inspects whether the given registry is currently frozen.
     *
     * @param registryName namespaced registry identifier
     * @return true if frozen, false if writable or unmonitored
     */
    public boolean isFrozen(String registryName) {
        if (registryName == null) {
            return false;
        }
        return Boolean.TRUE.equals(this.freezeStates.get(registryName));
    }

    /**
     * Issues a single-use administrative security token allowing controlled unfreeze for dynamic datapacks.
     *
     * @param registryName namespaced registry identifier
     * @return cryptographically random UUID unfreeze token
     */
    public UUID issueTemporaryUnfreezeToken(String registryName) {
        Objects.requireNonNull(registryName, "registryName cannot be null");

        UUID token = UUID.randomUUID();
        this.securityTokens.put(registryName, token);
        return token;
    }

    /**
     * Unfreezes the registry if a valid security token is presented.
     *
     * @param registryName namespaced registry identifier
     * @param token        security token issued by {@link #issueTemporaryUnfreezeToken(String)}
     * @return true if unfreeze was authorized and successful
     * @throws SecurityException if token is missing, null, or invalid
     */
    public boolean unfreezeWithToken(String registryName, UUID token) {
        Objects.requireNonNull(registryName, "registryName cannot be null");
        if (token == null) {
            throw new SecurityException("Unfreeze rejected: security token cannot be null");
        }

        UUID validToken = this.securityTokens.get(registryName);
        if (validToken == null || !validToken.equals(token)) {
            throw new SecurityException("Unfreeze rejected: invalid security token for registry '" + registryName + "'");
        }

        this.freezeStates.put(registryName, Boolean.FALSE);
        this.securityTokens.remove(registryName);
        return true;
    }

    /**
     * Verifies the structural integrity of a frozen registry against its sealed entry count.
     *
     * @param registryName     namespaced registry identifier
     * @param actualEntryCount current entry count
     * @throws IllegalStateException if the entry count diverges from sealed snapshot
     */
    public void verifyIntegrity(String registryName, int actualEntryCount) {
        Objects.requireNonNull(registryName, "registryName cannot be null");

        if (isFrozen(registryName)) {
            Integer expected = this.frozenEntryCounts.get(registryName);
            if (expected != null && expected != actualEntryCount) {
                throw new IllegalStateException(
                        "Registry integrity violation for '" + registryName + "': expected "
                                + expected + " entries, but found " + actualEntryCount
                );
            }
        }
    }

    /**
     * Returns the recorded sealed entry count for the given registry.
     *
     * @param registryName namespaced registry identifier
     * @return entry count at freeze time, or -1 if never frozen
     */
    public int getFrozenEntryCount(String registryName) {
        if (registryName == null) {
            return -1;
        }
        return this.frozenEntryCounts.getOrDefault(registryName, -1);
    }

    /**
     * Resets all monitored freeze states and tokens.
     */
    public void resetAll() {
        this.freezeStates.clear();
        this.securityTokens.clear();
        this.frozenEntryCounts.clear();
    }
}
