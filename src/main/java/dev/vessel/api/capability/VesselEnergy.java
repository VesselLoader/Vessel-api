package dev.vessel.api.capability;

/**
 * Universal energy storage and transfer interface for the VESSEL Super-API ecosystem.
 *
 * <p>Standardizes Forge Energy (FE / RF) and Fabric Energy (TechReborn Energy / Team Reborn)
 * into a single unified contract with a 1:1 numerical equivalence.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.1.0
 * @since 1.0.0
 */
public interface VesselEnergy {

    /**
     * Resolves currently stored energy amount in Forge Energy (FE) units.
     *
     * @return current energy amount
     */
    long getEnergyStored();

    /**
     * Resolves maximum energy storage capacity in Forge Energy (FE) units.
     *
     * @return maximum energy capacity
     */
    long getMaxEnergyStored();

    /**
     * Injects energy into the container.
     *
     * @param maxReceive maximum energy units to insert
     * @param simulate   if true, calculates return value without modifying stored state
     * @return actual energy units received
     */
    long receiveEnergy(long maxReceive, boolean simulate);

    /**
     * Extracts energy from the container.
     *
     * @param maxExtract maximum energy units to extract
     * @param simulate   if true, calculates return value without modifying stored state
     * @return actual energy units extracted
     */
    long extractEnergy(long maxExtract, boolean simulate);

    /**
     * Checks if energy extraction is supported.
     *
     * @return true if extraction is allowed
     */
    default boolean canExtract() {
        return true;
    }

    /**
     * Checks if energy reception is supported.
     *
     * @return true if reception is allowed
     */
    default boolean canReceive() {
        return true;
    }
}
