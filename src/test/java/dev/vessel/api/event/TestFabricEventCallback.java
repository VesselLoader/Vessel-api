package dev.vessel.api.event;

/**
 * Functional callback interface fixture for testing Fabric Event.invoker() proxy symmetry.
 *
 * @version 1.0.0
 */
public interface TestFabricEventCallback {

    void onEvent(TestSpeedEvent event);
}
