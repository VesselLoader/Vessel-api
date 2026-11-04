package dev.vessel.api.event;

/**
 * Top-level listener class for verifying {@link VesselSubscribe} reception.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 */
public final class TestVesselEventListener {

    private boolean invoked = false;
    private String receivedMessage = null;

    @VesselSubscribe(priority = VesselEventPriority.HIGH)
    public void onPulse(TestVesselPulseEvent event) {
        this.invoked = true;
        this.receivedMessage = event.getMessage();
    }

    public boolean isInvoked() {
        return invoked;
    }

    public String getReceivedMessage() {
        return receivedMessage;
    }
}
