package dev.vessel.api.block;

import java.util.Arrays;

/**
 * Redstone connectivity, weak power signal, and direct (strong) power query table.
 * Mirrors Forge {@code IForgeBlock.canConnectRedstone}, {@code IForgeBlock.getSignal},
 * and {@code IForgeBlock.getDirectSignal}.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class RedstoneConnectivityLookup {

    public static final int DEFAULT_CAPACITY = 4096;
    public static final int DIRECTION_COUNT = 6;

    public static final int DIRECTION_DOWN = 0;
    public static final int DIRECTION_UP = 1;
    public static final int DIRECTION_NORTH = 2;
    public static final int DIRECTION_SOUTH = 3;
    public static final int DIRECTION_WEST = 4;
    public static final int DIRECTION_EAST = 5;

    public static final int MAX_SIGNAL = 15;

    private final boolean[][] connectivity;
    private final byte[][] weakSignals;
    private final byte[][] directSignals;

    /**
     * Constructs a redstone connectivity lookup table with default capacity and standard components.
     */
    public RedstoneConnectivityLookup() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * Constructs a redstone connectivity lookup table with custom capacity.
     *
     * @param capacity maximum block ID range
     */
    public RedstoneConnectivityLookup(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive: " + capacity);
        }
        this.connectivity = new boolean[capacity][DIRECTION_COUNT];
        this.weakSignals = new byte[capacity][DIRECTION_COUNT];
        this.directSignals = new byte[capacity][DIRECTION_COUNT];
        initializeStandardRedstoneComponents();
    }

    private void initializeStandardRedstoneComponents() {
        // Redstone Wire (ID 55): connects horizontally, passes weak power
        for (int dir = DIRECTION_NORTH; dir <= DIRECTION_EAST; dir++) {
            setRedstoneProperties(55, dir, true, MAX_SIGNAL, 0);
        }

        // Redstone Torch (ID 76): strong power upwards, weak power sides
        setRedstoneProperties(76, DIRECTION_UP, true, MAX_SIGNAL, MAX_SIGNAL);
        for (int dir = DIRECTION_NORTH; dir <= DIRECTION_EAST; dir++) {
            setRedstoneProperties(76, dir, true, MAX_SIGNAL, 0);
        }

        // Redstone Block (ID 152): omnidirectional weak power
        setOmnidirectionalProperties(152, true, MAX_SIGNAL, 0);
    }

    /**
     * Configures redstone properties for a block in a specific direction.
     *
     * @param blockId numerical block ID
     * @param direction direction index (0..5)
     * @param canConnect whether wire/device connects
     * @param weakSignal weak emission level (0..15)
     * @param directSignal direct/strong emission level (0..15)
     */
    public void setRedstoneProperties(int blockId, int direction, boolean canConnect, int weakSignal, int directSignal) {
        if (blockId >= 0 && blockId < connectivity.length && direction >= 0 && direction < DIRECTION_COUNT) {
            connectivity[blockId][direction] = canConnect;
            weakSignals[blockId][direction] = (byte) Math.max(0, Math.min(MAX_SIGNAL, weakSignal));
            directSignals[blockId][direction] = (byte) Math.max(0, Math.min(MAX_SIGNAL, directSignal));
        }
    }

    /**
     * Configures omnidirectional redstone properties for a block across all 6 faces.
     *
     * @param blockId numerical block ID
     * @param canConnect whether wire/device connects
     * @param weakSignal weak emission level (0..15)
     * @param directSignal direct/strong emission level (0..15)
     */
    public void setOmnidirectionalProperties(int blockId, boolean canConnect, int weakSignal, int directSignal) {
        for (int dir = 0; dir < DIRECTION_COUNT; dir++) {
            setRedstoneProperties(blockId, dir, canConnect, weakSignal, directSignal);
        }
    }

    /**
     * Checks if redstone wire can connect to this block from the given direction.
     *
     * @param blockId numerical block ID
     * @param direction direction code (0..5)
     * @return true if connectable
     */
    public boolean canConnectRedstone(int blockId, int direction) {
        if (blockId >= 0 && blockId < connectivity.length && direction >= 0 && direction < DIRECTION_COUNT) {
            return connectivity[blockId][direction];
        }
        return false;
    }

    /**
     * Retrieves the weak redstone signal emitted by this block to the given direction.
     *
     * @param blockId numerical block ID
     * @param direction direction code (0..5)
     * @return signal strength (0..15)
     */
    public int getSignal(int blockId, int direction) {
        if (blockId >= 0 && blockId < weakSignals.length && direction >= 0 && direction < DIRECTION_COUNT) {
            return weakSignals[blockId][direction] & 0xFF;
        }
        return 0;
    }

    /**
     * Retrieves the direct (strong) redstone signal emitted by this block to the given direction.
     *
     * @param blockId numerical block ID
     * @param direction direction code (0..5)
     * @return direct signal strength (0..15)
     */
    public int getDirectSignal(int blockId, int direction) {
        if (blockId >= 0 && blockId < directSignals.length && direction >= 0 && direction < DIRECTION_COUNT) {
            return directSignals[blockId][direction] & 0xFF;
        }
        return 0;
    }
}
