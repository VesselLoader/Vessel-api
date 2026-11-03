// ==============================================================================
// VESSEL-API — VesselWaveFiveApiContractsTest.java
// Version: 1.0.0
// Changelog:
// - 1.0.0: Initial creation of JUnit 5 test suite verifying Wave 5 VUSA contracts
//          VesselCustomPayloadDescriptor and VesselDataAttachmentSnapshot.
// ==============================================================================

package dev.vessel.api.network;

import dev.vessel.api.attachment.VesselDataAttachmentSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for Wave 5 VUSA network and data attachment contracts
 * ({@link VesselCustomPayloadDescriptor} and {@link VesselDataAttachmentSnapshot}).
 *
 * @version 1.0.0
 */
final class VesselWaveFiveApiContractsTest {

    @Test
    void testCustomPayloadDescriptorDirectionAndProperties() {
        VesselCustomPayloadDescriptor c2s = new VesselCustomPayloadDescriptor(
                "vessel:sync_energy",
                "PLAY_C2S",
                "neoforge",
                32768);

        assertEquals("vessel:sync_energy", c2s.channelKey());
        assertEquals("PLAY_C2S", c2s.direction());
        assertEquals("neoforge", c2s.sourceLoader());
        assertEquals(32768, c2s.maxPayloadBytes());
        assertTrue(c2s.isServerbound());
        assertFalse(c2s.isClientbound());

        VesselCustomPayloadDescriptor serverbound = new VesselCustomPayloadDescriptor(
                "vessel:input",
                "serverbound",
                "fabric",
                1024);
        assertTrue(serverbound.isServerbound());
        assertFalse(serverbound.isClientbound());

        VesselCustomPayloadDescriptor s2c = new VesselCustomPayloadDescriptor(
                "vessel:chunk_delta",
                "PLAY_S2C",
                "paper",
                1048576);
        assertFalse(s2c.isServerbound());
        assertTrue(s2c.isClientbound());

        VesselCustomPayloadDescriptor clientbound = new VesselCustomPayloadDescriptor(
                "vessel:overlay",
                "clientbound",
                "neoforge",
                4096);
        assertFalse(clientbound.isServerbound());
        assertTrue(clientbound.isClientbound());
    }

    @Test
    void testCustomPayloadDescriptorValidation() {
        assertThrows(
                NullPointerException.class,
                () -> new VesselCustomPayloadDescriptor(null, "PLAY_C2S", "neoforge", 1024));
        assertThrows(
                NullPointerException.class,
                () -> new VesselCustomPayloadDescriptor("vessel:chan", null, "neoforge", 1024));
        assertThrows(
                NullPointerException.class,
                () -> new VesselCustomPayloadDescriptor("vessel:chan", "PLAY_C2S", null, 1024));

        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselCustomPayloadDescriptor("   ", "PLAY_C2S", "neoforge", 1024));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselCustomPayloadDescriptor("vessel:chan", "  ", "neoforge", 1024));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselCustomPayloadDescriptor("vessel:chan", "PLAY_C2S", "", 1024));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselCustomPayloadDescriptor("vessel:chan", "PLAY_C2S", "neoforge", 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselCustomPayloadDescriptor("vessel:chan", "PLAY_C2S", "neoforge", -512));
    }

    @Test
    void testDataAttachmentSnapshotPropertiesAndValidation() {
        VesselDataAttachmentSnapshot snapshot = new VesselDataAttachmentSnapshot(
                "vessel:mana_pool",
                "java.lang.Integer",
                "250",
                true);

        assertEquals("vessel:mana_pool", snapshot.attachmentKey());
        assertEquals("java.lang.Integer", snapshot.valueTypeClassName());
        assertEquals("250", snapshot.serializedValue());
        assertTrue(snapshot.persistent());

        assertThrows(
                NullPointerException.class,
                () -> new VesselDataAttachmentSnapshot(null, "java.lang.String", "val", false));
        assertThrows(
                NullPointerException.class,
                () -> new VesselDataAttachmentSnapshot("vessel:key", null, "val", false));
        assertThrows(
                NullPointerException.class,
                () -> new VesselDataAttachmentSnapshot("vessel:key", "java.lang.String", null, false));

        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselDataAttachmentSnapshot("   ", "java.lang.String", "val", false));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselDataAttachmentSnapshot("vessel:key", "  ", "val", false));
    }
}
