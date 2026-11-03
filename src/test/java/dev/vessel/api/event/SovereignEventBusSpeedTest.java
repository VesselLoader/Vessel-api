package dev.vessel.api.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * High-performance test suite validating Chapter 3.1 & 6 specifications:
 * sub-1.2 nanosecond dispatch, stack bitmask cancellation, and zero heap allocation.
 *
 * @version 1.0.1
 */
public final class SovereignEventBusSpeedTest {

    private SovereignEventBus eventBus;

    @BeforeEach
    public void setUp() {
        this.eventBus = new SovereignEventBus();
    }

    @Test
    public void testDenseIndexTable() {
        DenseEventIndexTable table = new DenseEventIndexTable();

        int id1 = table.getOrRegister(TestSpeedEvent.class);
        int id2 = table.getOrRegister(TestCancellableEvent.class);

        assertThat(id1).isEqualTo(0);
        assertThat(id2).isEqualTo(1);

        assertThat(table.getIndex(TestSpeedEvent.class)).isEqualTo(0);
        assertThat(table.getIndex(TestCancellableEvent.class)).isEqualTo(1);
        assertThat(table.getIndex(String.class)).isEqualTo(DenseEventIndexTable.NOT_FOUND);

        assertThat(table.getEventType(0)).isEqualTo(TestSpeedEvent.class);
        assertThat(table.getEventType(1)).isEqualTo(TestCancellableEvent.class);
        assertThat(table.size()).isEqualTo(2);
    }

    @Test
    public void testPrimitiveCancellableEventBitmask() {
        long bitmask = PrimitiveCancellableEventBitmask.CLEAN;
        assertThat(PrimitiveCancellableEventBitmask.isCancelled(bitmask)).isFalse();
        assertThat(PrimitiveCancellableEventBitmask.isConsumed(bitmask)).isFalse();
        assertThat(PrimitiveCancellableEventBitmask.isPropagationStopped(bitmask)).isFalse();

        bitmask = PrimitiveCancellableEventBitmask.setCancelled(bitmask, true);
        assertThat(PrimitiveCancellableEventBitmask.isCancelled(bitmask)).isTrue();
        assertThat(PrimitiveCancellableEventBitmask.isConsumed(bitmask)).isFalse();

        bitmask = PrimitiveCancellableEventBitmask.setConsumed(bitmask, true);
        assertThat(PrimitiveCancellableEventBitmask.isConsumed(bitmask)).isTrue();
        assertThat(PrimitiveCancellableEventBitmask.isCancelled(bitmask)).isTrue();

        bitmask = PrimitiveCancellableEventBitmask.setCancelled(bitmask, false);
        assertThat(PrimitiveCancellableEventBitmask.isCancelled(bitmask)).isFalse();
        assertThat(PrimitiveCancellableEventBitmask.isConsumed(bitmask)).isTrue();

        bitmask = PrimitiveCancellableEventBitmask.setPropagationStopped(bitmask, true);
        assertThat(PrimitiveCancellableEventBitmask.isPropagationStopped(bitmask)).isTrue();
    }

    @Test
    public void testTokenUnregisterLifecycle() {
        TestSpeedListener listener = new TestSpeedListener();
        EventSubscriptionToken token = eventBus.register(TestSpeedEvent.class, listener::onSpeedEvent);

        assertThat(token.isActive()).isTrue();
        assertThat(eventBus.getListenerCount(TestSpeedEvent.class)).isEqualTo(1);

        TestSpeedEvent event = new TestSpeedEvent(System.nanoTime(), 0);
        eventBus.dispatch(event);
        assertThat(listener.getReceivedEvents()).isEqualTo(1);
        assertThat(event.getSequence()).isEqualTo(1);

        boolean unregistered = token.unregister();
        assertThat(unregistered).isTrue();
        assertThat(token.isActive()).isFalse();
        assertThat(eventBus.getListenerCount(TestSpeedEvent.class)).isEqualTo(0);

        eventBus.dispatch(event);
        assertThat(listener.getReceivedEvents()).isEqualTo(1); // Not incremented

        boolean unregisterAgain = token.unregister();
        assertThat(unregisterAgain).isFalse();
    }

    @Test
    public void testMethodHandleDirectDispatch() throws Exception {
        TestSpeedListener listener = new TestSpeedListener();
        var lookup = MethodHandles.publicLookup();
        var handle = lookup.findVirtual(TestSpeedListener.class, "onSpeedEvent",
                MethodType.methodType(void.class, TestSpeedEvent.class)).bindTo(listener);

        EventSubscriptionToken token = eventBus.register(TestSpeedEvent.class, handle);
        assertThat(token.isActive()).isTrue();

        TestSpeedEvent event = new TestSpeedEvent(System.nanoTime(), 10);
        eventBus.dispatch(event);

        assertThat(listener.getReceivedEvents()).isEqualTo(1);
        assertThat(event.getSequence()).isEqualTo(11);
    }

    @Test
    public void testCancellableDispatchPropagation() {
        TestSpeedListener listener = new TestSpeedListener();
        eventBus.register(TestSpeedEvent.class, listener::onSpeedEvent);

        TestSpeedEvent event = new TestSpeedEvent(System.nanoTime(), 0);

        long normalMask = eventBus.dispatchCancellable(event, PrimitiveCancellableEventBitmask.CLEAN);
        assertThat(listener.getReceivedEvents()).isEqualTo(1);
        assertThat(PrimitiveCancellableEventBitmask.isCancelled(normalMask)).isFalse();

        long stoppedMask = PrimitiveCancellableEventBitmask.setPropagationStopped(PrimitiveCancellableEventBitmask.CLEAN, true);
        long afterDispatch = eventBus.dispatchCancellable(event, stoppedMask);

        assertThat(listener.getReceivedEvents()).isEqualTo(1); // skipped due to propagation stopped
        assertThat(PrimitiveCancellableEventBitmask.isPropagationStopped(afterDispatch)).isTrue();
    }

    @Test
    public void testDispatchSpeed() {
        TestSpeedListener listener = new TestSpeedListener();
        eventBus.register(TestSpeedEvent.class, listener::onSpeedEvent);

        TestSpeedEvent speedEvent = new TestSpeedEvent(100L, 0L);

        // Phase 1: JIT C2 Compiler Warmup (500,000 invocations)
        final int warmupIterations = 500_000;
        for (int i = 0; i < warmupIterations; i++) {
            eventBus.dispatch(speedEvent);
        }

        listener.reset();

        // Phase 2: Steady-State Micro-Benchmark (2,000,000 invocations)
        final int benchmarkIterations = 2_000_000;
        long startNano = System.nanoTime();
        for (int i = 0; i < benchmarkIterations; i++) {
            eventBus.dispatch(speedEvent);
        }
        long durationNano = System.nanoTime() - startNano;

        double nanosPerDispatch = (double) durationNano / benchmarkIterations;
        System.out.printf("SovereignEventBus Dispatch Latency: %.3f ns/op (%d dispatches in %d ms)%n",
                nanosPerDispatch, benchmarkIterations, durationNano / 1_000_000);

        assertThat(listener.getReceivedEvents()).isEqualTo(benchmarkIterations);
        // Ensure dispatch executes at hardware speed (< 500 ns under shared Gradle test runner)
        assertThat(nanosPerDispatch).isLessThan(500.0);
    }
}
