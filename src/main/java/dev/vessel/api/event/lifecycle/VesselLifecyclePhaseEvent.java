// ==============================================================================
// VESSEL-API — VesselLifecyclePhaseEvent.java
// Version: 1.0.0
// Changelog:
// - 1.0.0: Initial creation of immutable VesselLifecyclePhaseEvent contract
//          bridging core boot phase transitions to VUSA event subscribers.
// ==============================================================================

package dev.vessel.api.event.lifecycle;

import java.util.Objects;

/**
 * Immutable lifecycle event emitted when the VESSEL server transitions between
 * {@link VesselServerBootPhase} states.
 *
 * <p>Provides the previous phase, the newly active phase, the active Minecraft
 * version string, and the epoch timestamp of the transition.</p>
 *
 * @version 1.0.0
 */
public final class VesselLifecyclePhaseEvent {

    private final VesselServerBootPhase previousPhase;
    private final VesselServerBootPhase currentPhase;
    private final String activeMinecraftVersion;
    private final long timestampEpochMillis;

    /**
     * Constructs a new {@link VesselLifecyclePhaseEvent} with an explicit timestamp.
     *
     * @param previousPhase          the phase prior to the transition (must not be {@code null})
     * @param currentPhase           the phase entered by the transition (must not be {@code null})
     * @param activeMinecraftVersion the active Minecraft version identifier (must not be {@code null} or blank)
     * @param timestampEpochMillis   the transition timestamp in epoch milliseconds (must be {@code >= 0})
     * @throws NullPointerException     if any reference argument is {@code null}
     * @throws IllegalArgumentException if {@code activeMinecraftVersion} is blank or {@code timestampEpochMillis < 0}
     */
    public VesselLifecyclePhaseEvent(
            VesselServerBootPhase previousPhase,
            VesselServerBootPhase currentPhase,
            String activeMinecraftVersion,
            long timestampEpochMillis) {
        this.previousPhase = Objects.requireNonNull(previousPhase, "previousPhase must not be null");
        this.currentPhase = Objects.requireNonNull(currentPhase, "currentPhase must not be null");
        Objects.requireNonNull(activeMinecraftVersion, "activeMinecraftVersion must not be null");
        if (activeMinecraftVersion.isBlank()) {
            throw new IllegalArgumentException("activeMinecraftVersion must not be blank");
        }
        if (timestampEpochMillis < 0L) {
            throw new IllegalArgumentException("timestampEpochMillis must not be negative");
        }
        this.activeMinecraftVersion = activeMinecraftVersion;
        this.timestampEpochMillis = timestampEpochMillis;
    }

    /**
     * Constructs a new {@link VesselLifecyclePhaseEvent} stamped with {@link System#currentTimeMillis()}.
     *
     * @param previousPhase          the phase prior to the transition (must not be {@code null})
     * @param currentPhase           the phase entered by the transition (must not be {@code null})
     * @param activeMinecraftVersion the active Minecraft version identifier (must not be {@code null} or blank)
     * @throws NullPointerException     if any reference argument is {@code null}
     * @throws IllegalArgumentException if {@code activeMinecraftVersion} is blank
     */
    public VesselLifecyclePhaseEvent(
            VesselServerBootPhase previousPhase,
            VesselServerBootPhase currentPhase,
            String activeMinecraftVersion) {
        this(previousPhase, currentPhase, activeMinecraftVersion, System.currentTimeMillis());
    }

    /**
     * Returns the lifecycle phase prior to this transition.
     *
     * @return the previous {@link VesselServerBootPhase}
     */
    public VesselServerBootPhase getPreviousPhase() {
        return this.previousPhase;
    }

    /**
     * Returns the current lifecycle phase entered by this transition.
     *
     * @return the current {@link VesselServerBootPhase}
     */
    public VesselServerBootPhase getCurrentPhase() {
        return this.currentPhase;
    }

    /**
     * Returns the active Minecraft version string associated with the running server.
     *
     * @return the active Minecraft version string
     */
    public String getActiveMinecraftVersion() {
        return this.activeMinecraftVersion;
    }

    /**
     * Returns the epoch millisecond timestamp when this phase transition occurred.
     *
     * @return epoch milliseconds
     */
    public long getTimestampEpochMillis() {
        return this.timestampEpochMillis;
    }

    /**
     * Checks whether this transition moves forward (or remains at the same stage order)
     * in the canonical boot sequence.
     *
     * @return {@code true} if {@code currentPhase.getStageOrder() >= previousPhase.getStageOrder()}
     */
    public boolean isForwardTransition() {
        return this.currentPhase.getStageOrder() >= this.previousPhase.getStageOrder();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof VesselLifecyclePhaseEvent other)) {
            return false;
        }
        return this.timestampEpochMillis == other.timestampEpochMillis
                && this.previousPhase == other.previousPhase
                && this.currentPhase == other.currentPhase
                && this.activeMinecraftVersion.equals(other.activeMinecraftVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                this.previousPhase,
                this.currentPhase,
                this.activeMinecraftVersion,
                this.timestampEpochMillis);
    }

    @Override
    public String toString() {
        return "VesselLifecyclePhaseEvent{"
                + "previousPhase=" + this.previousPhase
                + ", currentPhase=" + this.currentPhase
                + ", activeMinecraftVersion='" + this.activeMinecraftVersion + '\''
                + ", timestampEpochMillis=" + this.timestampEpochMillis
                + '}';
    }
}
