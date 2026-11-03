// ==============================================================================
// VESSEL-API — VesselWaveFourApiContractsTest.java
// Version: 1.0.0
// Changelog:
// - 1.0.0: Initial creation of JUnit 5 test suite verifying Wave 4 VUSA contracts
//          VesselComponentSnapshot and VesselItemStackSnapshot.
// ==============================================================================

package dev.vessel.api.text;

import dev.vessel.api.inventory.VesselItemStackSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for Wave 4 VUSA text and inventory contracts
 * ({@link VesselComponentSnapshot} and {@link VesselItemStackSnapshot}).
 *
 * @version 1.0.0
 */
public final class VesselWaveFourApiContractsTest {

    @Test
    void testComponentSnapshotPropertiesAndEmptiness() {
        VesselComponentSnapshot populated = new VesselComponentSnapshot(
                "VESSEL Sovereign Core",
                "#55FFFF",
                2);

        assertEquals("VESSEL Sovereign Core", populated.getPlainText());
        assertEquals("#55FFFF", populated.getHexColor());
        assertEquals(2, populated.getChildCount());
        assertFalse(populated.isEmpty());

        VesselComponentSnapshot emptyWithChildren = new VesselComponentSnapshot(
                "",
                "#FFFFFF",
                1);
        assertFalse(emptyWithChildren.isEmpty());

        VesselComponentSnapshot trulyEmpty = new VesselComponentSnapshot(
                "",
                "#FFFFFF",
                0);
        assertTrue(trulyEmpty.isEmpty());

        VesselComponentSnapshot populatedCopy = new VesselComponentSnapshot(
                "VESSEL Sovereign Core",
                "#55FFFF",
                2);
        assertEquals(populated, populatedCopy);
        assertEquals(populated.hashCode(), populatedCopy.hashCode());
        assertNotEquals(populated, trulyEmpty);
        assertTrue(populated.toString().contains("VESSEL Sovereign Core"));
    }

    @Test
    void testComponentSnapshotValidation() {
        assertThrows(
                NullPointerException.class,
                () -> new VesselComponentSnapshot(null, "#FFFFFF", 0));
        assertThrows(
                NullPointerException.class,
                () -> new VesselComponentSnapshot("Text", null, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselComponentSnapshot("Text", "   ", 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselComponentSnapshot("Text", "#FFFFFF", -1));
    }

    @Test
    void testItemStackSnapshotPropertiesAndAirOrEmpty() {
        VesselItemStackSnapshot sword = new VesselItemStackSnapshot(
                "minecraft:netherite_sword",
                1,
                "Excalibur",
                1001);

        assertEquals("minecraft:netherite_sword", sword.getMaterialKey());
        assertEquals(1, sword.getAmount());
        assertEquals("Excalibur", sword.getDisplayName());
        assertEquals(1001, sword.getCustomModelData());
        assertFalse(sword.isAirOrEmpty());

        VesselItemStackSnapshot zeroCount = new VesselItemStackSnapshot(
                "minecraft:diamond",
                0,
                "",
                0);
        assertTrue(zeroCount.isAirOrEmpty());

        VesselItemStackSnapshot airStack = new VesselItemStackSnapshot(
                "MINECRAFT:AIR",
                1,
                "",
                0);
        assertTrue(airStack.isAirOrEmpty());

        VesselItemStackSnapshot swordCopy = new VesselItemStackSnapshot(
                "minecraft:netherite_sword",
                1,
                "Excalibur",
                1001);
        assertEquals(sword, swordCopy);
        assertEquals(sword.hashCode(), swordCopy.hashCode());
        assertNotEquals(sword, zeroCount);
        assertTrue(sword.toString().contains("Excalibur"));
    }

    @Test
    void testItemStackSnapshotValidation() {
        assertThrows(
                NullPointerException.class,
                () -> new VesselItemStackSnapshot(null, 1, "", 0));
        assertThrows(
                NullPointerException.class,
                () -> new VesselItemStackSnapshot("minecraft:stone", 1, null, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselItemStackSnapshot("   ", 1, "", 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselItemStackSnapshot("minecraft:stone", -1, "", 0));
    }
}
