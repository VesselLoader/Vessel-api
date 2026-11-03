package dev.vessel.api.energy;

/**
 * Lightweight, mutable in-memory energy container designed for machine simulation, tile entity
 * buffers, and deterministic testing without heap allocation during energy transactions.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class SimpleEnergyContainer {

    /**
     * Default maximum energy transfer limit.
     */
    public static final long DEFAULT_TRANSFER_LIMIT = Long.MAX_VALUE;

    private final long capacity;
    private final long maxReceive;
    private final long maxExtract;
    private long energyStored;

    /**
     * Constructs a container with specified capacity and unrestricted transfer limits.
     *
     * @param capacity maximum capacity
     */
    public SimpleEnergyContainer(long capacity) {
        this(capacity, DEFAULT_TRANSFER_LIMIT, DEFAULT_TRANSFER_LIMIT, 0L);
    }

    /**
     * Constructs a container with custom capacity and transfer limits.
     *
     * @param capacity   maximum capacity
     * @param maxReceive max receive rate
     * @param maxExtract max extract rate
     */
    public SimpleEnergyContainer(long capacity, long maxReceive, long maxExtract) {
        this(capacity, maxReceive, maxExtract, 0L);
    }

    /**
     * Constructs a container with custom capacity, transfer limits, and initial energy.
     *
     * @param capacity      maximum capacity
     * @param maxReceive    max receive rate
     * @param maxExtract    max extract rate
     * @param initialEnergy initial energy stored
     */
    public SimpleEnergyContainer(long capacity, long maxReceive, long maxExtract, long initialEnergy) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity cannot be negative: " + capacity);
        }
        if (maxReceive < 0) {
            throw new IllegalArgumentException("maxReceive cannot be negative: " + maxReceive);
        }
        if (maxExtract < 0) {
            throw new IllegalArgumentException("maxExtract cannot be negative: " + maxExtract);
        }
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
        this.energyStored = Math.clamp(initialEnergy, 0L, capacity);
    }

    /**
     * Returns the current energy stored.
     *
     * @return energy stored
     */
    public synchronized long getEnergyStored() {
        return energyStored;
    }

    /**
     * Returns the maximum energy capacity.
     *
     * @return maximum capacity
     */
    public long getMaxEnergyStored() {
        return capacity;
    }

    /**
     * Returns the maximum receive rate.
     *
     * @return max receive
     */
    public long getMaxReceive() {
        return maxReceive;
    }

    /**
     * Returns the maximum extract rate.
     *
     * @return max extract
     */
    public long getMaxExtract() {
        return maxExtract;
    }

    /**
     * Returns whether the container can currently receive energy.
     *
     * @return {@code true} if receive is allowed
     */
    public synchronized boolean canReceive() {
        return maxReceive > 0L && energyStored < capacity;
    }

    /**
     * Returns whether the container can currently extract energy.
     *
     * @return {@code true} if extract is allowed
     */
    public synchronized boolean canExtract() {
        return maxExtract > 0L && energyStored > 0L;
    }

    /**
     * Receives energy into the container.
     *
     * @param amount   amount offered
     * @param simulate {@code true} to simulate without state mutation
     * @return amount accepted
     */
    public synchronized long receiveEnergy(long amount, boolean simulate) {
        if (amount <= 0L || maxReceive <= 0L) {
            return 0L;
        }
        long rate = Math.min(amount, maxReceive);
        long space = Math.max(0L, capacity - energyStored);
        long accepted = Math.min(rate, space);
        if (!simulate && accepted > 0L) {
            energyStored += accepted;
        }
        return accepted;
    }

    /**
     * Extracts energy from the container.
     *
     * @param amount   amount requested
     * @param simulate {@code true} to simulate without state mutation
     * @return amount extracted
     */
    public synchronized long extractEnergy(long amount, boolean simulate) {
        if (amount <= 0L || maxExtract <= 0L) {
            return 0L;
        }
        long rate = Math.min(amount, maxExtract);
        long extracted = Math.min(rate, energyStored);
        if (!simulate && extracted > 0L) {
            energyStored -= extracted;
        }
        return extracted;
    }

    /**
     * Sets the energy balance directly within bounds.
     *
     * @param newEnergy new energy balance
     */
    public synchronized void setEnergyStored(long newEnergy) {
        this.energyStored = Math.clamp(newEnergy, 0L, capacity);
    }

    /**
     * Resets the stored energy to zero.
     */
    public synchronized void reset() {
        this.energyStored = 0L;
    }
}
