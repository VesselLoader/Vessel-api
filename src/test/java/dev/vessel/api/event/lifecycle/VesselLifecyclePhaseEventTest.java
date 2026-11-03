// ==============================================================================
// VESSEL-API — VesselLifecyclePhaseEventTest.java
// Version: 1.0.0
// Changelog:
// - 1.0.0: Initial creation of unit test suite for VesselServerBootPhase and
//          VesselLifecyclePhaseEvent verifying ADR-0001 (VES-BOOT-002) contracts.
// ==============================================================================

package dev.vessel.api.event.lifecycle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link VesselServerBootPhase} and {@link VesselLifecyclePhaseEvent}.
 *
 * @version 1.0.0
 */
public final class VesselLifecyclePhaseEventTest {

    @Test
    void testBootPhaseStageOrderAndWorldAccessibility() {
        assertEquals(0, VesselServerBootPhase.UNINITIALIZED.getStageOrder());
        assertFalse(VesselServerBootPhase.UNINITIALIZED.isWorldAccessible());

        assertEquals(1, VesselServerBootPhase.PRE_MAIN_MOD_INIT.getStageOrder());
        assertFalse(VesselServerBootPhase.PRE_MAIN_MOD_INIT.isWorldAccessible());

        assertEquals(2, VesselServerBootPhase.VANILLA_BOOTSTRAP_ACTIVE.getStageOrder());
        assertFalse(VesselServerBootPhase.VANILLA_BOOTSTRAP_ACTIVE.isWorldAccessible());

        assertEquals(3, VesselServerBootPhase.SERVER_STARTING.getStageOrder());
        assertTrue(VesselServerBootPhase.SERVER_STARTING.isWorldAccessible());

        assertEquals(4, VesselServerBootPhase.SERVER_STARTED_POST_WORLD.getStageOrder());
        assertTrue(VesselServerBootPhase.SERVER_STARTED_POST_WORLD.isWorldAccessible());

        assertEquals(5, VesselServerBootPhase.SERVER_STOPPING.getStageOrder());
        assertFalse(VesselServerBootPhase.SERVER_STOPPING.isWorldAccessible());
    }

    @Test
    void testBootPhaseIsAfterComparison() {
        assertTrue(VesselServerBootPhase.SERVER_STARTED_POST_WORLD.isAfter(VesselServerBootPhase.SERVER_STARTING));
        assertTrue(VesselServerBootPhase.PRE_MAIN_MOD_INIT.isAfter(VesselServerBootPhase.UNINITIALIZED));
        assertFalse(VesselServerBootPhase.UNINITIALIZED.isAfter(VesselServerBootPhase.PRE_MAIN_MOD_INIT));
        assertFalse(VesselServerBootPhase.SERVER_STARTING.isAfter(VesselServerBootPhase.SERVER_STARTING));
        assertThrows(
                NullPointerException.class,
                () -> VesselServerBootPhase.UNINITIALIZED.isAfter(null));
    }

    @Test
    void testBootPhaseFromPhaseNameLookup() {
        assertEquals(
                VesselServerBootPhase.SERVER_STARTED_POST_WORLD,
                VesselServerBootPhase.fromPhaseName("SERVER_STARTED_POST_WORLD"));
        assertEquals(
                VesselServerBootPhase.VANILLA_BOOTSTRAP_ACTIVE,
                VesselServerBootPhase.fromPhaseName("  vanilla_bootstrap_active  "));
        assertEquals(
                VesselServerBootPhase.UNINITIALIZED,
                VesselServerBootPhase.fromPhaseName("UNKNOWN_PHASE_NAME"));
        assertEquals(
                VesselServerBootPhase.UNINITIALIZED,
                VesselServerBootPhase.fromPhaseName("   "));
        assertEquals(
                VesselServerBootPhase.UNINITIALIZED,
                VesselServerBootPhase.fromPhaseName(null));
    }

    @Test
    void testLifecyclePhaseEventPropertiesAndTransitions() {
        VesselLifecyclePhaseEvent forwardEvent = new VesselLifecyclePhaseEvent(
                VesselServerBootPhase.VANILLA_BOOTSTRAP_ACTIVE,
                VesselServerBootPhase.SERVER_STARTING,
                "1.21.1",
                1730000000000L);

        assertEquals(VesselServerBootPhase.VANILLA_BOOTSTRAP_ACTIVE, forwardEvent.getPreviousPhase());
        assertEquals(VesselServerBootPhase.SERVER_STARTING, forwardEvent.getCurrentPhase());
        assertEquals("1.21.1", forwardEvent.getActiveMinecraftVersion());
        assertEquals(1730000000000L, forwardEvent.getTimestampEpochMillis());
        assertTrue(forwardEvent.isForwardTransition());

        VesselLifecyclePhaseEvent sameStageEvent = new VesselLifecyclePhaseEvent(
                VesselServerBootPhase.SERVER_STARTING,
                VesselServerBootPhase.SERVER_STARTING,
                "1.21.1",
                1730000000100L);
        assertTrue(sameStageEvent.isForwardTransition());

        VesselLifecyclePhaseEvent rollbackEvent = new VesselLifecyclePhaseEvent(
                VesselServerBootPhase.SERVER_STOPPING,
                VesselServerBootPhase.UNINITIALIZED,
                "1.21.1");
        assertFalse(rollbackEvent.isForwardTransition());
        assertTrue(rollbackEvent.getTimestampEpochMillis() > 0L);
    }

    @Test
    void testLifecyclePhaseEventValidationAndEquality() {
        assertThrows(
                NullPointerException.class,
                () -> new VesselLifecyclePhaseEvent(
                        null,
                        VesselServerBootPhase.PRE_MAIN_MOD_INIT,
                        "1.21.1",
                        1000L));
        assertThrows(
                NullPointerException.class,
                () -> new VesselLifecyclePhaseEvent(
                        VesselServerBootPhase.UNINITIALIZED,
                        null,
                        "1.21.1",
                        1000L));
        assertThrows(
                NullPointerException.class,
                () -> new VesselLifecyclePhaseEvent(
                        VesselServerBootPhase.UNINITIALIZED,
                        VesselServerBootPhase.PRE_MAIN_MOD_INIT,
                        null,
                        1000L));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselLifecyclePhaseEvent(
                        VesselServerBootPhase.UNINITIALIZED,
                        VesselServerBootPhase.PRE_MAIN_MOD_INIT,
                        "   ",
                        1000L));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselLifecyclePhaseEvent(
                        VesselServerBootPhase.UNINITIALIZED,
                        VesselServerBootPhase.PRE_MAIN_MOD_INIT,
                        "1.21.1",
                        -1L));

        VesselLifecyclePhaseEvent first = new VesselLifecyclePhaseEvent(
                VesselServerBootPhase.PRE_MAIN_MOD_INIT,
                VesselServerBootPhase.VANILLA_BOOTSTRAP_ACTIVE,
                "1.21.1",
                5000L);
        VesselLifecyclePhaseEvent second = new VesselLifecyclePhaseEvent(
                VesselServerBootPhase.PRE_MAIN_MOD_INIT,
                VesselServerBootPhase.VANILLA_BOOTSTRAP_ACTIVE,
                "1.21.1",
                5000L);
        VesselLifecyclePhaseEvent different = new VesselLifecyclePhaseEvent(
                VesselServerBootPhase.PRE_MAIN_MOD_INIT,
                VesselServerBootPhase.SERVER_STARTING,
                "1.21.1",
                5000L);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, different);
        assertTrue(first.toString().contains("VANILLA_BOOTSTRAP_ACTIVE"));
    }
}
