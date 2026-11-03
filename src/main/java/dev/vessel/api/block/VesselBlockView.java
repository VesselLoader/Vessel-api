package dev.vessel.api.block;

import java.util.Objects;

/**
 * High-performance primitive accessor for block queries across all mod loaders.
 * Bypasses high-level wrapper proxy allocations, routing direct memory reads for
 * block ID, light level, passability, and spatial relative offsets.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class VesselBlockView {

    public static final String RECEPTOR_CLASS_INTERNAL = "dev/vessel/api/block/VesselBlockView";

    /**
     * Constructs a new block view accessor.
     */
    public VesselBlockView() {
    }

    /**
     * Direct primitive accessor for numerical block type ID.
     *
     * @param block target block object
     * @return numerical block ID (or 0 for air)
     */
    public static int getTypeId(Object block) {
        if (block instanceof SimpleBlockSnapshot snapshot) {
            return snapshot.getBlockId();
        }
        if (block instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    /**
     * Direct primitive accessor for block light emission or illumination level.
     *
     * @param block target block object
     * @return light level from 0 to 15
     */
    public static int getLightLevel(Object block) {
        if (block instanceof SimpleBlockSnapshot snapshot) {
            return snapshot.getLightLevel();
        }
        return 0;
    }

    /**
     * Direct primitive accessor for block passability.
     *
     * @param block target block object
     * @return true if entities can pass through this block
     */
    public static boolean isPassable(Object block) {
        if (block instanceof SimpleBlockSnapshot snapshot) {
            return snapshot.isPassable();
        }
        return false;
    }

    /**
     * Direct accessor for relative neighbor block lookup.
     *
     * @param block target block object
     * @param modX delta X coordinate
     * @param modY delta Y coordinate
     * @param modZ delta Z coordinate
     * @return relative block object or null
     */
    public static Object getRelative(Object block, int modX, int modY, int modZ) {
        if (block instanceof SimpleBlockSnapshot snapshot) {
            return snapshot.getRelative(modX, modY, modZ);
        }
        return null;
    }

    /**
     * Direct accessor for block hardness value.
     *
     * @param block target block object
     * @return hardness in float
     */
    public static float getHardness(Object block) {
        if (block instanceof SimpleBlockSnapshot snapshot) {
            return snapshot.getHardness();
        }
        return 0.0f;
    }

    /**
     * Resolves the canonical material ID of the target block.
     *
     * @param block target block object
     * @return material identifier (e.g. "minecraft:stone")
     */
    public static String getMaterialId(Object block) {
        if (block instanceof SimpleBlockSnapshot snapshot) {
            return snapshot.getBlockData().materialId();
        }
        if (block instanceof SimpleBlockData data) {
            return data.materialId();
        }
        return SimpleBlockData.AIR_ID;
    }
}
