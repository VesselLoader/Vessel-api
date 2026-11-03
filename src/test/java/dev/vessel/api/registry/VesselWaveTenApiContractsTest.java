// ==============================================================================
// VESSEL-API — VesselWaveTenApiContractsTest.java
// Version: 1.0.0
// Changelog:
// - 1.0.0: Initial creation of JUnit 5 test suite verifying Wave 10 VUSA contracts
//          VesselBuiltInRegistryEntrySnapshot and VesselVirtualInventorySnapshot.
// ==============================================================================

package dev.vessel.api.registry;

import dev.vessel.api.storage.VesselVirtualInventorySnapshot;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for Wave 10 VUSA built-in registry and virtual inventory contracts
 * ({@link VesselBuiltInRegistryEntrySnapshot} and {@link VesselVirtualInventorySnapshot}).
 *
 * @version 1.0.0
 */
public final class VesselWaveTenApiContractsTest {

    @Test
    void testBuiltInRegistryEntrySnapshotPropertiesTagsAndValidation() {
        Set<String> mutableTags = new HashSet<>();
        mutableTags.add("minecraft:mineable/pickaxe");
        mutableTags.add("minecraft:needs_iron_tool");

        VesselBuiltInRegistryEntrySnapshot entry = new VesselBuiltInRegistryEntrySnapshot(
                "minecraft:block",
                "vessel:titanium_ore",
                412,
                mutableTags);

        mutableTags.add("minecraft:beacon_base_blocks");

        assertEquals("minecraft:block", entry.registryKey());
        assertEquals("vessel:titanium_ore", entry.entryKey());
        assertEquals(412, entry.rawNumericalId());
        assertEquals(2, entry.boundTags().size());
        assertTrue(entry.hasTag("minecraft:mineable/pickaxe"));
        assertFalse(entry.hasTag("minecraft:beacon_base_blocks"));
        assertFalse(entry.hasTag(null));

        assertThrows(
                UnsupportedOperationException.class,
                () -> entry.boundTags().add("minecraft:logs"));

        VesselBuiltInRegistryEntrySnapshot nullTagsEntry = new VesselBuiltInRegistryEntrySnapshot(
                "minecraft:item",
                "vessel:titanium_ingot",
                0,
                null);
        assertTrue(nullTagsEntry.boundTags().isEmpty());

        assertThrows(
                NullPointerException.class,
                () -> new VesselBuiltInRegistryEntrySnapshot(null, "vessel:item", 1, Set.of()));
        assertThrows(
                NullPointerException.class,
                () -> new VesselBuiltInRegistryEntrySnapshot("minecraft:item", null, 1, Set.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselBuiltInRegistryEntrySnapshot("   ", "vessel:item", 1, Set.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselBuiltInRegistryEntrySnapshot("minecraft:item", "  ", 1, Set.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselBuiltInRegistryEntrySnapshot("minecraft:item", "vessel:item", -1, Set.of()));
    }

    @Test
    void testVirtualInventorySnapshotSlotsAndValidation() {
        Map<Integer, String> mutableSlots = new HashMap<>();
        mutableSlots.put(0, "minecraft:compass");
        mutableSlots.put(13, "minecraft:nether_star");

        VesselVirtualInventorySnapshot gui = new VesselVirtualInventorySnapshot(
                "minecraft:generic_9x3",
                "Server Selector",
                27,
                mutableSlots);

        mutableSlots.put(26, "minecraft:barrier");

        assertEquals("minecraft:generic_9x3", gui.inventoryTypeId());
        assertEquals("Server Selector", gui.title());
        assertEquals(27, gui.size());
        assertEquals(2, gui.occupiedSlotCount());
        assertTrue(gui.isSlotOccupied(0));
        assertTrue(gui.isSlotOccupied(13));
        assertFalse(gui.isSlotOccupied(26));
        assertFalse(gui.isSlotOccupied(5));

        assertThrows(
                UnsupportedOperationException.class,
                () -> gui.occupiedSlots().put(1, "minecraft:stone"));

        VesselVirtualInventorySnapshot emptyGui = new VesselVirtualInventorySnapshot(
                "minecraft:chest",
                "",
                9,
                null);
        assertEquals(0, emptyGui.occupiedSlotCount());

        assertThrows(
                NullPointerException.class,
                () -> new VesselVirtualInventorySnapshot(null, "Title", 9, Map.of()));
        assertThrows(
                NullPointerException.class,
                () -> new VesselVirtualInventorySnapshot("minecraft:chest", null, 9, Map.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselVirtualInventorySnapshot("   ", "Title", 9, Map.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselVirtualInventorySnapshot("minecraft:chest", "Title", 0, Map.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselVirtualInventorySnapshot("minecraft:chest", "Title", -9, Map.of()));
    }
}
