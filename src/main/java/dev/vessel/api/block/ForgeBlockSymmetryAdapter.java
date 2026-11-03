package dev.vessel.api.block;

import java.util.Objects;

/**
 * Direct opcode-level symmetry receptor and adapter bridging the 40+ methods of Forge
 * and NeoForge {@code net.minecraftforge.common.extensions.IForgeBlock}.
 *
 * <p>Directs upstream block extension calls to native VESSEL dense cache tables and primitive
 * evaluators without intermediate object wrappers.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ForgeBlockSymmetryAdapter {

    public static final String FORGE_BLOCK_INTERNAL = "net/minecraftforge/common/extensions/IForgeBlock";
    public static final String RECEPTOR_CLASS_INTERNAL = "dev/vessel/api/block/ForgeBlockSymmetryAdapter";

    public static final float DEFAULT_FRICTION = 0.6f;
    public static final float SLIME_FRICTION = 0.8f;
    public static final float ICE_FRICTION = 0.98f;

    private static final ExplosionResistanceCache EXPLOSION_CACHE = new ExplosionResistanceCache();
    private static final BlockHarvestGovernor HARVEST_GOVERNOR = new BlockHarvestGovernor();
    private static final FlammabilityEvaluationTable FLAMMABILITY_TABLE = new FlammabilityEvaluationTable();
    private static final RedstoneConnectivityLookup REDSTONE_LOOKUP = new RedstoneConnectivityLookup();

    /**
     * Constructs a new Forge block symmetry adapter instance.
     */
    public ForgeBlockSymmetryAdapter() {
    }

    public static ExplosionResistanceCache getExplosionCache() {
        return EXPLOSION_CACHE;
    }

    public static BlockHarvestGovernor getHarvestGovernor() {
        return HARVEST_GOVERNOR;
    }

    public static FlammabilityEvaluationTable getFlammabilityTable() {
        return FLAMMABILITY_TABLE;
    }

    public static RedstoneConnectivityLookup getRedstoneLookup() {
        return REDSTONE_LOOKUP;
    }

    /**
     * Direct opcode receptor for {@code IForgeBlock.getExplosionResistance()}.
     */
    public static float getExplosionResistance(Object block) {
        int blockId = VesselBlockView.getTypeId(block);
        return EXPLOSION_CACHE.getExplosionResistance(blockId);
    }

    /**
     * Direct opcode receptor for {@code IForgeBlock.getHarvestTool(BlockState state)}.
     */
    public static String getHarvestTool(Object block) {
        int blockId = VesselBlockView.getTypeId(block);
        return HARVEST_GOVERNOR.getHarvestTool(blockId);
    }

    /**
     * Direct opcode receptor for {@code IForgeBlock.getHarvestLevel(BlockState state)}.
     */
    public static int getHarvestLevel(Object block) {
        int blockId = VesselBlockView.getTypeId(block);
        return HARVEST_GOVERNOR.getHarvestLevel(blockId);
    }

    /**
     * Direct opcode receptor for {@code IForgeBlock.canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player)}.
     */
    public static boolean canHarvestBlock(Object block, String toolType, int toolLevel) {
        int blockId = VesselBlockView.getTypeId(block);
        return HARVEST_GOVERNOR.canHarvestBlock(blockId, toolType, toolLevel);
    }

    /**
     * Direct opcode receptor for {@code IForgeBlock.isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction)}.
     */
    public static boolean isFlammable(Object block) {
        int blockId = VesselBlockView.getTypeId(block);
        return FLAMMABILITY_TABLE.isFlammable(blockId);
    }

    /**
     * Direct opcode receptor for {@code IForgeBlock.getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction)}.
     */
    public static int getFlammability(Object block) {
        int blockId = VesselBlockView.getTypeId(block);
        return FLAMMABILITY_TABLE.getFlammability(blockId);
    }

    /**
     * Direct opcode receptor for {@code IForgeBlock.getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction)}.
     */
    public static int getFireSpreadSpeed(Object block) {
        int blockId = VesselBlockView.getTypeId(block);
        return FLAMMABILITY_TABLE.getFireSpreadSpeed(blockId);
    }

    /**
     * Direct opcode receptor for {@code IForgeBlock.canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, Direction direction)}.
     */
    public static boolean canConnectRedstone(Object block, int direction) {
        int blockId = VesselBlockView.getTypeId(block);
        return REDSTONE_LOOKUP.canConnectRedstone(blockId, direction);
    }

    /**
     * Direct opcode receptor for {@code IForgeBlock.getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction)}.
     */
    public static int getSignal(Object block, int direction) {
        int blockId = VesselBlockView.getTypeId(block);
        return REDSTONE_LOOKUP.getSignal(blockId, direction);
    }

    /**
     * Direct opcode receptor for {@code IForgeBlock.getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction)}.
     */
    public static int getDirectSignal(Object block, int direction) {
        int blockId = VesselBlockView.getTypeId(block);
        return REDSTONE_LOOKUP.getDirectSignal(blockId, direction);
    }

    /**
     * Direct opcode receptor for {@code IForgeBlock.getLightEmission(BlockState state, BlockGetter level, BlockPos pos)}.
     */
    public static int getLightEmission(Object block) {
        return VesselBlockView.getLightLevel(block);
    }

    /**
     * Direct opcode receptor for {@code IForgeBlock.isPassable(BlockState state, BlockGetter level, BlockPos pos)}.
     */
    public static boolean isPassable(Object block) {
        return VesselBlockView.isPassable(block);
    }

    /**
     * Direct opcode receptor for {@code IForgeBlock.isAir(BlockState state, BlockGetter level, BlockPos pos)}.
     */
    public static boolean isAir(Object block) {
        int blockId = VesselBlockView.getTypeId(block);
        return blockId == 0;
    }

    /**
     * Direct opcode receptor for {@code IForgeBlock.getEnchantPowerBonus(BlockState state, LevelReader level, BlockPos pos)}.
     */
    public static float getEnchantPowerBonus(Object block) {
        int blockId = VesselBlockView.getTypeId(block);
        // Bookshelf (ID 47) grants 1.0 bonus
        return blockId == 47 ? 1.0f : 0.0f;
    }

    /**
     * Direct opcode receptor for {@code IForgeBlock.isBeaconBase(BlockState state, LevelReader level, BlockPos pos, BlockPos beacon)}.
     */
    public static boolean isBeaconBase(Object block) {
        int blockId = VesselBlockView.getTypeId(block);
        // Iron (42), Gold (41), Diamond (57), Emerald (133), Netherite
        return blockId == 42 || blockId == 41 || blockId == 57 || blockId == 133;
    }

    /**
     * Direct opcode receptor for {@code IForgeBlock.isPortalFrame(BlockState state, BlockGetter level, BlockPos pos)}.
     */
    public static boolean isPortalFrame(Object block) {
        int blockId = VesselBlockView.getTypeId(block);
        return blockId == 49 || blockId == 120; // Obsidian or End Portal Frame
    }

    /**
     * Direct opcode receptor for {@code IForgeBlock.getFriction(BlockState state, LevelReader level, BlockPos pos, Entity entity)}.
     */
    public static float getFriction(Object block) {
        int blockId = VesselBlockView.getTypeId(block);
        if (blockId == 79 || blockId == 174) { // Ice or Packed Ice
            return ICE_FRICTION;
        }
        if (blockId == 165) { // Slime Block
            return SLIME_FRICTION;
        }
        return DEFAULT_FRICTION;
    }

    /**
     * Direct opcode receptor for {@code IForgeBlock.isSlimeBlock(BlockState state)}.
     */
    public static boolean isSlimeBlock(Object block) {
        return VesselBlockView.getTypeId(block) == 165;
    }

    /**
     * Direct opcode receptor for {@code IForgeBlock.isStickyBlock(BlockState state)}.
     */
    public static boolean isStickyBlock(Object block) {
        int id = VesselBlockView.getTypeId(block);
        return id == 165 || id == 260; // Slime or Honey
    }
}
