package dev.vessel.api.capability;

import java.util.Objects;
import java.util.Optional;

/**
 * Unified cross-loader capability representation bridging Forge IItemHandler,
 * NeoForge Capabilities, and Fabric Transfer API Storage contracts under VUSA Chapter 3.2.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @param <T> capability provider contract interface type
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
public final class VesselUnifiedCapability<T> {

    private final String capabilityId;
    private final Class<T> capabilityType;
    private final T provider;
    private volatile boolean valid;

    /**
     * Constructs a new unified capability handle.
     *
     * @param capabilityId   canonical capability identifier (e.g. "vessel:item_storage")
     * @param capabilityType class object representing the capability contract
     * @param provider       the concrete provider instance
     */
    public VesselUnifiedCapability(String capabilityId, Class<T> capabilityType, T provider) {
        this.capabilityId = Objects.requireNonNull(capabilityId, "capabilityId cannot be null");
        this.capabilityType = Objects.requireNonNull(capabilityType, "capabilityType cannot be null");
        this.provider = Objects.requireNonNull(provider, "provider cannot be null");
        this.valid = true;
    }

    /**
     * Returns the canonical capability identifier.
     *
     * @return capability identifier string
     */
    public String getCapabilityId() {
        return capabilityId;
    }

    /**
     * Returns the class contract of this capability.
     *
     * @return capability contract class
     */
    public Class<T> getCapabilityType() {
        return capabilityType;
    }

    /**
     * Resolves the active provider if this capability is still valid.
     *
     * @return Optional containing the provider if valid, or Optional.empty() if invalidated
     */
    public Optional<T> getProvider() {
        if (!valid) {
            return Optional.empty();
        }
        return Optional.of(provider);
    }

    /**
     * Checks if this capability is currently valid and unexpired.
     *
     * @return true if valid, false if invalidated or chunk unmounted
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Invalidates this capability, revoking further provider access.
     */
    public void invalidate() {
        this.valid = false;
    }

    @Override
    public String toString() {
        return "VesselUnifiedCapability[id=" + capabilityId
                + ", type=" + capabilityType.getName()
                + ", valid=" + valid + "]";
    }
}
