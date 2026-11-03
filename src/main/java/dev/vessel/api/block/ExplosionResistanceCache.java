package dev.vessel.api.block;

import java.util.Arrays;

/**
 * Dense flat float array cache indexing block blast and explosion resistance values.
 * Directly indexed by numerical block ID for sub-nanosecond L1 CPU cache access during
 * intense TNT explosions, Creeper blasts, and Wither projectiles.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ExplosionResistanceCache {

    public static final int DEFAULT_CAPACITY = 4096;

    public static final float RESISTANCE_AIR = 0.0f;
    public static final float RESISTANCE_DIRT = 0.5f;
    public static final float RESISTANCE_WOOD = 3.0f;
    public static final float RESISTANCE_STONE = 6.0f;
    public static final float RESISTANCE_WATER = 100.0f;
    public static final float RESISTANCE_OBSIDIAN = 1200.0f;
    public static final float RESISTANCE_BEDROCK = 3_600_000.0f;

    private final float[] resistance;

    /**
     * Constructs an explosion resistance cache with default 4096-entry capacity and standard profiles.
     */
    public ExplosionResistanceCache() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * Constructs an explosion resistance cache with custom capacity.
     *
     * @param capacity maximum block ID range
     */
    public ExplosionResistanceCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive: " + capacity);
        }
        this.resistance = new float[capacity];
        Arrays.fill(this.resistance, RESISTANCE_AIR);
        initializeStandardBlastResistances();
    }

    private void initializeStandardBlastResistances() {
        // Air
        setExplosionResistance(0, RESISTANCE_AIR);

        // Stone & Cobblestone
        setExplosionResistance(1, RESISTANCE_STONE);
        setExplosionResistance(4, RESISTANCE_STONE);

        // Dirt
        setExplosionResistance(3, RESISTANCE_DIRT);

        // Wood Planks
        setExplosionResistance(5, RESISTANCE_WOOD);

        // Bedrock
        setExplosionResistance(7, RESISTANCE_BEDROCK);

        // Liquids (Water & Lava absorb blast)
        setExplosionResistance(8, RESISTANCE_WATER);
        setExplosionResistance(9, RESISTANCE_WATER);
        setExplosionResistance(10, RESISTANCE_WATER);
        setExplosionResistance(11, RESISTANCE_WATER);

        // Obsidian & Anvil
        setExplosionResistance(49, RESISTANCE_OBSIDIAN);
        setExplosionResistance(145, RESISTANCE_OBSIDIAN);
    }

    /**
     * Directly sets the explosion resistance for a specific block ID.
     *
     * @param blockId numerical block ID
     * @param value explosion resistance float
     */
    public void setExplosionResistance(int blockId, float value) {
        if (blockId >= 0 && blockId < resistance.length) {
            resistance[blockId] = Math.max(0.0f, value);
        }
    }

    /**
     * Sub-nanosecond L1 CPU cache primitive lookup for explosion resistance.
     *
     * @param blockId numerical block ID
     * @return explosion resistance value in float
     */
    public float getExplosionResistance(int blockId) {
        if (blockId >= 0 && blockId < resistance.length) {
            return resistance[blockId];
        }
        return RESISTANCE_AIR;
    }

    public int getCapacity() {
        return resistance.length;
    }
}
