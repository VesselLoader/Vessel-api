package dev.vessel.api.event;

/**
 * High-speed subscriber fixture for verifying dispatch invocation and counts.
 *
 * @version 1.0.0
 */
public final class TestSpeedListener {

    private long receivedEvents;

    public TestSpeedListener() {
        this.receivedEvents = 0;
    }

    public void onSpeedEvent(TestSpeedEvent event) {
        this.receivedEvents++;
        event.incrementSequence();
    }

    public long getReceivedEvents() {
        return receivedEvents;
    }

    public void reset() {
        this.receivedEvents = 0;
    }
}
