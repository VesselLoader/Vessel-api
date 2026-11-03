// ==============================================================================
// VESSEL-API — VesselServerBootPhase.java
// Version: 1.0.0
// Changelog:
// - 1.0.0: Initial creation of VesselServerBootPhase enum defining canonical
//          boot lifecycle phases in accordance with ADR-0001 (VES-BOOT-002).
// ==============================================================================

package dev.vessel.api.event.lifecycle;

import java.util.Locale;
import java.util.Objects;

/**
 * Canonical enumeration of VESSEL server boot lifecycle phases as mandated by
 * {@code ADR-0001} ({@code VES-BOOT-002}: Deferred Bukkit/Spigot World Activation
 * until Post-Vanilla-Bootstrap).
 *
 * <p>Each phase defines a deterministic stage ordering index and indicates whether
 * server world registries and level state are safely accessible to mods and plugins.</p>
 *
 * @version 1.0.0
 */
public enum VesselServerBootPhase {

    /**
     * Initial state before the VESSEL bootstrap harness has initialized.
     */
    UNINITIALIZED(0, false),

    /**
     * Early pre-main mod initialization phase where NeoForge/Fabric entrypoints
     * register blocks, items, and data structures prior to vanilla server startup.
     */
    PRE_MAIN_MOD_INIT(1, false),

    /**
     * Vanilla bootstrap and registry freeze phase. Bukkit/Spigot world access
     * remains strictly deferred during this stage.
     */
    VANILLA_BOOTSTRAP_ACTIVE(2, false),

    /**
     * Dedicated server instance is starting and world levels are being prepared
     * for activation.
     */
    SERVER_STARTING(3, true),

    /**
     * Server has completed post-world activation and all hybrid mod/plugin
     * registries and worlds are fully accessible.
     */
    SERVER_STARTED_POST_WORLD(4, true),

    /**
     * Server is shutting down and releasing world and plugin resources.
     */
    SERVER_STOPPING(5, false);

    private final int stageOrder;
    private final boolean worldAccessible;

    VesselServerBootPhase(int stageOrder, boolean worldAccessible) {
        this.stageOrder = stageOrder;
        this.worldAccessible = worldAccessible;
    }

    /**
     * Returns the monotonic ordinal stage index for lifecycle progression checks.
     *
     * @return stage order index ({@code 0..5})
     */
    public int getStageOrder() {
        return this.stageOrder;
    }

    /**
     * Indicates whether world state and level registries are accessible during this phase.
     *
     * @return {@code true} if worlds are accessible, {@code false} otherwise
     */
    public boolean isWorldAccessible() {
        return this.worldAccessible;
    }

    /**
     * Determines whether this boot phase occurs strictly after the specified phase.
     *
     * @param other the phase to compare against (must not be {@code null})
     * @return {@code true} if {@code this.stageOrder > other.stageOrder}
     * @throws NullPointerException if {@code other} is {@code null}
     */
    public boolean isAfter(VesselServerBootPhase other) {
        Objects.requireNonNull(other, "other phase must not be null");
        return this.stageOrder > other.stageOrder;
    }

    /**
     * Performs a null-safe lookup of a {@link VesselServerBootPhase} by its name,
     * defaulting to {@link #UNINITIALIZED} if the input is {@code null}, blank, or unknown.
     *
     * @param phaseName the phase name to resolve (may be {@code null})
     * @return the matching {@link VesselServerBootPhase}, or {@link #UNINITIALIZED} if not found
     */
    public static VesselServerBootPhase fromPhaseName(String phaseName) {
        if (phaseName == null || phaseName.isBlank()) {
            return UNINITIALIZED;
        }
        String normalized = phaseName.trim().toUpperCase(Locale.ROOT);
        for (VesselServerBootPhase phase : values()) {
            if (phase.name().equals(normalized)) {
                return phase;
            }
        }
        return UNINITIALIZED;
    }
}
