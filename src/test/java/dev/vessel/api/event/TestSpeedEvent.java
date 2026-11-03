package dev.vessel.api.event;

/**
 * Benchmark payload event for hardware-speed event dispatch measurement.
 *
 * @version 1.0.0
 */
public final class TestSpeedEvent {

    private final long timestamp;
    private long sequence;

    public TestSpeedEvent(long timestamp, long sequence) {
        this.timestamp = timestamp;
        this.sequence = sequence;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getSequence() {
        return sequence;
    }

    public void incrementSequence() {
        this.sequence++;
    }
}
