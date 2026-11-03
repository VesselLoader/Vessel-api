// ==============================================================================
// VESSEL-API — VesselWaveSixApiContractsTest.java
// Version: 1.0.0
// Changelog:
// - 1.0.0: Initial creation of JUnit 5 test suite verifying Wave 6 VUSA contracts
//          VesselModConfigEntryDescriptor and VesselPersistentDataSnapshot.
// ==============================================================================

package dev.vessel.api.config;

import dev.vessel.api.persistence.VesselPersistentDataSnapshot;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for Wave 6 VUSA configuration and persistent data contracts
 * ({@link VesselModConfigEntryDescriptor} and {@link VesselPersistentDataSnapshot}).
 *
 * @version 1.0.0
 */
final class VesselWaveSixApiContractsTest {

    @Test
    void testModConfigEntryDescriptorPropertiesAndModifiedState() {
        VesselModConfigEntryDescriptor unmodified = new VesselModConfigEntryDescriptor(
                "mekanism",
                "SERVER",
                "general.energyUnit",
                "FE",
                "FE");

        assertEquals("mekanism", unmodified.modId());
        assertEquals("SERVER", unmodified.configType());
        assertEquals("general.energyUnit", unmodified.dotPath());
        assertEquals("FE", unmodified.defaultValue());
        assertEquals("FE", unmodified.currentValue());
        assertFalse(unmodified.isModified());

        VesselModConfigEntryDescriptor modified = new VesselModConfigEntryDescriptor(
                "mekanism",
                "SERVER",
                "general.energyUnit",
                "FE",
                "J");
        assertTrue(modified.isModified());
    }

    @Test
    void testModConfigEntryDescriptorValidation() {
        assertThrows(
                NullPointerException.class,
                () -> new VesselModConfigEntryDescriptor(null, "SERVER", "a.b", "1", "1"));
        assertThrows(
                NullPointerException.class,
                () -> new VesselModConfigEntryDescriptor("modid", null, "a.b", "1", "1"));
        assertThrows(
                NullPointerException.class,
                () -> new VesselModConfigEntryDescriptor("modid", "SERVER", null, "1", "1"));
        assertThrows(
                NullPointerException.class,
                () -> new VesselModConfigEntryDescriptor("modid", "SERVER", "a.b", null, "1"));
        assertThrows(
                NullPointerException.class,
                () -> new VesselModConfigEntryDescriptor("modid", "SERVER", "a.b", "1", null));

        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselModConfigEntryDescriptor("   ", "SERVER", "a.b", "1", "1"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselModConfigEntryDescriptor("modid", "  ", "a.b", "1", "1"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselModConfigEntryDescriptor("modid", "SERVER", "", "1", "1"));
    }

    @Test
    void testPersistentDataSnapshotLookupAndImmutability() {
        Map<String, String> mutableMap = new HashMap<>();
        mutableMap.put("vessel:guild_id", "g-101");
        mutableMap.put("vessel:rank", "COMMANDER");

        VesselPersistentDataSnapshot snapshot = new VesselPersistentDataSnapshot(
                "player:550e8400-e29b-41d4-a716-446655440000",
                mutableMap);

        mutableMap.put("vessel:extra", "ignored");

        assertEquals("player:550e8400-e29b-41d4-a716-446655440000", snapshot.ownerIdentifier());
        assertEquals(2, snapshot.entries().size());
        assertTrue(snapshot.hasKey("vessel:guild_id"));
        assertFalse(snapshot.hasKey("vessel:extra"));
        assertFalse(snapshot.hasKey(null));
        assertEquals("COMMANDER", snapshot.getValue("vessel:rank"));
        assertNull(snapshot.getValue("vessel:missing"));
        assertNull(snapshot.getValue(null));

        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.entries().put("vessel:new", "value"));

        VesselPersistentDataSnapshot nullEntriesSnapshot = new VesselPersistentDataSnapshot(
                "chunk:0,0",
                null);
        assertTrue(nullEntriesSnapshot.entries().isEmpty());
    }

    @Test
    void testPersistentDataSnapshotValidation() {
        assertThrows(
                NullPointerException.class,
                () -> new VesselPersistentDataSnapshot(null, Map.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselPersistentDataSnapshot("   ", Map.of()));
    }
}
