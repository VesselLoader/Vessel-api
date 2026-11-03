package dev.vessel.api.event;

/**
 * Listener fixture simulating Forge / Bukkit reflective subscriber object.
 *
 * @version 1.0.0
 */
public final class TestForgeListener {

    private int eventCount;

    public TestForgeListener() {
        this.eventCount = 0;
    }

    public void handleSpeedEvent(TestSpeedEvent event) {
        this.eventCount++;
    }

    public int getEventCount() {
        return eventCount;
    }

    public void reset() {
        this.eventCount = 0;
    }
}
