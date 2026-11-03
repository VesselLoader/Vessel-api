// ==============================================================================
// VESSEL-API — VesselWaveEightApiContractsTest.java
// Version: 1.0.0
// Changelog:
// - 1.0.0: Initial creation of JUnit 5 test suite verifying Wave 8 VUSA contracts
//          VesselServiceRegistrationDescriptor and VesselChunkStateSnapshot.
// ==============================================================================

package dev.vessel.api.service;

import dev.vessel.api.world.VesselChunkStateSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for Wave 8 VUSA service and chunk state contracts
 * ({@link VesselServiceRegistrationDescriptor} and {@link VesselChunkStateSnapshot}).
 *
 * @version 1.0.0
 */
final class VesselWaveEightApiContractsTest {

    @Test
    void testServiceRegistrationDescriptorPropertiesAndPriority() {
        VesselServiceRegistrationDescriptor highest = new VesselServiceRegistrationDescriptor(
                "net.milkbowl.vault.economy.Economy",
                "dev.vessel.economy.VesselEconomyProvider",
                "VesselEconomy",
                "HIGHEST");

        assertEquals("net.milkbowl.vault.economy.Economy", highest.serviceInterfaceName());
        assertEquals("dev.vessel.economy.VesselEconomyProvider", highest.providerClassName());
        assertEquals("VesselEconomy", highest.ownerPluginId());
        assertEquals("HIGHEST", highest.priorityLevel());
        assertTrue(highest.isHighestPriority());

        VesselServiceRegistrationDescriptor normal = new VesselServiceRegistrationDescriptor(
                "net.milkbowl.vault.permission.Permission",
                "dev.vessel.perm.VesselPermProvider",
                "VesselPerms",
                "NORMAL");
        assertFalse(normal.isHighestPriority());
    }

    @Test
    void testServiceRegistrationDescriptorValidation() {
        assertThrows(
                NullPointerException.class,
                () -> new VesselServiceRegistrationDescriptor(null, "Provider", "Plugin", "NORMAL"));
        assertThrows(
                NullPointerException.class,
                () -> new VesselServiceRegistrationDescriptor("Service", null, "Plugin", "NORMAL"));
        assertThrows(
                NullPointerException.class,
                () -> new VesselServiceRegistrationDescriptor("Service", "Provider", null, "NORMAL"));
        assertThrows(
                NullPointerException.class,
                () -> new VesselServiceRegistrationDescriptor("Service", "Provider", "Plugin", null));

        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselServiceRegistrationDescriptor("   ", "Provider", "Plugin", "NORMAL"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselServiceRegistrationDescriptor("Service", "  ", "Plugin", "NORMAL"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselServiceRegistrationDescriptor("Service", "Provider", "", "NORMAL"));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselServiceRegistrationDescriptor("Service", "Provider", "Plugin", " "));
    }

    @Test
    void testChunkStateSnapshotPropertiesAndPackedKey() {
        VesselChunkStateSnapshot chunk = new VesselChunkStateSnapshot(
                "minecraft:overworld",
                -4,
                7,
                true,
                true,
                18);

        assertEquals("minecraft:overworld", chunk.worldName());
        assertEquals(-4, chunk.chunkX());
        assertEquals(7, chunk.chunkZ());
        assertTrue(chunk.loaded());
        assertTrue(chunk.forceLoaded());
        assertEquals(18, chunk.entityCount());

        long packed = chunk.packedChunkKey();
        assertEquals(-4, (int) packed);
        assertEquals(7, (int) (packed >>> 32));
    }

    @Test
    void testChunkStateSnapshotValidation() {
        assertThrows(
                NullPointerException.class,
                () -> new VesselChunkStateSnapshot(null, 0, 0, true, false, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselChunkStateSnapshot("   ", 0, 0, true, false, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselChunkStateSnapshot("minecraft:overworld", 0, 0, true, false, -1));
    }
}
