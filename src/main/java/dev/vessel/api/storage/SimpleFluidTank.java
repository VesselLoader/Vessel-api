package dev.vessel.api.storage;

import java.util.Objects;

/**
 * Clean-room immutable fluid tank representation for cross-loader storage operations.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner or nested classes.</p>
 *
 * @param fluidId      canonical identifier of the fluid (e.g. "minecraft:water", "minecraft:lava")
 * @param millibuckets current volume in millibuckets (mB)
 * @param capacityMb   total capacity in millibuckets
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record SimpleFluidTank(
        String fluidId,
        long millibuckets,
        long capacityMb
) {

    public static final String EMPTY_FLUID_ID = "minecraft:empty";
    public static final SimpleFluidTank EMPTY = new SimpleFluidTank(EMPTY_FLUID_ID, 0L, 0L);

    public SimpleFluidTank {
        Objects.requireNonNull(fluidId, "fluidId cannot be null");
        if (millibuckets < 0) {
            throw new IllegalArgumentException("millibuckets cannot be negative: " + millibuckets);
        }
        if (capacityMb < 0) {
            throw new IllegalArgumentException("capacityMb cannot be negative: " + capacityMb);
        }
    }

    /**
     * Checks if this tank holds no fluid.
     *
     * @return true if volume is 0 or fluid id is empty
     */
    public boolean isEmpty() {
        return millibuckets <= 0 || EMPTY_FLUID_ID.equals(fluidId);
    }

    /**
     * Calculates remaining space in millibuckets.
     *
     * @return remaining capacity
     */
    public long getRemainingSpace() {
        return Math.max(0L, capacityMb - millibuckets);
    }

    /**
     * Returns an updated tank with new fluid level.
     *
     * @param newVolume new volume in millibuckets
     * @return updated tank
     */
    public SimpleFluidTank withVolume(long newVolume) {
        long bounded = Math.min(Math.max(0L, newVolume), capacityMb);
        if (bounded == 0L) {
            return new SimpleFluidTank(EMPTY_FLUID_ID, 0L, capacityMb);
        }
        return new SimpleFluidTank(fluidId, bounded, capacityMb);
    }
}
