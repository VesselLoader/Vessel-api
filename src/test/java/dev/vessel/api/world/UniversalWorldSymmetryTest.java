package dev.vessel.api.world;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verification test suite for Universal World & Chunk Substrate under VUSA Chapter 3.6
 * and Method Symmetry Dictionary Domain 6.
 *
 * <p>Strictly adheres to Rule 17: Zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 * @since 1.0.0 (Pass K-33)
 */
public final class UniversalWorldSymmetryTest {

    private VesselWorldView worldView;
    private WorldEntitySpatialGrid spatialGrid;
    private WorldEnvironmentalGovernor environmentalGovernor;
    private ChunkTicketGovernor ticketGovernor;
    private BukkitWorldSymmetryAdapter worldAdapter;
    private BukkitChunkSymmetryAdapter chunkAdapter;

    private final String mockWorld = "vessel:overworld";

    @BeforeEach
    public void setUp() {
        this.worldView = new VesselWorldView();
        this.spatialGrid = new WorldEntitySpatialGrid();
        this.environmentalGovernor = new WorldEnvironmentalGovernor(worldView);
        this.ticketGovernor = new ChunkTicketGovernor(worldView);
        this.worldAdapter = new BukkitWorldSymmetryAdapter(worldView, spatialGrid, environmentalGovernor);
        this.chunkAdapter = new BukkitChunkSymmetryAdapter(worldView, ticketGovernor, spatialGrid);
    }

    @Test
    @DisplayName("CoordinateFastPacker packs and unpacks coordinates across full signed bounds")
    public void testCoordinatePackingRoundtrip() {
        // Standard coordinates
        int x1 = 150;
        int y1 = 64;
        int z1 = -230;
        long packed1 = CoordinateFastPacker.pack(x1, y1, z1);
        assertThat(CoordinateFastPacker.unpackX(packed1)).isEqualTo(x1);
        assertThat(CoordinateFastPacker.unpackY(packed1)).isEqualTo(y1);
        assertThat(CoordinateFastPacker.unpackZ(packed1)).isEqualTo(z1);

        // Negative build height (1.18+ cave heights: -64)
        int x2 = -1024;
        int y2 = -64;
        int z2 = -4096;
        long packed2 = CoordinateFastPacker.pack(x2, y2, z2);
        assertThat(CoordinateFastPacker.unpackX(packed2)).isEqualTo(x2);
        assertThat(CoordinateFastPacker.unpackY(packed2)).isEqualTo(y2);
        assertThat(CoordinateFastPacker.unpackZ(packed2)).isEqualTo(z2);

        // Max build height (320)
        int x3 = 50000;
        int y3 = 320;
        int z3 = 80000;
        long packed3 = CoordinateFastPacker.pack(x3, y3, z3);
        assertThat(CoordinateFastPacker.unpackX(packed3)).isEqualTo(x3);
        assertThat(CoordinateFastPacker.unpackY(packed3)).isEqualTo(y3);
        assertThat(CoordinateFastPacker.unpackZ(packed3)).isEqualTo(z3);

        // Chunk packing
        int cx = -45;
        int cz = 88;
        long packedChunk = CoordinateFastPacker.packChunk(cx, cz);
        assertThat(CoordinateFastPacker.unpackChunkX(packedChunk)).isEqualTo(cx);
        assertThat(CoordinateFastPacker.unpackChunkZ(packedChunk)).isEqualTo(cz);
    }

