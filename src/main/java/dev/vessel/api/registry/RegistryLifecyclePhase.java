package dev.vessel.api.registry;

/**
 * Lifecycle phases for the Vessel Universal Registry engine.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner or nested classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
public enum RegistryLifecyclePhase {

    /**
     * Engine pre-flight boot phase before mod loader initialization.
     */
    PRE_FLIGHT("PRE_FLIGHT", true),

    /**
     * Active mod registration phase where Forge, NeoForge, and Fabric entries are gathered.
     */
    MOD_REGISTRATION("MOD_REGISTRATION", true),

    /**
     * Dynamic datapack reload phase for runtime data and recipe updates.
     */
    DYNAMIC_RELOAD("DYNAMIC_RELOAD", true),

    /**
     * Immutable post-boot frozen state. No further static entry mutations permitted.
     */
    FROZEN("FROZEN", false);

    private final String phaseName;
    private final boolean registrationAllowed;

    RegistryLifecyclePhase(String phaseName, boolean registrationAllowed) {
        this.phaseName = phaseName;
        this.registrationAllowed = registrationAllowed;
    }

    /**
     * Resolves the canonical phase name.
     *
     * @return phase name string
     */
    public String phaseName() {
        return phaseName;
    }

    /**
     * Indicates whether entry registration is permitted in this phase.
     *
     * @return true if registration is allowed
     */
    public boolean isRegistrationAllowed() {
        return registrationAllowed;
    }
}
