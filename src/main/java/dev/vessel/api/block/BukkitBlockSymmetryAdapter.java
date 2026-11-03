package dev.vessel.api.block;

import java.util.Objects;

/**
 * Direct opcode-level symmetry receptor and adapter bridging Bukkit and Paper
 * {@code org.bukkit.block.Block} and {@code org.bukkit.block.data.BlockData}.
 *
 * <p>Directs upstream block manipulation calls to native VESSEL snapshots and primitive
 * readers without wrapper proxies or craftbukkit internal overhead.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class BukkitBlockSymmetryAdapter {

    public static final String BUKKIT_BLOCK_INTERNAL = "org/bukkit/block/Block";
    public static final String BUKKIT_BLOCK_DATA_INTERNAL = "org/bukkit/block/data/BlockData";
    public static final String RECEPTOR_CLASS_INTERNAL = "dev/vessel/api/block/BukkitBlockSymmetryAdapter";

    /**
     * Constructs a new Bukkit block symmetry adapter instance.
     */
    public BukkitBlockSymmetryAdapter() {
    }

    /**
     * Direct opcode receptor for {@code Block.getType()}.
     *
     * @param block target block object
     * @return material identifier string (e.g. "minecraft:stone")
     */
    public static String getType(Object block) {
        return VesselBlockView.getMaterialId(block);
    }

    /**
     * Direct opcode receptor for {@code Block.setType(Material material)}.
     *
     * @param block target block object
     * @param type material identifier or object
     */
    public static void setType(Object block, Object type) {
        if (block instanceof SimpleBlockSnapshot snapshot && type != null) {
            String materialId = type.toString();
            snapshot.setBlockData(SimpleBlockData.of(materialId));
        }
    }

    /**
     * Direct opcode receptor for {@code Block.getLightLevel()}.
     *
     * @param block target block object
     * @return light emission level (0..15)
     */
    public static int getLightLevel(Object block) {
        return VesselBlockView.getLightLevel(block);
    }

    /**
     * Direct opcode receptor for {@code Block.getBlockData()}.
     *
     * @param block target block object
     * @return block data metadata object
     */
    public static Object getBlockData(Object block) {
        if (block instanceof SimpleBlockSnapshot snapshot) {
            return snapshot.getBlockData();
        }
        return SimpleBlockData.AIR;
    }

    /**
     * Direct opcode receptor for {@code Block.setBlockData(BlockData data)}.
     *
     * @param block target block object
     * @param data new block data state
     */
    public static void setBlockData(Object block, Object data) {
        if (block instanceof SimpleBlockSnapshot snapshot && data instanceof SimpleBlockData blockData) {
            snapshot.setBlockData(blockData);
        }
    }

    /**
     * Direct opcode receptor for {@code Block.getRelative(int modX, int modY, int modZ)}.
     *
     * @param block target block object
     * @param modX delta X
     * @param modY delta Y
     * @param modZ delta Z
     * @return relative block snapshot
     */
    public static Object getRelative(Object block, int modX, int modY, int modZ) {
        return VesselBlockView.getRelative(block, modX, modY, modZ);
    }

    /**
     * Direct opcode receptor for {@code Block.isPassable()}.
     *
     * @param block target block object
     * @return true if passable
     */
    public static boolean isPassable(Object block) {
        return VesselBlockView.isPassable(block);
    }

    /**
     * Direct opcode receptor for {@code Block.isEmpty()}.
     *
     * @param block target block object
     * @return true if block is air
     */
    public static boolean isEmpty(Object block) {
        int blockId = VesselBlockView.getTypeId(block);
        if (blockId == 0) {
            return true;
        }
        return SimpleBlockData.AIR_ID.equals(VesselBlockView.getMaterialId(block));
    }
}
