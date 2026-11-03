package dev.vessel.api.event;

/**
 * Event fixture for verifying stack bitmask and event bus cancellation handling.
 *
 * @version 1.0.0
 */
public final class TestCancellableEvent {

    private final String payload;
    private boolean cancelled;

    public TestCancellableEvent(String payload) {
        this.payload = payload;
        this.cancelled = false;
    }

    public String getPayload() {
        return payload;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
