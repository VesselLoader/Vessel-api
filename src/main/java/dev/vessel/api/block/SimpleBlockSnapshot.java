package dev.vessel.api.block;

import java.util.Objects;

/**
 * Clean-room block snapshot representing a block at a discrete world coordinate.
 * Mirrors Bukkit {@code Block} and Forge block positional queries.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner or nested classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class SimpleBlockSnapshot {

    private final int blockId;
    private final int x;
    private final int y;
    private final int z;
    private SimpleBlockData blockData;
    private int lightLevel;
    private boolean passable;
    private float hardness;

    /**
     * Constructs a block snapshot with standard properties.
     *
     * @param blockId numerical block id
     * @param x world x-coordinate
     * @param y world y-coordinate
     * @param z world z-coordinate
     * @param blockData block state metadata
     * @param lightLevel block light level (0..15)
     * @param passable whether entities can pass through this block
     * @param hardness block hardness value
     */
    public SimpleBlockSnapshot(
            int blockId,
            int x,
            int y,
            int z,
            SimpleBlockData blockData,
            int lightLevel,
            boolean passable,
            float hardness
    ) {
        this.blockId = blockId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.blockData = Objects.requireNonNull(blockData, "blockData cannot be null");
        this.lightLevel = Math.max(0, Math.min(15, lightLevel));
        this.passable = passable;
        this.hardness = hardness;
    }

    public int getBlockId() {
        return blockId;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public SimpleBlockData getBlockData() {
        return blockData;
    }

    public void setBlockData(SimpleBlockData blockData) {
        this.blockData = Objects.requireNonNull(blockData, "blockData cannot be null");
    }

    public int getLightLevel() {
        return lightLevel;
    }

    public void setLightLevel(int lightLevel) {
        this.lightLevel = Math.max(0, Math.min(15, lightLevel));
    }

    public boolean isPassable() {
        return passable;
    }

    public void setPassable(boolean passable) {
        this.passable = passable;
    }

    public float getHardness() {
        return hardness;
    }

    public void setHardness(float hardness) {
        this.hardness = hardness;
    }

    /**
     * Returns a relative neighbor snapshot offset by the given coordinates.
     *
     * @param modX delta X
     * @param modY delta Y
     * @param modZ delta Z
     * @return offset block snapshot
     */
    public SimpleBlockSnapshot getRelative(int modX, int modY, int modZ) {
        return new SimpleBlockSnapshot(
                0, // air by default
                this.x + modX,
                this.y + modY,
                this.z + modZ,
                SimpleBlockData.AIR,
                0,
                true,
                0.0f
        );
    }
}
