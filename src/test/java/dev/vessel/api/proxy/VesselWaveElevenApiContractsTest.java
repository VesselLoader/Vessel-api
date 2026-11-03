package dev.vessel.api.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vessel.api.block.VesselBlockStatePropertySnapshot;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * JUnit 5 test suite verifying Wave 11 VUSA contracts ({@link VesselProxySessionSnapshot} and
 * {@link VesselBlockStatePropertySnapshot}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
final class VesselWaveElevenApiContractsTest {

    @Test
    void testProxySessionSnapshotPropertiesAndMatching() {
        UUID sessionId = UUID.randomUUID();
        VesselProxySessionSnapshot session = new VesselProxySessionSnapshot(
                sessionId,
                "Shiroz",
                "VELOCITY",
                "lobby-01",
                15L);

        assertEquals(sessionId, session.sessionId());
        assertEquals("Shiroz", session.username());
        assertEquals("VELOCITY", session.proxyType());
        assertEquals("lobby-01", session.connectedServer());
        assertEquals(15L, session.pingMillis());
        assertTrue(session.isConnectedTo("lobby-01"));
        assertTrue(session.isConnectedTo("LOBBY-01"));
        assertFalse(session.isConnectedTo("survival-01"));
    }

    @Test
    void testBlockStatePropertySnapshotProperties() {
        VesselBlockStatePropertySnapshot snapshot = new VesselBlockStatePropertySnapshot(
                "minecraft:oak_stairs",
                Map.of("facing", "north", "half", "bottom"),
                true,
                false);

        assertEquals("minecraft:oak_stairs", snapshot.blockId());
        assertTrue(snapshot.solid());
        assertFalse(snapshot.air());
        assertTrue(snapshot.hasProperty("facing"));
        assertEquals("north", snapshot.getProperty("facing"));
        assertEquals("bottom", snapshot.getProperty("half"));
        assertNull(snapshot.getProperty("non_existent"));
    }
}
