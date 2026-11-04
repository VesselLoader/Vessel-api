package dev.vessel.api.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verification test suite for {@link VesselSubscribe} and {@link DynamicEventSubscriptionRouter}.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 */
public final class VesselEventBusSubscribeTest {

    @Test
    @DisplayName("Verify @VesselSubscribe registers and dispatches via SovereignEventBus")
    void verifyVesselSubscribeReception() {
        TestVesselEventListener listener = new TestVesselEventListener();
        DynamicEventSubscriptionRouter.getInstance().registerSyntheticObject(listener);

        TestVesselPulseEvent event = new TestVesselPulseEvent("VESSEL_PULSE_OK");
        DynamicEventSubscriptionRouter.getInstance().dispatchDynamicEvent(event);

        assertThat(listener.isInvoked()).isTrue();
        assertThat(listener.getReceivedMessage()).isEqualTo("VESSEL_PULSE_OK");
    }
}
