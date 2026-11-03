package dev.vessel.api.energy;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Universal primitive energy read accessor providing zero-overhead, reflection-free inspection
 * of energy storage units across Forge/NeoForge ({@code IEnergyStorage}) and Fabric ({@code EnergyStorage}).
 *
 * <p>Transmutation Target for VSTM Domain: Energy Substrate.</p>
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class VesselEnergyView {

    /**
     * Default value returned when energy stored cannot be determined or target is null.
     */
    public static final long DEFAULT_ENERGY_STORED_EMPTY = 0L;

    /**
     * Default maximum capacity returned when capacity cannot be determined or target is null.
     */
    public static final long DEFAULT_MAX_CAPACITY_UNKNOWN = 0L;

    /**
     * Constructs a new VesselEnergyView.
     */
    public VesselEnergyView() {
    }

    /**
     * Retrieves the current stored energy from the specified target container.
     *
     * @param target energy storage target
     * @return current energy stored in energy units (FE / TR)
     */
    public static long getEnergyStored(Object target) {
        if (target == null) {
            return DEFAULT_ENERGY_STORED_EMPTY;
        }
        if (target instanceof ZeroAllocationEnergyStorage storage) {
            return storage.getEnergyStored();
        }
        if (target instanceof SimpleEnergyContainer container) {
            return container.getEnergyStored();
        }
        if (target instanceof AtomicLong atomic) {
            return atomic.get();
        }
        if (target instanceof Number num) {
            return num.longValue();
        }
        return DEFAULT_ENERGY_STORED_EMPTY;
    }

    /**
     * Retrieves the maximum energy capacity of the specified target container.
     *
     * @param target energy storage target
     * @return maximum energy capacity in energy units (FE / TR)
     */
    public static long getMaxEnergyStored(Object target) {
        if (target == null) {
            return DEFAULT_MAX_CAPACITY_UNKNOWN;
        }
        if (target instanceof ZeroAllocationEnergyStorage storage) {
            return storage.getMaxEnergyStored();
        }
        if (target instanceof SimpleEnergyContainer container) {
            return container.getMaxEnergyStored();
        }
        if (target instanceof Number num) {
            return num.longValue();
        }
        return DEFAULT_MAX_CAPACITY_UNKNOWN;
    }

    /**
     * Determines whether energy can be extracted from the target container.
     *
     * @param target energy storage target
     * @return {@code true} if energy can be extracted
     */
    public static boolean canExtract(Object target) {
        if (target == null) {
            return false;
        }
        if (target instanceof ZeroAllocationEnergyStorage storage) {
            return storage.canExtract();
        }
        if (target instanceof SimpleEnergyContainer container) {
            return container.canExtract();
        }
        if (target instanceof AtomicLong atomic) {
            return atomic.get() > 0L;
        }
        if (target instanceof Number num) {
            return num.longValue() > 0L;
        }
        return false;
    }

    /**
     * Determines whether energy can be inserted or received by the target container.
     *
     * @param target energy storage target
     * @return {@code true} if energy can be received
     */
    public static boolean canReceive(Object target) {
        if (target == null) {
            return false;
        }
        if (target instanceof ZeroAllocationEnergyStorage storage) {
            return storage.canReceive();
        }
        if (target instanceof SimpleEnergyContainer container) {
            return container.canReceive();
        }
        if (target instanceof AtomicLong) {
            return true;
        }
        return false;
    }
}
