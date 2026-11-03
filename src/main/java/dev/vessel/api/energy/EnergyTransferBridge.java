package dev.vessel.api.energy;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Universal two-way primitive energy transfer bridge translating transactions symmetrically
 * between Forge Energy (FE/RF) and Fabric Energy (TechReborn/TeamReborn Energy) without heap allocations.
 *
 * <p>Symmetrical ratio: 1 Forge Energy (FE) == 1 Fabric Energy unit (TR E).</p>
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class EnergyTransferBridge {

    /**
     * Exact 1:1 symmetrical conversion ratio between Forge Energy (FE) and Fabric Energy (TR).
     */
    public static final double FORGE_ENERGY_TO_FABRIC_ENERGY_RATIO = 1.0;

    /**
     * Default maximum energy transfer limit per tick or transfer cycle.
     */
    public static final long DEFAULT_MAX_ENERGY_TRANSFER = Long.MAX_VALUE;

    /**
     * Constructs a new EnergyTransferBridge.
     */
    public EnergyTransferBridge() {
    }

    /**
     * Receives energy into a target container.
     *
     * @param target     energy storage target
     * @param maxReceive maximum amount of energy offered
     * @param simulate   {@code true} to simulate without state mutation
     * @return actual amount of energy accepted
     */
    public static long receiveEnergy(Object target, long maxReceive, boolean simulate) {
        if (target == null || maxReceive <= 0L) {
            return 0L;
        }
        if (target instanceof ZeroAllocationEnergyStorage storage) {
            return storage.receiveEnergy(maxReceive, simulate);
        }
        if (target instanceof SimpleEnergyContainer container) {
            return container.receiveEnergy(maxReceive, simulate);
        }
        if (target instanceof AtomicLong atomic) {
            if (simulate) {
                return maxReceive;
            }
            return atomic.addAndGet(maxReceive);
        }
        return 0L;
    }

    /**
     * Extracts energy from a target container.
     *
     * @param target     energy storage target
     * @param maxExtract maximum amount of energy requested
     * @param simulate   {@code true} to simulate without state mutation
     * @return actual amount of energy extracted
     */
    public static long extractEnergy(Object target, long maxExtract, boolean simulate) {
        if (target == null || maxExtract <= 0L) {
            return 0L;
        }
        if (target instanceof ZeroAllocationEnergyStorage storage) {
            return storage.extractEnergy(maxExtract, simulate);
        }
        if (target instanceof SimpleEnergyContainer container) {
            return container.extractEnergy(maxExtract, simulate);
        }
        if (target instanceof AtomicLong atomic) {
            while (true) {
                long current = atomic.get();
                long toExtract = Math.min(maxExtract, current);
                if (toExtract <= 0L) {
                    return 0L;
                }
                if (simulate) {
                    return toExtract;
                }
                if (atomic.compareAndSet(current, current - toExtract)) {
                    return toExtract;
                }
            }
        }
        return 0L;
    }

    /**
     * Performs a direct point-to-point energy transfer from a source storage to a destination storage.
     *
     * @param source      source energy container
     * @param destination destination energy container
     * @param maxAmount   maximum transfer amount
     * @return actual amount of energy moved from source to destination
     */
    public static long transferEnergy(Object source, Object destination, long maxAmount) {
        if (source == null || destination == null || maxAmount <= 0L) {
            return 0L;
        }

        // Phase 1: Simulate extraction from source
        long simulatedExtract = extractEnergy(source, maxAmount, true);
        if (simulatedExtract <= 0L) {
            return 0L;
        }

        // Phase 2: Simulate reception in destination
        long simulatedReceive = receiveEnergy(destination, simulatedExtract, true);
        if (simulatedReceive <= 0L) {
            return 0L;
        }

        // Phase 3: Execute actual extraction of accepted amount from source
        long actualExtracted = extractEnergy(source, simulatedReceive, false);
        if (actualExtracted <= 0L) {
            return 0L;
        }

        // Phase 4: Execute actual insertion into destination
        long actualReceived = receiveEnergy(destination, actualExtracted, false);

        // Safety compensation: if destination accepted less than extracted, refund difference
        if (actualReceived < actualExtracted) {
            long refund = actualExtracted - actualReceived;
            receiveEnergy(source, refund, false);
        }

        return actualReceived;
    }

    /**
     * Converts Forge Energy units (FE) to Fabric Energy units (TR E).
     *
     * @param forgeEnergy FE amount
     * @return equivalent Fabric energy units
     */
    public static long forgeToFabricEnergy(long forgeEnergy) {
        return Math.max(0L, forgeEnergy);
    }

    /**
     * Converts Fabric Energy units (TR E) to Forge Energy units (FE).
     *
     * @param fabricEnergy TR energy amount
     * @return equivalent Forge energy units
     */
    public static long fabricToForgeEnergy(long fabricEnergy) {
        return Math.max(0L, fabricEnergy);
    }
}
