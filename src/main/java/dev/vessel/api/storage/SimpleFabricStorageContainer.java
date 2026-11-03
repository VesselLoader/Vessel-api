package dev.vessel.api.storage;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Clean-room in-memory storage container implementing Fabric Transfer API semantics
 * for unit testing and native runtime execution without requiring external Fabric binaries.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class SimpleFabricStorageContainer {

    private final String resourceId;
    private final long capacity;
    private long currentAmount;
    private final AtomicLong version = new AtomicLong(0);
    private boolean supportsInsertion = true;
    private boolean supportsExtraction = true;

    /**
     * Constructs a storage container with the specified resource and maximum capacity.
     *
     * @param resourceId resource identifier (e.g. "minecraft:water", "minecraft:iron_ingot")
     * @param capacity   maximum capacity in units or droplets
     */
    public SimpleFabricStorageContainer(String resourceId, long capacity) {
        this(resourceId, capacity, 0L);
    }

    /**
     * Constructs a storage container with initial contents.
     *
     * @param resourceId    resource identifier
     * @param capacity      maximum capacity
     * @param initialAmount starting amount
     */
    public SimpleFabricStorageContainer(String resourceId, long capacity, long initialAmount) {
        this.resourceId = Objects.requireNonNull(resourceId, "resourceId cannot be null");
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive: " + capacity);
        }
        this.capacity = capacity;
        this.currentAmount = Math.min(Math.max(0L, initialAmount), capacity);
    }

    public String getResourceId() {
        return resourceId;
    }

    public long getCapacity() {
        return capacity;
    }

    public long getCurrentAmount() {
        return currentAmount;
    }

    public long getVersion() {
        return version.get();
    }

    public boolean supportsInsertion() {
        return supportsInsertion;
    }

    public void setSupportsInsertion(boolean supportsInsertion) {
        this.supportsInsertion = supportsInsertion;
    }

    public boolean supportsExtraction() {
        return supportsExtraction;
    }

    public void setSupportsExtraction(boolean supportsExtraction) {
        this.supportsExtraction = supportsExtraction;
    }

    /**
     * Inserts an amount of the matching resource under the given transaction.
     *
     * @param resource target resource identifier
     * @param maxAmount maximum quantity to insert
     * @param transaction optional transaction context
     * @return accepted amount
     */
    public long insert(String resource, long maxAmount, FabricTransactionContext transaction) {
        if (!supportsInsertion || maxAmount <= 0 || !resourceId.equals(resource)) {
            return 0L;
        }
        long space = capacity - currentAmount;
        long accepted = Math.min(maxAmount, space);
        if (accepted <= 0) {
            return 0L;
        }

        if (transaction != null) {
            long previousAmount = this.currentAmount;
            this.currentAmount += accepted;
            this.version.incrementAndGet();
            transaction.addRollbackAction(() -> {
                this.currentAmount = previousAmount;
                this.version.incrementAndGet();
            });
        } else {
            this.currentAmount += accepted;
            this.version.incrementAndGet();
        }

        return accepted;
    }

    /**
     * Extracts an amount of the matching resource under the given transaction.
     *
     * @param resource target resource identifier
     * @param maxAmount maximum quantity to extract
     * @param transaction optional transaction context
     * @return extracted amount
     */
    public long extract(String resource, long maxAmount, FabricTransactionContext transaction) {
        if (!supportsExtraction || maxAmount <= 0 || !resourceId.equals(resource)) {
            return 0L;
        }
        long available = Math.min(maxAmount, currentAmount);
        if (available <= 0) {
            return 0L;
        }

        if (transaction != null) {
            long previousAmount = this.currentAmount;
            this.currentAmount -= available;
            this.version.incrementAndGet();
            transaction.addRollbackAction(() -> {
                this.currentAmount = previousAmount;
                this.version.incrementAndGet();
            });
        } else {
            this.currentAmount -= available;
            this.version.incrementAndGet();
        }

        return available;
    }
}
