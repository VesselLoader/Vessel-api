package dev.vessel.api.energy;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Ultra-high-performance, lock-free standalone energy storage container backed by 64-bit
 * primitive {@link AtomicLong} compare-and-swap (CAS) loops, guaranteeing zero heap allocations
 * during steady-state multi-threaded energy transfer.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ZeroAllocationEnergyStorage {

    /**
     * Default maximum energy transfer rate applied when unrestricted.
     */
    public static final long DEFAULT_UNRESTRICTED_TRANSFER_RATE = Long.MAX_VALUE;

    /**
     * Default initial energy balance.
     */
    public static final long DEFAULT_INITIAL_ENERGY = 0L;

    private final long capacity;
    private final long maxReceive;
    private final long maxExtract;
    private final AtomicLong energy;

    /**
     * Constructs a storage container with uniform capacity and unrestricted transfer rates.
     *
     * @param capacity maximum energy capacity
     */
    public ZeroAllocationEnergyStorage(long capacity) {
        this(capacity, DEFAULT_UNRESTRICTED_TRANSFER_RATE, DEFAULT_UNRESTRICTED_TRANSFER_RATE, DEFAULT_INITIAL_ENERGY);
    }

    /**
     * Constructs a storage container with custom transfer rate limits.
     *
     * @param capacity   maximum energy capacity
     * @param maxReceive maximum rate of energy receipt per operation
     * @param maxExtract maximum rate of energy extraction per operation
     */
    public ZeroAllocationEnergyStorage(long capacity, long maxReceive, long maxExtract) {
        this(capacity, maxReceive, maxExtract, DEFAULT_INITIAL_ENERGY);
    }

    /**
     * Constructs a storage container with custom limits and an initial energy amount.
     *
     * @param capacity      maximum energy capacity
     * @param maxReceive    maximum rate of energy receipt
     * @param maxExtract    maximum rate of energy extraction
     * @param initialEnergy starting energy level
     */
    public ZeroAllocationEnergyStorage(long capacity, long maxReceive, long maxExtract, long initialEnergy) {
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
        long clampedInitial = Math.clamp(initialEnergy, 0L, capacity);
        this.energy = new AtomicLong(clampedInitial);
    }

    /**
     * Returns the current energy level stored in the container.
     *
     * @return current energy
     */
    public long getEnergyStored() {
        return energy.get();
    }

    /**
     * Returns the maximum energy capacity of the container.
     *
     * @return maximum capacity
     */
    public long getMaxEnergyStored() {
        return capacity;
    }

    /**
     * Returns the maximum rate of energy that can be received in a single operation.
     *
     * @return maximum receive rate
     */
    public long getMaxReceive() {
        return maxReceive;
    }

    /**
     * Returns the maximum rate of energy that can be extracted in a single operation.
     *
     * @return maximum extract rate
     */
    public long getMaxExtract() {
        return maxExtract;
    }

    /**
     * Returns whether this container permits receiving energy.
     *
     * @return {@code true} if receive rate is positive and capacity remains
     */
    public boolean canReceive() {
        return maxReceive > 0L && energy.get() < capacity;
    }

    /**
     * Returns whether this container permits extracting energy.
     *
     * @return {@code true} if extract rate is positive and energy exists
     */
    public boolean canExtract() {
        return maxExtract > 0L && energy.get() > 0L;
    }

    /**
     * Receives energy into the storage container using an atomic CAS loop (zero heap allocations).
     *
     * @param offeredAmount amount of energy offered
     * @param simulate      {@code true} to simulate without mutating state
     * @return actual amount of energy accepted
     */
    public long receiveEnergy(long offeredAmount, boolean simulate) {
        if (offeredAmount <= 0L || maxReceive <= 0L) {
            return 0L;
        }

        long allowedRate = Math.min(offeredAmount, maxReceive);

        while (true) {
            long current = energy.get();
            long space = Math.max(0L, capacity - current);
            long toInsert = Math.min(allowedRate, space);

            if (toInsert <= 0L) {
                return 0L;
            }

            if (simulate) {
                return toInsert;
            }

            long next = current + toInsert;
            if (energy.compareAndSet(current, next)) {
                return toInsert;
            }
        }
    }

    /**
     * Extracts energy from the storage container using an atomic CAS loop (zero heap allocations).
     *
     * @param requestedAmount amount of energy requested
     * @param simulate        {@code true} to simulate without mutating state
     * @return actual amount of energy extracted
     */
    public long extractEnergy(long requestedAmount, boolean simulate) {
        if (requestedAmount <= 0L || maxExtract <= 0L) {
            return 0L;
        }

        long allowedRate = Math.min(requestedAmount, maxExtract);

        while (true) {
            long current = energy.get();
            long toExtract = Math.min(allowedRate, current);

            if (toExtract <= 0L) {
                return 0L;
            }

            if (simulate) {
                return toExtract;
            }

            long next = current - toExtract;
            if (energy.compareAndSet(current, next)) {
                return toExtract;
            }
        }
    }

    /**
     * Overwrites the stored energy directly, clamping to valid capacity bounds.
     *
     * @param newEnergy new energy balance
     */
    public void setEnergyStored(long newEnergy) {
        long clamped = Math.clamp(newEnergy, 0L, capacity);
        energy.set(clamped);
    }

    /**
     * Resets the energy balance to zero.
     */
    public void reset() {
        energy.set(0L);
    }
}