    @Test
    @DisplayName("VesselWorldView accesses and mutates primitive world states correctly")
    public void testVesselWorldViewPrimitives() {
        // Blocks: default air = 0
        assertThat(worldView.getBlockTypeId(mockWorld, 10, 64, 10)).isEqualTo(0);

        // Mutate block to diamond block (ID 57)
        worldView.setBlockTypeId(mockWorld, 10, 64, 10, 57);
        assertThat(worldView.getBlockTypeId(mockWorld, 10, 64, 10)).isEqualTo(57);

        // Time
        worldView.setTime(mockWorld, 6000L); // noon
        assertThat(worldView.getTime(mockWorld)).isEqualTo(6000L);

        // Storm
        assertThat(worldView.hasStorm(mockWorld)).isFalse();
        worldView.setStorm(mockWorld, true);
        assertThat(worldView.hasStorm(mockWorld)).isTrue();

        // Chunk loaded state
        assertThat(worldView.isChunkLoaded(mockWorld, 5, 5)).isFalse();
        worldView.setChunkLoaded(mockWorld, 5, 5, true);
        assertThat(worldView.isChunkLoaded(mockWorld, 5, 5)).isTrue();
    }

    @Test
    @DisplayName("WorldEntitySpatialGrid accurately queries entities in radius with O(1) cell lookup")
    public void testSpatialGridRadiusQuery() {
        String zombie = "zombie_1";
        String skeleton = "skeleton_1";
        String dragon = "dragon_far";

        // Register zombie at (10, 64, 10)
        spatialGrid.registerEntity(mockWorld, zombie, 10.0, 64.0, 10.0);
        // Register skeleton at (13, 64, 14) (distance ~ 5 blocks)
        spatialGrid.registerEntity(mockWorld, skeleton, 13.0, 64.0, 14.0);
        // Register dragon far away at (200, 100, 200)
        spatialGrid.registerEntity(mockWorld, dragon, 200.0, 100.0, 200.0);

        assertThat(spatialGrid.getEntityCount(mockWorld)).isEqualTo(3);

        // Query radius 10 around (10, 64, 10)
        List<Object> inRadius = spatialGrid.getEntitiesInRadius(mockWorld, 10.0, 64.0, 10.0, 10.0);
        assertThat(inRadius).contains(zombie, skeleton);
        assertThat(inRadius).doesNotContain(dragon);

        // Move dragon close to (11, 64, 11)
        spatialGrid.updateEntityPosition(mockWorld, dragon, 11.0, 64.0, 11.0);
        List<Object> updatedInRadius = spatialGrid.getEntitiesInRadius(mockWorld, 10.0, 64.0, 10.0, 10.0);
        assertThat(updatedInRadius).contains(zombie, skeleton, dragon);

        // Remove zombie
        spatialGrid.removeEntity(mockWorld, zombie);
        List<Object> afterRemoval = spatialGrid.getEntitiesInRadius(mockWorld, 10.0, 64.0, 10.0, 10.0);
        assertThat(afterRemoval).doesNotContain(zombie);
        assertThat(afterRemoval).contains(skeleton, dragon);

        // Chunk query (chunk 0, 0 covers 0..15 in X and Z)
        List<Object> inChunk = spatialGrid.getEntitiesInChunk(mockWorld, 0, 0);
        assertThat(inChunk).contains(skeleton, dragon);
    }

