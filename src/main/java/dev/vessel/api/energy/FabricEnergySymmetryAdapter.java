package dev.vessel.api.energy;

import dev.vessel.api.storage.CapabilityTransactionSimulator;

/**
 * Bytecode opcode receptor and symmetry adapter transmuting Fabric Energy (TeamReborn {@code EnergyStorage})
 * calls (which utilize 64-bit longs and nested {@code TransactionContext}) directly into VESSEL's Super-API
 * {@link EnergyTransferBridge} and {@link CapabilityTransactionSimulator}.
 *
 * <p>Transmutation Target for VSTM Domain: Fabric Energy Symmetry.</p>
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class FabricEnergySymmetryAdapter {

    /**
     * Default slot index used when tracking energy transactions in the rollback simulator.
     */
    public static final int ENERGY_TRANSACTION_SLOT_INDEX = 0;

    /**
     * Constructs a new FabricEnergySymmetryAdapter.
     */
    public FabricEnergySymmetryAdapter() {
    }

    /**
     * Transmutation receptor for {@code EnergyStorage.insert(long maxAmount, TransactionContext transaction)}.
     *
     * @param target             energy storage target
     * @param maxAmount          maximum energy offered (64-bit long)
     * @param transactionContext transaction context or simulator
     * @return energy accepted in Fabric energy units
     */
    public static long insert(Object target, long maxAmount, Object transactionContext) {
        if (target == null || maxAmount <= 0L) {
            return 0L;
        }

        if (transactionContext instanceof CapabilityTransactionSimulator sim) {
            long current = VesselEnergyView.getEnergyStored(target);
            long accepted = EnergyTransferBridge.receiveEnergy(target, maxAmount, false);
            if (accepted > 0L) {
                sim.recordSnapshot(ENERGY_TRANSACTION_SLOT_INDEX, current);
            }
            return accepted;
        }

        return EnergyTransferBridge.receiveEnergy(target, maxAmount, false);
    }

    /**
     * Transmutation receptor for {@code EnergyStorage.extract(long maxAmount, TransactionContext transaction)}.
     *
     * @param target             energy storage target
     * @param maxAmount          maximum energy requested (64-bit long)
     * @param transactionContext transaction context or simulator
     * @return energy extracted in Fabric energy units
     */
    public static long extract(Object target, long maxAmount, Object transactionContext) {
        if (target == null || maxAmount <= 0L) {
            return 0L;
        }

        if (transactionContext instanceof CapabilityTransactionSimulator sim) {
            long current = VesselEnergyView.getEnergyStored(target);
            long extracted = EnergyTransferBridge.extractEnergy(target, maxAmount, false);
            if (extracted > 0L) {
                sim.recordSnapshot(ENERGY_TRANSACTION_SLOT_INDEX, current);
            }
            return extracted;
        }

        return EnergyTransferBridge.extractEnergy(target, maxAmount, false);
    }

    /**
     * Transmutation receptor for {@code EnergyStorage.getAmount()}.
     *
     * @param target energy storage target
     * @return current energy stored
     */
    public static long getAmount(Object target) {
        return VesselEnergyView.getEnergyStored(target);
    }

    /**
     * Transmutation receptor for {@code EnergyStorage.getCapacity()}.
     *
     * @param target energy storage target
     * @return maximum energy capacity
     */
    public static long getCapacity(Object target) {
        return VesselEnergyView.getMaxEnergyStored(target);
    }

    /**
     * Transmutation receptor for {@code EnergyStorage.supportsInsertion()}.
     *
     * @param target energy storage target
     * @return {@code true} if insertion is supported
     */
    public static boolean supportsInsertion(Object target) {
        return VesselEnergyView.canReceive(target);
    }

    /**
     * Transmutation receptor for {@code EnergyStorage.supportsExtraction()}.
     *
     * @param target energy storage target
     * @return {@code true} if extraction is supported
     */
    public static boolean supportsExtraction(Object target) {
        return VesselEnergyView.canExtract(target);
    }
}
