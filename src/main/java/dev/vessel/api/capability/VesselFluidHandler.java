package dev.vessel.api.capability;

/**
 * Universal fluid storage and transfer interface for the VESSEL Super-API ecosystem.
 *
 * <p>Bridges Forge/NeoForge milli-Bucket standard ($1\text{ bucket} = 1000\text{ mB}$)
 * and Fabric Transfer API droplet standard ($1\text{ bucket} = 81{,}000\text{ droplets}$).
 * Conversion factor: $1\text{ mB} = 81\text{ droplets}$.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.1.0
 * @since 1.0.0
 */
public interface VesselFluidHandler {

    long DROPLETS_PER_MB = 81L;
    long MB_PER_BUCKET = 1000L;
    long DROPLETS_PER_BUCKET = MB_PER_BUCKET * DROPLETS_PER_MB; // 81,000

    /**
     * Resolves the namespaced type identifier of the stored fluid (e.g. {@code "minecraft:water"}).
     *
     * @return namespaced fluid id, or empty string if tank is empty
     */
    String getFluidType();

    /**
     * Resolves the current fluid volume in milli-Buckets (mB).
     *
     * @return fluid volume in mB
     */
    long getFluidAmount();

    /**
     * Resolves the current fluid volume converted to Fabric Transfer API droplets.
     *
     * @return fluid volume in droplets
     */
    default long getFluidAmountDroplets() {
        return getFluidAmount() * DROPLETS_PER_MB;
    }

    /**
     * Resolves the total tank capacity in milli-Buckets (mB).
     *
     * @return capacity in mB
     */
    long getCapacity();

    /**
     * Resolves the total tank capacity in Fabric droplets.
     *
     * @return capacity in droplets
     */
    default long getCapacityDroplets() {
        return getCapacity() * DROPLETS_PER_MB;
    }

    /**
     * Fills the tank with a specified fluid type and volume in mB.
     *
     * @param fluidType namespaced fluid type
     * @param amount    volume to fill in mB
     * @param simulate  if true, calculates return without modifying internal state
     * @return actual amount filled in mB
     */
    long fill(String fluidType, long amount, boolean simulate);

    /**
     * Drains fluid from the tank.
     *
     * @param fluidType expected fluid type, or empty string for any
     * @param maxDrain  maximum volume to drain in mB
     * @param simulate  if true, calculates return without modifying internal state
     * @return actual amount drained in mB
     */
    long drain(String fluidType, long maxDrain, boolean simulate);
}
