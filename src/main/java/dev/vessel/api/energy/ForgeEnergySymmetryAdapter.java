package dev.vessel.api.energy;

/**
 * Bytecode opcode receptor and symmetry adapter transmuting Forge/NeoForge {@code IEnergyStorage}
 * calls (which use 32-bit signed integers) directly into VESSEL's 64-bit primitive Super-API
 * {@link EnergyTransferBridge} without object allocations or proxy wrappers.
 *
 * <p>Transmutation Target for VSTM Domain: Forge Energy Symmetry.</p>
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ForgeEnergySymmetryAdapter {

    /**
     * Constructs a new ForgeEnergySymmetryAdapter.
     */
    public ForgeEnergySymmetryAdapter() {
    }

    /**
     * Transmutation receptor for {@code IEnergyStorage.receiveEnergy(int maxReceive, boolean simulate)}.
     *
     * @param target     energy storage target
     * @param maxReceive maximum energy offered (32-bit signed int)
     * @param simulate   {@code true} to simulate without state mutation
     * @return energy received clamped to integer bounds
     */
    public static int receiveEnergy(Object target, int maxReceive, boolean simulate) {
        if (maxReceive <= 0) {
            return 0;
        }
        long received = EnergyTransferBridge.receiveEnergy(target, (long) maxReceive, simulate);
        return (int) Math.min((long) Integer.MAX_VALUE, received);
    }

    /**
     * Transmutation receptor for {@code IEnergyStorage.extractEnergy(int maxExtract, boolean simulate)}.
     *
     * @param target     energy storage target
     * @param maxExtract maximum energy requested (32-bit signed int)
     * @param simulate   {@code true} to simulate without state mutation
     * @return energy extracted clamped to integer bounds
     */
    public static int extractEnergy(Object target, int maxExtract, boolean simulate) {
        if (maxExtract <= 0) {
            return 0;
        }
        long extracted = EnergyTransferBridge.extractEnergy(target, (long) maxExtract, simulate);
        return (int) Math.min((long) Integer.MAX_VALUE, extracted);
    }

    /**
     * Transmutation receptor for {@code IEnergyStorage.getEnergyStored()}.
     *
     * @param target energy storage target
     * @return energy stored clamped to integer bounds
     */
    public static int getEnergyStored(Object target) {
        long stored = VesselEnergyView.getEnergyStored(target);
        return (int) Math.min((long) Integer.MAX_VALUE, stored);
    }

    /**
     * Transmutation receptor for {@code IEnergyStorage.getMaxEnergyStored()}.
     *
     * @param target energy storage target
     * @return maximum capacity clamped to integer bounds
     */
    public static int getMaxEnergyStored(Object target) {
        long capacity = VesselEnergyView.getMaxEnergyStored(target);
        return (int) Math.min((long) Integer.MAX_VALUE, capacity);
    }

    /**
     * Transmutation receptor for {@code IEnergyStorage.canExtract()}.
     *
     * @param target energy storage target
     * @return {@code true} if extraction is supported
     */
    public static boolean canExtract(Object target) {
        return VesselEnergyView.canExtract(target);
    }

    /**
     * Transmutation receptor for {@code IEnergyStorage.canReceive()}.
     *
     * @param target energy storage target
     * @return {@code true} if receiving energy is supported
     */
    public static boolean canReceive(Object target) {
        return VesselEnergyView.canReceive(target);
    }
}
