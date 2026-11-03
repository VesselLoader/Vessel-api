package dev.vessel.api.event;

/**
 * Test fixture representing a Forge mod class using @SubscribeEvent.
 * Strictly adheres to Rule 17 as an independent top-level class.
 */
public final class TestForgeSubscriber {

    private int callCount;
    private TestSpeedEvent lastEvent;

    public TestForgeSubscriber() {
        this.callCount = 0;
        this.lastEvent = null;
    }

    @SubscribeEvent
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
