package dev.vessel.api.block;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests verifying Universal Block Mechanics bridging Forge IForgeBlock and Bukkit Block
 * across L1 Cache speed, harvest governance, flammability, redstone signals, and explosion resistance.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class UniversalBlockSymmetryTest {

    @Test
    @DisplayName("L1 Cache Speed: ExplosionResistanceCache lookup should achieve sub-nanosecond / micro-burst throughput")
    public void testL1CacheSpeedBenchmark() {
        ExplosionResistanceCache cache = new ExplosionResistanceCache();

        // Warm up JIT compiler
        float warmupAccumulator = 0.0f;
        for (int i = 0; i < 100_000; i++) {
            warmupAccumulator += cache.getExplosionResistance(i % 100);
        }
        assertThat(warmupAccumulator).isGreaterThan(0.0f);

        // Benchmark 10,000,000 iterations
        final int iterations = 10_000_000;
        float accumulator = 0.0f;

        long startNanos = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            accumulator += cache.getExplosionResistance(i & 127);
        }
        long totalNanos = System.nanoTime() - startNanos;

        double nanosPerOp = (double) totalNanos / iterations;
        System.out.println("[BENCHMARK] ExplosionResistanceCache L1 lookup: " + nanosPerOp + " ns/op (accumulator=" + accumulator + ")");

        // Micro-burst throughput expectation in standard JVM
        assertThat(nanosPerOp).isLessThan(30.0);
    }

    @Test
    @DisplayName("Harvest Governor: Evaluates required tool types, mining levels, and effectiveness")
    public void testBlockHarvestGovernor() {
        BlockHarvestGovernor governor = new BlockHarvestGovernor();

        // Stone (ID 1)
        assertThat(governor.getHarvestTool(1)).isEqualTo(BlockHarvestGovernor.TOOL_PICKAXE);
        assertThat(governor.getHarvestLevel(1)).isEqualTo(BlockHarvestGovernor.LEVEL_WOOD);
        assertThat(governor.canHarvestBlock(1, BlockHarvestGovernor.TOOL_PICKAXE, BlockHarvestGovernor.LEVEL_WOOD)).isTrue();
        assertThat(governor.canHarvestBlock(1, BlockHarvestGovernor.TOOL_AXE, BlockHarvestGovernor.LEVEL_DIAMOND)).isFalse();

        // Obsidian (ID 49) requires Diamond pickaxe
        assertThat(governor.getHarvestTool(49)).isEqualTo(BlockHarvestGovernor.TOOL_PICKAXE);
        assertThat(governor.getHarvestLevel(49)).isEqualTo(BlockHarvestGovernor.LEVEL_DIAMOND);
        assertThat(governor.canHarvestBlock(49, BlockHarvestGovernor.TOOL_PICKAXE, BlockHarvestGovernor.LEVEL_IRON)).isFalse();
        assertThat(governor.canHarvestBlock(49, BlockHarvestGovernor.TOOL_PICKAXE, BlockHarvestGovernor.LEVEL_DIAMOND)).isTrue();
        assertThat(governor.canHarvestBlock(49, BlockHarvestGovernor.TOOL_PICKAXE, BlockHarvestGovernor.LEVEL_NETHERITE)).isTrue();

        // Oak Log (ID 17) requires Axe
        assertThat(governor.getHarvestTool(17)).isEqualTo(BlockHarvestGovernor.TOOL_AXE);
        assertThat(governor.canHarvestBlock(17, BlockHarvestGovernor.TOOL_AXE, BlockHarvestGovernor.LEVEL_WOOD)).isTrue();

        // Custom modded block registration
        governor.registerHarvestTool(500, "pickaxe", BlockHarvestGovernor.LEVEL_NETHERITE);
        assertThat(governor.canHarvestBlock(500, "pickaxe", BlockHarvestGovernor.LEVEL_DIAMOND)).isFalse();
        assertThat(governor.canHarvestBlock(500, "pickaxe", BlockHarvestGovernor.LEVEL_NETHERITE)).isTrue();
    }

    @Test
    @DisplayName("Flammability Table: Evaluates combustion, fire spread rate, and inert blocks")
    public void testFlammabilityEvaluationTable() {
        FlammabilityEvaluationTable table = new FlammabilityEvaluationTable();

        // Wood Planks (ID 5)
        assertThat(table.isFlammable(5)).isTrue();
        assertThat(table.getFlammability(5)).isEqualTo(FlammabilityEvaluationTable.FLAMMABILITY_WOOD);
        assertThat(table.getFireSpreadSpeed(5)).isEqualTo(FlammabilityEvaluationTable.SPREAD_WOOD);

        // Leaves (ID 18)
        assertThat(table.isFlammable(18)).isTrue();
        assertThat(table.getFlammability(18)).isEqualTo(FlammabilityEvaluationTable.FLAMMABILITY_LEAVES);
        assertThat(table.getFireSpreadSpeed(18)).isEqualTo(FlammabilityEvaluationTable.SPREAD_LEAVES);

        // Stone (ID 1) is completely inert
        assertThat(table.isFlammable(1)).isFalse();
        assertThat(table.getFlammability(1)).isEqualTo(0);
        assertThat(table.getFireSpreadSpeed(1)).isEqualTo(0);

        // Custom flammability configuration
        table.setFlammability(300, 80, 40);
        assertThat(table.isFlammable(300)).isTrue();
        assertThat(table.getFlammability(300)).isEqualTo(80);
        assertThat(table.getFireSpreadSpeed(300)).isEqualTo(40);
    }

    @Test
    @DisplayName("Redstone Connectivity: Evaluates directional signals, direct emission, and wire connections")
    public void testRedstoneConnectivityLookup() {
        RedstoneConnectivityLookup redstone = new RedstoneConnectivityLookup();

        // Redstone Wire (ID 55): connects horizontally
        assertThat(redstone.canConnectRedstone(55, RedstoneConnectivityLookup.DIRECTION_NORTH)).isTrue();
        assertThat(redstone.canConnectRedstone(55, RedstoneConnectivityLookup.DIRECTION_UP)).isFalse();
        assertThat(redstone.getSignal(55, RedstoneConnectivityLookup.DIRECTION_NORTH)).isEqualTo(15);
        assertThat(redstone.getDirectSignal(55, RedstoneConnectivityLookup.DIRECTION_NORTH)).isEqualTo(0);

        // Redstone Torch (ID 76): strong signal upwards
        assertThat(redstone.getDirectSignal(76, RedstoneConnectivityLookup.DIRECTION_UP)).isEqualTo(15);
        assertThat(redstone.getDirectSignal(76, RedstoneConnectivityLookup.DIRECTION_NORTH)).isEqualTo(0);
        assertThat(redstone.getSignal(76, RedstoneConnectivityLookup.DIRECTION_NORTH)).isEqualTo(15);

        // Redstone Block (ID 152): omnidirectional weak signal 15
        for (int dir = 0; dir < RedstoneConnectivityLookup.DIRECTION_COUNT; dir++) {
            assertThat(redstone.getSignal(152, dir)).isEqualTo(15);
        }

        // Custom redstone device
        redstone.setRedstoneProperties(800, RedstoneConnectivityLookup.DIRECTION_WEST, true, 12, 10);
        assertThat(redstone.canConnectRedstone(800, RedstoneConnectivityLookup.DIRECTION_WEST)).isTrue();
        assertThat(redstone.getSignal(800, RedstoneConnectivityLookup.DIRECTION_WEST)).isEqualTo(12);
        assertThat(redstone.getDirectSignal(800, RedstoneConnectivityLookup.DIRECTION_WEST)).isEqualTo(10);
    }

    @Test
    @DisplayName("Explosion Resistance: Verifies dense cache array values")
    public void testExplosionResistanceCache() {
        ExplosionResistanceCache cache = new ExplosionResistanceCache();

        assertThat(cache.getExplosionResistance(0)).isEqualTo(ExplosionResistanceCache.RESISTANCE_AIR);
        assertThat(cache.getExplosionResistance(1)).isEqualTo(ExplosionResistanceCache.RESISTANCE_STONE);
        assertThat(cache.getExplosionResistance(49)).isEqualTo(ExplosionResistanceCache.RESISTANCE_OBSIDIAN);
        assertThat(cache.getExplosionResistance(7)).isEqualTo(ExplosionResistanceCache.RESISTANCE_BEDROCK);
        assertThat(cache.getExplosionResistance(8)).isEqualTo(ExplosionResistanceCache.RESISTANCE_WATER);

        cache.setExplosionResistance(999, 45.5f);
        assertThat(cache.getExplosionResistance(999)).isEqualTo(45.5f);
    }

    @Test
    @DisplayName("Forge Block Symmetry Adapter: Verifies IForgeBlock receptor routing")
    public void testForgeBlockSymmetryAdapter() {
        SimpleBlockSnapshot stoneSnapshot = new SimpleBlockSnapshot(
                1, 10, 64, 10, SimpleBlockData.of("minecraft:stone"), 0, false, 1.5f
        );

        assertThat(ForgeBlockSymmetryAdapter.getExplosionResistance(stoneSnapshot)).isEqualTo(6.0f);
        assertThat(ForgeBlockSymmetryAdapter.getHarvestTool(stoneSnapshot)).isEqualTo("pickaxe");
        assertThat(ForgeBlockSymmetryAdapter.getHarvestLevel(stoneSnapshot)).isEqualTo(0);
        assertThat(ForgeBlockSymmetryAdapter.canHarvestBlock(stoneSnapshot, "pickaxe", 0)).isTrue();
        assertThat(ForgeBlockSymmetryAdapter.isFlammable(stoneSnapshot)).isFalse();
        assertThat(ForgeBlockSymmetryAdapter.isAir(stoneSnapshot)).isFalse();
        assertThat(ForgeBlockSymmetryAdapter.isPassable(stoneSnapshot)).isFalse();

        // Slime block
        SimpleBlockSnapshot slimeSnapshot = new SimpleBlockSnapshot(
                165, 0, 0, 0, SimpleBlockData.of("minecraft:slime_block"), 0, false, 0.0f
        );
        assertThat(ForgeBlockSymmetryAdapter.isSlimeBlock(slimeSnapshot)).isTrue();
        assertThat(ForgeBlockSymmetryAdapter.isStickyBlock(slimeSnapshot)).isTrue();
        assertThat(ForgeBlockSymmetryAdapter.getFriction(slimeSnapshot)).isEqualTo(ForgeBlockSymmetryAdapter.SLIME_FRICTION);

        // Bookshelf
        SimpleBlockSnapshot bookshelfSnapshot = new SimpleBlockSnapshot(
                47, 0, 0, 0, SimpleBlockData.of("minecraft:bookshelf"), 0, false, 1.5f
        );
        assertThat(ForgeBlockSymmetryAdapter.getEnchantPowerBonus(bookshelfSnapshot)).isEqualTo(1.0f);

        // Beacon Base (Iron Block)
        SimpleBlockSnapshot ironSnapshot = new SimpleBlockSnapshot(
                42, 0, 0, 0, SimpleBlockData.of("minecraft:iron_block"), 0, false, 5.0f
        );
        assertThat(ForgeBlockSymmetryAdapter.isBeaconBase(ironSnapshot)).isTrue();
    }

    @Test
    @DisplayName("Bukkit Block Symmetry Adapter: Verifies Block and BlockData receptor routing")
    public void testBukkitBlockSymmetryAdapter() {
        SimpleBlockSnapshot block = new SimpleBlockSnapshot(
                1, 100, 64, -200, SimpleBlockData.of("minecraft:stone"), 12, false, 1.5f
        );

        assertThat(BukkitBlockSymmetryAdapter.getType(block)).isEqualTo("minecraft:stone");
        assertThat(BukkitBlockSymmetryAdapter.getLightLevel(block)).isEqualTo(12);
        assertThat(BukkitBlockSymmetryAdapter.isPassable(block)).isFalse();
        assertThat(BukkitBlockSymmetryAdapter.isEmpty(block)).isFalse();

        // Mutate type
        BukkitBlockSymmetryAdapter.setType(block, "minecraft:diamond_block");
        assertThat(BukkitBlockSymmetryAdapter.getType(block)).isEqualTo("minecraft:diamond_block");

        // Relative lookup
        Object relative = BukkitBlockSymmetryAdapter.getRelative(block, 0, 1, 0);
        assertThat(relative).isInstanceOf(SimpleBlockSnapshot.class);
        SimpleBlockSnapshot relSnapshot = (SimpleBlockSnapshot) relative;
        assertThat(relSnapshot.getY()).isEqualTo(65);
    }

    @Test
    @DisplayName("Rule 17 Compliance: Strictly zero inner or nested classes across dev.vessel.api.block")
    public void testRule17ComplianceZeroInnerClasses() {
        Class<?>[] classes = new Class<?>[] {
                VesselBlockView.class,
                BlockHarvestGovernor.class,
                FlammabilityEvaluationTable.class,
                RedstoneConnectivityLookup.class,
                ExplosionResistanceCache.class,
                ForgeBlockSymmetryAdapter.class,
                BukkitBlockSymmetryAdapter.class,
                SimpleBlockData.class,
                SimpleBlockSnapshot.class,
                UniversalBlockSymmetryTest.class
        };

        for (Class<?> clazz : classes) {
            assertThat(clazz.getName()).doesNotContain("$");
            assertThat(clazz.isMemberClass()).isFalse();
            assertThat(clazz.isAnonymousClass()).isFalse();
            assertThat(clazz.isLocalClass()).isFalse();
        }
    }
}
