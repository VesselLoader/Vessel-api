// ==============================================================================
// VESSEL-API — VesselWaveTwoApiContractsTest.java
// Version: 1.0.0
// Changelog:
// - 1.0.0: Initial creation of JUnit 5 test suite verifying Wave 2 VUSA contracts
//          VesselDeferredEntrySnapshot and VesselRegionTaskDescriptor.
// ==============================================================================

package dev.vessel.api.registry;

import dev.vessel.api.scheduler.VesselRegionTaskDescriptor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for Wave 2 VUSA registry and scheduler contracts
 * ({@link VesselDeferredEntrySnapshot} and {@link VesselRegionTaskDescriptor}).
 *
 * @version 1.0.0
 */
public final class VesselWaveTwoApiContractsTest {

    @Test
    void testDeferredEntrySnapshotPropertiesAndEquality() {
        VesselDeferredEntrySnapshot snapshot = new VesselDeferredEntrySnapshot(
                "minecraft:block",
                "vessel:mythril_block",
                "neoforge",
                true);

        assertEquals("minecraft:block", snapshot.getRegistryKey());
        assertEquals("vessel:mythril_block", snapshot.getEntryId());
        assertEquals("neoforge", snapshot.getSourceLoader());
        assertTrue(snapshot.isBound());

        VesselDeferredEntrySnapshot identical = new VesselDeferredEntrySnapshot(
                "minecraft:block",
                "vessel:mythril_block",
                "neoforge",
                true);
        VesselDeferredEntrySnapshot unbound = new VesselDeferredEntrySnapshot(
                "minecraft:block",
                "vessel:mythril_block",
                "neoforge",
                false);

        assertEquals(snapshot, identical);
        assertEquals(snapshot.hashCode(), identical.hashCode());
        assertNotEquals(snapshot, unbound);
        assertTrue(snapshot.toString().contains("vessel:mythril_block"));
    }

    @Test
    void testDeferredEntrySnapshotValidation() {
        assertThrows(
                NullPointerException.class,
                () -> new VesselDeferredEntrySnapshot(null, "vessel:item", "fabric", false));
        assertThrows(
                NullPointerException.class,
                () -> new VesselDeferredEntrySnapshot("minecraft:item", null, "fabric", false));
        assertThrows(
                NullPointerException.class,
                () -> new VesselDeferredEntrySnapshot("minecraft:item", "vessel:item", null, false));

        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselDeferredEntrySnapshot("   ", "vessel:item", "fabric", false));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselDeferredEntrySnapshot("minecraft:item", "  ", "fabric", false));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselDeferredEntrySnapshot("minecraft:item", "vessel:item", "", false));
    }

    @Test
    void testRegionTaskDescriptorPropertiesAndClassification() {
        VesselRegionTaskDescriptor chunkTask = new VesselRegionTaskDescriptor(
                42L,
                "vessel_core",
                12,
                -8,
                false,
                false,
                false);

        assertEquals(42L, chunkTask.getTaskId());
        assertEquals("vessel_core", chunkTask.getOwnerNamespace());
        assertEquals(12, chunkTask.getChunkX());
        assertEquals(-8, chunkTask.getChunkZ());
        assertFalse(chunkTask.isGlobalRegion());
        assertFalse(chunkTask.isAsync());
        assertFalse(chunkTask.isCancelled());
        assertTrue(chunkTask.isChunkRegionTask());

        VesselRegionTaskDescriptor globalTask = new VesselRegionTaskDescriptor(
                43L,
                "vessel_core",
                0,
                0,
                true,
                false,
                false);
        assertTrue(globalTask.isGlobalRegion());
        assertFalse(globalTask.isChunkRegionTask());

        VesselRegionTaskDescriptor asyncTask = new VesselRegionTaskDescriptor(
                44L,
                "vessel_core",
                0,
                0,
                false,
                true,
                true);
        assertTrue(asyncTask.isAsync());
        assertTrue(asyncTask.isCancelled());
        assertFalse(asyncTask.isChunkRegionTask());

        VesselRegionTaskDescriptor chunkTaskCopy = new VesselRegionTaskDescriptor(
                42L,
                "vessel_core",
                12,
                -8,
                false,
                false,
                false);
        assertEquals(chunkTask, chunkTaskCopy);
        assertEquals(chunkTask.hashCode(), chunkTaskCopy.hashCode());
        assertNotEquals(chunkTask, globalTask);
        assertTrue(chunkTask.toString().contains("vessel_core"));
    }

    @Test
    void testRegionTaskDescriptorValidation() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselRegionTaskDescriptor(-1L, "vessel", 0, 0, false, false, false));
        assertThrows(
                NullPointerException.class,
                () -> new VesselRegionTaskDescriptor(1L, null, 0, 0, false, false, false));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselRegionTaskDescriptor(1L, "   ", 0, 0, false, false, false));
    }
}
