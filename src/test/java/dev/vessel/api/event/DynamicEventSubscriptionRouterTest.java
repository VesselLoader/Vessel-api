package dev.vessel.api.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verification test suite for DynamicEventSubscriptionRouter.
 * Tests Forge @SubscribeEvent, Bukkit @EventHandler, and Fabric callback registrations.
 * Strictly adheres to Rule 17 with zero inner/nested classes.
 *
 * @version 1.0.1
 */
public final class DynamicEventSubscriptionRouterTest {

    private SovereignEventBus eventBus;
    private DynamicEventSubscriptionRouter router;

    @BeforeEach
    public void setUp() {
        this.eventBus = new SovereignEventBus();
        this.router = new DynamicEventSubscriptionRouter(eventBus);
    }

    @Test
    @DisplayName("Forge @SubscribeEvent reflection discovery and dispatch")
    public void testForgeSubscriberRegistration() {
        TestForgeSubscriber subscriber = new TestForgeSubscriber();
        router.registerSyntheticObject(subscriber);

        assertThat(eventBus.hasListeners(TestSpeedEvent.class)).isTrue();

        TestSpeedEvent event = new TestSpeedEvent(10L, 0L);
        boolean canceled = router.dispatchDynamicEvent(event);

        assertThat(canceled).isFalse();
        assertThat(subscriber.getCallCount()).isEqualTo(1);
        assertThat(subscriber.getLastEvent()).isSameAs(event);
    }

    @Test
    @DisplayName("Bukkit @EventHandler reflection discovery and dispatch")
    public void testBukkitListenerRegistration() {
        TestBukkitSubscriber listener = new TestBukkitSubscriber();
        router.registerBukkitListener(listener, "TestPlugin");

        assertThat(eventBus.hasListeners(TestSpeedEvent.class)).isTrue();

        TestSpeedEvent event = new TestSpeedEvent(20L, 0L);
        router.dispatchBukkitEvent(event);

        assertThat(listener.getCallCount()).isEqualTo(1);
        assertThat(listener.getLastEvent()).isSameAs(event);
    }

    @Test
    @DisplayName("Fabric / Quilt functional consumer callback registration and dispatch")
    public void testFabricCallbackRegistration() {
        AtomicInteger counter = new AtomicInteger(0);
        Consumer<TestSpeedEvent> callback = ev -> counter.incrementAndGet();

        router.registerFunctionalListener(TestSpeedEvent.class, callback);

        assertThat(eventBus.hasListeners(TestSpeedEvent.class)).isTrue();

        TestSpeedEvent event = new TestSpeedEvent(30L, 0L);
        eventBus.dispatch(event);

        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Unified registration discovers both Forge and Bukkit subscribers")
    public void testUnifiedRouteSubscription() {
        TestForgeSubscriber forgeSub = new TestForgeSubscriber();
        TestBukkitSubscriber bukkitSub = new TestBukkitSubscriber();

        router.registerSyntheticObject(forgeSub);
        router.registerSyntheticObject(bukkitSub);

        assertThat(eventBus.hasListeners(TestSpeedEvent.class)).isTrue();

        TestSpeedEvent event = new TestSpeedEvent(40L, 0L);
        router.dispatchDynamicEvent(event);

        assertThat(forgeSub.getCallCount()).isEqualTo(1);
        assertThat(bukkitSub.getCallCount()).isEqualTo(1);
    }
}
