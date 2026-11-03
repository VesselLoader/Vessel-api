// ==============================================================================
// VESSEL-API — VesselWaveNineApiContractsTest.java
// Version: 1.0.0
// Changelog:
// - 1.0.0: Initial creation of JUnit 5 test suite verifying Wave 9 VUSA contracts
//          VesselNbtCompoundSnapshot and VesselWorldBorderSnapshot.
// ==============================================================================

package dev.vessel.api.nbt;

import dev.vessel.api.world.VesselWorldBorderSnapshot;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for Wave 9 VUSA NBT compound and world border contracts
 * ({@link VesselNbtCompoundSnapshot} and {@link VesselWorldBorderSnapshot}).
 *
 * @version 1.0.0
 */
final class VesselWaveNineApiContractsTest {

    @Test
    void testNbtCompoundSnapshotCountsLookupAndImmutability() {
        Map<String, String> mutableStrings = new HashMap<>();
        mutableStrings.put("CustomName", "Excalibur");
        mutableStrings.put("Owner", "VesselAdmin");

        Map<String, Long> mutableNumbers = new HashMap<>();
        mutableNumbers.put("RepairCost", 15L);

        VesselNbtCompoundSnapshot snapshot = new VesselNbtCompoundSnapshot(
                "tag",
                mutableStrings,
                mutableNumbers);

        mutableStrings.put("ExtraString", "Ignored");
        mutableNumbers.put("ExtraNum", 99L);

        assertEquals("tag", snapshot.rootTagName());
        assertEquals(3, snapshot.totalTagCount());
        assertTrue(snapshot.containsKey("CustomName"));
        assertTrue(snapshot.containsKey("RepairCost"));
        assertFalse(snapshot.containsKey("ExtraString"));
        assertFalse(snapshot.containsKey("MissingKey"));
        assertFalse(snapshot.containsKey(null));

        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.stringEntries().put("Mutated", "Val"));
        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.numericEntries().put("Mutated", 1L));

        VesselNbtCompoundSnapshot emptyMapsSnapshot = new VesselNbtCompoundSnapshot("", null, null);
        assertEquals(0, emptyMapsSnapshot.totalTagCount());
        assertTrue(emptyMapsSnapshot.stringEntries().isEmpty());
        assertTrue(emptyMapsSnapshot.numericEntries().isEmpty());

        assertThrows(
                NullPointerException.class,
                () -> new VesselNbtCompoundSnapshot(null, Map.of(), Map.of()));
    }

    @Test
    void testWorldBorderSnapshotBoundsAndValidation() {
        VesselWorldBorderSnapshot border = new VesselWorldBorderSnapshot(
                "minecraft:overworld",
                100.0D,
                -200.0D,
                1000.0D,
                0.2D,
                5);

        assertEquals("minecraft:overworld", border.worldName());
        assertEquals(100.0D, border.centerX());
        assertEquals(-200.0D, border.centerZ());
        assertEquals(1000.0D, border.size());
        assertEquals(0.2D, border.damageAmount());
        assertEquals(5, border.warningDistance());

        assertTrue(border.isInside(100.0D, -200.0D));
        assertTrue(border.isInside(600.0D, 300.0D));
        assertFalse(border.isInside(600.1D, -200.0D));
        assertFalse(border.isInside(100.0D, -700.1D));

        assertThrows(
                NullPointerException.class,
                () -> new VesselWorldBorderSnapshot(null, 0.0D, 0.0D, 100.0D, 0.2D, 5));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselWorldBorderSnapshot("   ", 0.0D, 0.0D, 100.0D, 0.2D, 5));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselWorldBorderSnapshot("minecraft:overworld", 0.0D, 0.0D, 0.5D, 0.2D, 5));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselWorldBorderSnapshot("minecraft:overworld", 0.0D, 0.0D, 100.0D, -0.1D, 5));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselWorldBorderSnapshot("minecraft:overworld", 0.0D, 0.0D, 100.0D, 0.2D, -1));
    }
}
