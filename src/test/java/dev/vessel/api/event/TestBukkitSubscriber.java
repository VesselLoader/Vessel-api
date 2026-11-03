package dev.vessel.api.event;

/**
 * Test fixture representing a Bukkit plugin listener using @EventHandler.
 * Strictly adheres to Rule 17 as an independent top-level class.
 */
public final class TestBukkitSubscriber {

    private int callCount;
    private TestSpeedEvent lastEvent;

    public TestBukkitSubscriber() {
        this.callCount = 0;
        this.lastEvent = null;
    }

    @EventHandler
    public void onEvent(TestSpeedEvent event) {
        this.callCount++;
        this.lastEvent = event;
    }

    public int getCallCount() {
        return this.callCount;
    }

    public TestSpeedEvent getLastEvent() {
        return this.lastEvent;
    }
}
