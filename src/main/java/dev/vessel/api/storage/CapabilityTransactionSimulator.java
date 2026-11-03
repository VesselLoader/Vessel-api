package dev.vessel.api.storage;

import java.util.Arrays;

/**
 * High-performance, zero-allocation multi-tier transaction rollback simulator utilizing flat
 * primitive array journals, mirroring Fabric's {@code TransactionContext} mechanics over
 * Forge/NeoForge simulated inventory actions without heap allocations.
 *
 * <p>Transmutation Target for VSTM Domain 1 (Item &amp; Fluid Storage Symmetry).</p>
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class CapabilityTransactionSimulator {

    /**
     * Default initial capacity for the tracked slot balances array.
     */
    public static final int DEFAULT_INITIAL_SLOT_CAPACITY = 128;

    /**
     * Default initial capacity for the flat snapshot rollback journal.
     */
    public static final int DEFAULT_INITIAL_SNAPSHOT_CAPACITY = 256;

    /**
     * Default initial capacity for transaction nesting depth pointers.
     */
    public static final int DEFAULT_INITIAL_MAX_DEPTH = 32;

    private int currentDepth;
    private int snapshotCount;

    private long[] slotBalances;
    private int[] depthSnapshotPointers;
    private int[] snapshotSlots;
    private long[] snapshotValues;

    /**
     * Constructs a simulator with default initial capacities.
     */
    public CapabilityTransactionSimulator() {
        this(DEFAULT_INITIAL_SLOT_CAPACITY, DEFAULT_INITIAL_SNAPSHOT_CAPACITY, DEFAULT_INITIAL_MAX_DEPTH);
    }

    /**
     * Constructs a simulator with configurable capacities.
     *
     * @param initialSlotCapacity     initial slot array capacity
     * @param initialSnapshotCapacity initial flat snapshot journal capacity
     * @param initialMaxDepth         initial transaction nesting depth capacity
     */
    public CapabilityTransactionSimulator(int initialSlotCapacity, int initialSnapshotCapacity, int initialMaxDepth) {
        if (initialSlotCapacity <= 0 || initialSnapshotCapacity <= 0 || initialMaxDepth <= 0) {
            throw new IllegalArgumentException("Capacities must be strictly positive");
        }
        this.currentDepth = 0;
        this.snapshotCount = 0;
        this.slotBalances = new long[initialSlotCapacity];
        this.snapshotSlots = new int[initialSnapshotCapacity];
        this.snapshotValues = new long[initialSnapshotCapacity];
        this.depthSnapshotPointers = new int[initialMaxDepth];
    }

    /**
     * Opens a new nested transaction scope.
     *
     * @return current 1-based transaction depth
     */
    public int beginNestedTransaction() {
        currentDepth++;
        if (currentDepth >= depthSnapshotPointers.length) {
            depthSnapshotPointers = Arrays.copyOf(depthSnapshotPointers, depthSnapshotPointers.length * 2);
        }
        depthSnapshotPointers[currentDepth] = snapshotCount;
        return currentDepth;
    }

    /**
     * Records a slot mutation, automatically capturing the previous balance in the flat snapshot journal
     * if within an active transaction.
     *
     * @param slotIndex slot index to mutate
     * @param newAmount new balance for the slot
     */
    public void recordMutation(int slotIndex, long newAmount) {
        if (slotIndex < 0) {
            throw new IllegalArgumentException("slotIndex cannot be negative: " + slotIndex);
        }
        ensureSlotCapacity(slotIndex + 1);

        if (currentDepth > 0) {
            long currentVal = slotBalances[slotIndex];
            recordSnapshot(slotIndex, currentVal);
        }
        slotBalances[slotIndex] = newAmount;
    }

    /**
     * Explicitly records a snapshot entry in the flat array journal.
     *
     * @param slotIndex      slot index being captured
     * @param previousAmount previous amount before mutation
     */
    public void recordSnapshot(int slotIndex, long previousAmount) {
        if (snapshotCount >= snapshotSlots.length) {
            int newCap = snapshotSlots.length * 2;
            snapshotSlots = Arrays.copyOf(snapshotSlots, newCap);
            snapshotValues = Arrays.copyOf(snapshotValues, newCap);
        }
        snapshotSlots[snapshotCount] = slotIndex;
        snapshotValues[snapshotCount] = previousAmount;
        snapshotCount++;
    }

    /**
     * Commits the innermost active transaction level to its parent or finalizes at root.
     *
     * @return depth that was committed
     */
    public int commitTransaction() {
        if (currentDepth <= 0) {
            throw new IllegalStateException("No active transaction to commit");
        }
        int committedDepth = currentDepth;
        if (currentDepth == 1) {
            snapshotCount = 0;
        }
        currentDepth--;
        return committedDepth;
    }

    /**
     * Aborts and rolls back all slot mutations performed in the current nested transaction level.
     *
     * @return number of slot entries restored
     */
    public int abortAndRollbackTransaction() {
        if (currentDepth <= 0) {
            throw new IllegalStateException("No active transaction to abort");
        }
        int startIndex = depthSnapshotPointers[currentDepth];
        int restoredCount = 0;

        for (int i = snapshotCount - 1; i >= startIndex; i--) {
            int slot = snapshotSlots[i];
            long prevValue = snapshotValues[i];
            slotBalances[slot] = prevValue;
            restoredCount++;
        }

        snapshotCount = startIndex;
        currentDepth--;
        return restoredCount;
    }

    /**
     * Returns the current balance of the given slot.
     *
     * @param slotIndex slot index
     * @return balance
     */
    public long getSlotAmount(int slotIndex) {
        if (slotIndex < 0) {
            throw new IllegalArgumentException("slotIndex cannot be negative: " + slotIndex);
        }
        if (slotIndex >= slotBalances.length) {
            return 0L;
        }
        return slotBalances[slotIndex];
    }

    /**
     * Sets the balance of the given slot directly.
     *
     * @param slotIndex slot index
     * @param amount    balance amount
     */
    public void setSlotAmount(int slotIndex, long amount) {
        if (slotIndex < 0) {
            throw new IllegalArgumentException("slotIndex cannot be negative: " + slotIndex);
        }
        ensureSlotCapacity(slotIndex + 1);
        slotBalances[slotIndex] = amount;
    }

    /**
     * Returns the total number of tracked slots.
     *
     * @return slot count
     */
    public int getSlotCount() {
        return slotBalances.length;
    }

    /**
     * Returns the active nesting depth (0 = root / outside transaction).
     *
     * @return nesting depth
     */
    public int getCurrentDepth() {
        return currentDepth;
    }

    /**
     * Checks if a transaction is currently active.
     *
     * @return true if depth > 0
     */
    public boolean isInTransaction() {
        return currentDepth > 0;
    }

    /**
     * Returns the total count of recorded snapshots in the flat journal.
     *
     * @return snapshot count
     */
    public int getSnapshotCount() {
        return snapshotCount;
    }

    /**
     * Resets the simulator, clearing all transactions and snapshots.
     */
    public void reset() {
        this.currentDepth = 0;
        this.snapshotCount = 0;
        Arrays.fill(this.slotBalances, 0L);
    }

    private void ensureSlotCapacity(int minCapacity) {
        if (minCapacity > slotBalances.length) {
            int newCap = Math.max(slotBalances.length * 2, minCapacity);
            slotBalances = Arrays.copyOf(slotBalances, newCap);
        }
    }
}
