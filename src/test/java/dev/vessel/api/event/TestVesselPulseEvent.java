package dev.vessel.api.event;

/**
 * Test event class for verifying {@link VesselSubscribe} handling.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 */
public final class TestVesselPulseEvent implements VesselCancellable {

    private final String message;
    private boolean cancelled;

    public TestVesselPulseEvent(String message) {
        this.message = message;
        this.cancelled = false;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
