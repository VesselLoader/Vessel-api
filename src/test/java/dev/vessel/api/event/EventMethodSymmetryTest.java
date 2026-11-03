package dev.vessel.api.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verification test suite for Wave 2 Event Symmetry facades:
 * {@link SyntheticEventMethodHandleTable}, {@link DynamicEventSubscriptionRouter}, and {@link EventCancellationStackBridge}.
 *
 * @version 1.0.0
 */
public final class EventMethodSymmetryTest {

    private SovereignEventBus eventBus;
    private DynamicEventSubscriptionRouter router;

    @BeforeEach
    public void setUp() {
        this.eventBus = new SovereignEventBus();
        this.router = new DynamicEventSubscriptionRouter(eventBus);
    }

    @Test
    public void testEventCancellationStackBridgeBitmask() {
        long mask = EventCancellationStackBridge.CLEAN;
        assertThat(EventCancellationStackBridge.isCancelled(mask)).isFalse();
        assertThat(EventCancellationStackBridge.isConsumed(mask)).isFalse();
        assertThat(EventCancellationStackBridge.isPropagationStopped(mask)).isFalse();
        assertThat(EventCancellationStackBridge.isDenied(mask)).isFalse();
        assertThat(EventCancellationStackBridge.isAllowed(mask)).isFalse();

        mask = EventCancellationStackBridge.setCancelled(mask, true);
        assertThat(EventCancellationStackBridge.isCancelled(mask)).isTrue();

        mask = EventCancellationStackBridge.setConsumed(mask, true);
        assertThat(EventCancellationStackBridge.isConsumed(mask)).isTrue();

        mask = EventCancellationStackBridge.setPropagationStopped(mask, true);
        assertThat(EventCancellationStackBridge.isPropagationStopped(mask)).isTrue();

        mask = EventCancellationStackBridge.setDenied(mask, true);
        assertThat(EventCancellationStackBridge.isDenied(mask)).isTrue();

        mask = EventCancellationStackBridge.setAllowed(mask, true);
        assertThat(EventCancellationStackBridge.isAllowed(mask)).isTrue();

        mask = EventCancellationStackBridge.setDefault(mask, true);
        assertThat(EventCancellationStackBridge.isDefault(mask)).isTrue();

        // Reset cancellation
        mask = EventCancellationStackBridge.setCancelled(mask, false);
        assertThat(EventCancellationStackBridge.isCancelled(mask)).isFalse();
    }

    @Test
    public void testEventCancellationStackBridgeObjectBridge() {
        TestCancellableEvent event = new TestCancellableEvent("test-payload");
        assertThat(EventCancellationStackBridge.isCanceled(event)).isFalse();

        EventCancellationStackBridge.setCanceled(event, true);
        assertThat(EventCancellationStackBridge.isCanceled(event)).isTrue();
        assertThat(event.isCancelled()).isTrue();

        EventCancellationStackBridge.setCanceled(event, false);
        assertThat(EventCancellationStackBridge.isCanceled(event)).isFalse();
        assertThat(event.isCancelled()).isFalse();
    }

    @Test
    public void testSyntheticEventMethodHandleTable() throws Throwable {
        SyntheticEventMethodHandleTable table = new SyntheticEventMethodHandleTable(32);
        assertThat(table.capacity()).isGreaterThanOrEqualTo(32);
        assertThat(table.size()).isEqualTo(0);

        TestSpeedListener listener = new TestSpeedListener();
        var lookup = MethodHandles.publicLookup();
        var rawHandle = lookup.findVirtual(TestSpeedListener.class, "onSpeedEvent",
                MethodType.methodType(void.class, TestSpeedEvent.class)).bindTo(listener);
        var adapted = rawHandle.asType(MethodType.methodType(void.class, Object.class));

        int eventId = 5;
        table.setHandle(eventId, adapted);
        assertThat(table.size()).isEqualTo(1);
        assertThat(table.getHandle(eventId)).isNotNull();

        TestSpeedEvent event = new TestSpeedEvent(12345L, 0L);
        table.invokeExact(eventId, (Object) event);
        assertThat(listener.getReceivedEvents()).isEqualTo(1);

        table.clear();
        assertThat(table.size()).isEqualTo(0);
        assertThat(table.getHandle(eventId)).isNull();
    }

    @Test
    public void testDynamicEventSubscriptionRouterForgeSymmetry() {
        TestForgeListener forgeListener = new TestForgeListener();
        router.registerSyntheticObject(forgeListener);

        assertThat(eventBus.getListenerCount(TestSpeedEvent.class)).isGreaterThanOrEqualTo(1);

        TestSpeedEvent event = new TestSpeedEvent(100L, 0L);
        boolean isCanceled = router.dispatchDynamicEvent(event);

        assertThat(isCanceled).isFalse();
        assertThat(forgeListener.getEventCount()).isEqualTo(1);
    }

    @Test
    public void testDynamicEventSubscriptionRouterFabricSymmetry() {
        AtomicInteger counter = new AtomicInteger();
        router.registerFunctionalListener(TestSpeedEvent.class, (java.util.function.Consumer<TestSpeedEvent>) ev -> counter.incrementAndGet());

        TestSpeedEvent event = new TestSpeedEvent(200L, 0L);
        eventBus.dispatch(event);

        assertThat(counter.get()).isEqualTo(1);

        // Test invoker resolution
        Object invoker = router.resolveEventInvoker(TestFabricEventCallback.class);
        assertThat(invoker).isInstanceOf(TestFabricEventCallback.class);

        TestFabricEventCallback callback = (TestFabricEventCallback) invoker;
        callback.onEvent(event);
        assertThat(counter.get()).isEqualTo(2);
    }

    @Test
    public void testDynamicEventSubscriptionRouterBukkitSymmetry() {
        TestForgeListener bukkitListener = new TestForgeListener();
        router.registerBukkitListener(bukkitListener, "TestPlugin");

        TestSpeedEvent event = new TestSpeedEvent(300L, 0L);
        router.dispatchBukkitEvent(event);

        assertThat(bukkitListener.getEventCount()).isEqualTo(1);
    }

    @Test
    public void testDynamicDispatchCancellableReturn() {
        TestCancellableEvent cancellableEvent = new TestCancellableEvent("blocked");
        eventBus.register(TestCancellableEvent.class, (TestCancellableEvent ev) -> ev.setCancelled(true));

        boolean wasCanceled = router.dispatchDynamicEvent(cancellableEvent);
        assertThat(wasCanceled).isTrue();
    }
}