    @Test
    @DisplayName("ChunkTicketGovernor manages ticket lifetimes and prevents premature chunk unloads")
    public void testChunkTicketLifecycle() {
        int cx = 16;
        int cz = 24;
        String modA = "mod_industrial";
        String pluginB = "plugin_claim";

        assertThat(ticketGovernor.isTicketActive(mockWorld, cx, cz)).isFalse();
        assertThat(chunkAdapter.isLoaded(mockWorld, cx, cz)).isFalse();

        // Add ticket from modA -> loads chunk
        boolean first = ticketGovernor.addTicket(mockWorld, cx, cz, modA);
        assertThat(first).isTrue();
        assertThat(ticketGovernor.isTicketActive(mockWorld, cx, cz)).isTrue();
        assertThat(chunkAdapter.isLoaded(mockWorld, cx, cz)).isTrue();

        // Add ticket from pluginB
        boolean second = ticketGovernor.addTicket(mockWorld, cx, cz, pluginB);
        assertThat(second).isFalse();
        assertThat(ticketGovernor.getTicketCount(mockWorld, cx, cz)).isEqualTo(2);

        // Attempt to unload chunk via Bukkit Chunk.unload -> must fail because tickets are active
        boolean unloaded = chunkAdapter.unload(mockWorld, cx, cz, true);
        assertThat(unloaded).isFalse();
        assertThat(chunkAdapter.isLoaded(mockWorld, cx, cz)).isTrue();

        // Remove modA ticket
        boolean emptyAfterModA = ticketGovernor.removeTicket(mockWorld, cx, cz, modA);
        assertThat(emptyAfterModA).isFalse(); // pluginB ticket still active
        assertThat(chunkAdapter.isLoaded(mockWorld, cx, cz)).isTrue();

        // Remove pluginB ticket -> chunk released
        boolean emptyAfterPluginB = ticketGovernor.removeTicket(mockWorld, cx, cz, pluginB);
        assertThat(emptyAfterPluginB).isTrue();
        assertThat(chunkAdapter.isLoaded(mockWorld, cx, cz)).isFalse();
    }

    @Test
    @DisplayName("BukkitWorldSymmetryAdapter and BukkitChunkSymmetryAdapter execute opcode recipes without allocation")
    public void testBukkitSymmetryAdapters() {
        // Set block and retrieve
        worldView.setBlockTypeId(mockWorld, 20, 70, 20, 1); // Stone
        Object blockType = worldAdapter.getBlockAt(mockWorld, 20, 70, 20);
        assertThat(blockType).isEqualTo(1);

        // Spawn entity
        Object spawned = worldAdapter.spawnEntity(mockWorld, 20.0, 70.0, 20.0, "COW");
        assertThat(spawned).isNotNull();

        // Drop item
        Object dropped = worldAdapter.dropItem(mockWorld, 21.0, 70.0, 20.0, "DIAMOND");
        assertThat(dropped).isNotNull();

        // Verify entities registered in chunk (chunk coords: 20 >> 4 = 1)
        Object[] chunkEntities = chunkAdapter.getEntities(mockWorld, 1, 1);
        assertThat(chunkEntities).contains(spawned, dropped);

        // Explosion test
        boolean exploded = worldAdapter.createExplosion(mockWorld, 20.0, 70.0, 20.0, 4.0f, true);
        assertThat(exploded).isTrue();
        assertThat(environmentalGovernor.getExplosionCount(mockWorld)).isEqualTo(1);

        // Static opcode receptor calls
        VesselWorldView.getInstance().setBlockTypeId(mockWorld, 20, 70, 20, 1);
        Object staticBlock = BukkitWorldSymmetryAdapter.getBlock(mockWorld, 20, 70, 20);
        assertThat(staticBlock).isEqualTo(1);

        boolean staticLoad = BukkitChunkSymmetryAdapter.loadChunk(mockWorld, 2, 2, true);
        assertThat(staticLoad).isTrue();
        assertThat(BukkitChunkSymmetryAdapter.isChunkLoaded(mockWorld, 2, 2)).isTrue();
    }

    @Test
    @DisplayName("Verify Rule 17 strict adherence across all world classes (Zero inner/nested classes)")
    public void testRule17Compliance() {
        Class<?>[] classes = new Class<?>[]{
                CoordinateFastPacker.class,
                VesselWorldView.class,
                WorldEntitySpatialGrid.class,
                WorldEnvironmentalGovernor.class,
                ChunkTicketGovernor.class,
                BukkitWorldSymmetryAdapter.class,
                BukkitChunkSymmetryAdapter.class,
                UniversalWorldSymmetryTest.class
        };

        for (Class<?> clazz : classes) {
            assertThat(clazz.getDeclaredClasses())
                    .withFailMessage("Class " + clazz.getName() + " violates Rule 17 by declaring inner/nested classes!")
                    .isEmpty();
        }
    }
}
