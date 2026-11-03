package dev.vessel.api.block;

import java.util.Arrays;

/**
 * Dense flat array cache evaluating fire spread speed, combustibility, and flammability rates.
 * Mirrors Forge {@code IForgeBlock.getFlammability} and {@code IForgeBlock.getFireSpreadSpeed}
 * using direct L1-cache-friendly primitive indexing.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class FlammabilityEvaluationTable {

    public static final int DEFAULT_CAPACITY = 4096;

    public static final int FLAMMABILITY_WOOD = 20;
    public static final int SPREAD_WOOD = 5;

    public static final int FLAMMABILITY_LEAVES = 60;
    public static final int SPREAD_LEAVES = 30;

    public static final int FLAMMABILITY_WOOL = 60;
    public static final int SPREAD_WOOL = 30;

    public static final int FLAMMABILITY_TNT = 100;
    public static final int SPREAD_TNT = 15;

    private final int[] flammability;
    private final int[] fireSpreadSpeed;

    /**
     * Constructs a flammability table with default capacity and standard combustible profiles.
     */
    public FlammabilityEvaluationTable() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * Constructs a flammability table with custom capacity.
     *
     * @param capacity maximum block ID range
     */
    public FlammabilityEvaluationTable(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive: " + capacity);
        }
        this.flammability = new int[capacity];
        this.fireSpreadSpeed = new int[capacity];
        Arrays.fill(this.flammability, 0);
        Arrays.fill(this.fireSpreadSpeed, 0);
        initializeStandardFlammability();
    }

    private void initializeStandardFlammability() {
        // Wood Planks (ID 5) & Oak Log (ID 17)
        setFlammability(5, FLAMMABILITY_WOOD, SPREAD_WOOD);
        setFlammability(17, FLAMMABILITY_WOOD, SPREAD_WOOD);

        // Leaves (ID 18)
        setFlammability(18, FLAMMABILITY_LEAVES, SPREAD_LEAVES);

        // Wool (ID 35)
        setFlammability(35, FLAMMABILITY_WOOL, SPREAD_WOOL);

        // TNT (ID 46)
        setFlammability(46, FLAMMABILITY_TNT, SPREAD_TNT);
    }

    /**
     * Registers flammability and fire spread speed for a given block ID.
     *
     * @param blockId numerical block ID
     * @param flammabilityRate combustibility factor (0 = non-flammable)
     * @param spreadSpeed fire propagation speed
     */
    public void setFlammability(int blockId, int flammabilityRate, int spreadSpeed) {
        if (blockId >= 0 && blockId < flammability.length) {
            flammability[blockId] = Math.max(0, flammabilityRate);
            fireSpreadSpeed[blockId] = Math.max(0, spreadSpeed);
        }
    }

    /**
     * Checks if a block can catch fire.
     *
     * @param blockId numerical block ID
     * @return true if flammability > 0
     */
    public boolean isFlammable(int blockId) {
        if (blockId >= 0 && blockId < flammability.length) {
            return flammability[blockId] > 0;
        }
        return false;
    }

    /**
     * Retrieves the flammability rating for the block.
     *
     * @param blockId numerical block ID
     * @return flammability factor
     */
    public int getFlammability(int blockId) {
        if (blockId >= 0 && blockId < flammability.length) {
            return flammability[blockId];
        }
        return 0;
    }

    /**
     * Retrieves the fire spread speed for the block.
     *
     * @param blockId numerical block ID
     * @return fire spread speed
     */
    public int getFireSpreadSpeed(int blockId) {
        if (blockId >= 0 && blockId < fireSpreadSpeed.length) {
            return fireSpreadSpeed[blockId];
        }
        return 0;
    }
}
