package dev.vessel.api.actor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JUnit 5 test suite validating the Regional Actor Model Concurrency, MPSC Mailbox,
 * Timeout Guard circuit breaker, and Global Region Sync Barrier.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class RegionalActorStrandTest {

    private RegionalActorStrand strandA;
    private RegionalActorStrand strandB;

    @BeforeEach
    void setUp() {
        strandA = new RegionalActorStrand(0, 0);
        strandB = new RegionalActorStrand(1, 1);
    }

    @AfterEach
    void tearDown() {
        if (strandA != null) {
            strandA.close();
        }
        if (strandB != null) {
            strandB.close();
        }
    }

    @Test
    @DisplayName("Actor Tell pattern (post) executes asynchronously on strand thread")
    void testTellPatternExecution() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean onCorrectThread = new AtomicBoolean(false);

        boolean enqueued = strandA.post(() -> {
            onCorrectThread.set(strandA.isCurrentStrand());
            latch.countDown();
        });

        assertThat(enqueued).isTrue();
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(onCorrectThread.get()).isTrue();
        assertThat(strandA.getProcessedTaskCount()).isGreaterThanOrEqualTo(1L);
    }

    @Test
    @DisplayName("Actor Ask pattern (ask) successfully replies with computed value")
    void testAskPatternSuccessfulReply() throws Exception {
        CompletableFuture<String> future = strandA.ask(() -> "vessel-region-0-0-ready");

        String result = future.get(5, TimeUnit.SECONDS);
        assertThat(result).isEqualTo("vessel-region-0-0-ready");
    }

    @Test
    @DisplayName("Cross-region communication between two independent actor strands")
    void testCrossRegionCommunicationBetweenTwoStrands() throws Exception {
        CompletableFuture<Integer> computation = strandA.ask(() -> {
            // Strand A queries Strand B for a block state value
            CompletableFuture<Integer> bQuery = strandB.ask(() -> 42);
            try {
                return bQuery.get(3, TimeUnit.SECONDS) * 2;
            } catch (Exception e) {
                return -1;
            }
        });

        int finalResult = computation.get(5, TimeUnit.SECONDS);
        assertThat(finalResult).isEqualTo(84);
    }

    @Test
    @DisplayName("Timeout guard enforces execution deadline and completes exceptionally")
    void testTimeoutGuardEnforcesDeadline() {
        CrossRegionAskTimeoutGuard guard = new CrossRegionAskTimeoutGuard(20L, 3, 500L);
        try (RegionalActorStrand guardedStrand = new RegionalActorStrand(2, 2, guard)) {
            CompletableFuture<String> stalledFuture = guardedStrand.ask(() -> {
                try {
                    Thread.sleep(150L);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                return "completed-late";
            }, 20L);

            assertThatThrownBy(() -> stalledFuture.get(1, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(TimeoutException.class);

            assertThat(guard.getTotalTimeoutCount()).isGreaterThanOrEqualTo(1L);
        }
    }

    @Test
    @DisplayName("Circuit breaker trips open after consecutive timeouts")
    void testCircuitBreakerTripping() {
        CrossRegionAskTimeoutGuard guard = new CrossRegionAskTimeoutGuard(10L, 3, 2000L);

        for (int i = 0; i < 3; i++) {
            guard.recordTimeout();
        }

        assertThat(guard.isCircuitOpen()).isTrue();
        assertThat(guard.getTotalCircuitTrips()).isEqualTo(1L);

        CompletableFuture<String> rejected = new CompletableFuture<>();
        guard.guard(rejected);

        assertThatThrownBy(() -> rejected.get(100, TimeUnit.MILLISECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("GlobalRegionSyncBarrier direct path when called on current strand")
    void testGlobalRegionSyncBarrierDirectPath() throws Exception {
        GlobalRegionSyncBarrier barrier = new GlobalRegionSyncBarrier();

        CompletableFuture<String> runOnStrand = strandA.ask(() -> {
            return barrier.syncCall(strandA, () -> "direct-fast-path", "fallback");
        });

        String result = runOnStrand.get(2, TimeUnit.SECONDS);
        assertThat(result).isEqualTo("direct-fast-path");
        assertThat(barrier.getTotalDirectInvocations()).isEqualTo(1L);
    }

    @Test
    @DisplayName("GlobalRegionSyncBarrier returns fallback on timeout without throwing IllegalStateException")
    void testGlobalRegionSyncBarrierFallbackOnTimeout() {
        GlobalRegionSyncBarrier barrier = new GlobalRegionSyncBarrier();

        String result = barrier.syncCall(
                strandA,
                () -> {
                    try {
                        Thread.sleep(100L);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                    return "unreachable-success";
                },
                "safe-fallback-state",
                15L // 15ms deadline
        );

        assertThat(result).isEqualTo("safe-fallback-state");
        assertThat(barrier.getTotalFallbackInvocations()).isGreaterThanOrEqualTo(1L);
    }

    @Test
    @DisplayName("LockFreeMailboxRingBuffer concurrent MPSC throughput test")
    void testLockFreeMailboxRingBufferConcurrency() throws InterruptedException {
        LockFreeMailboxRingBuffer<Integer> ringBuffer = new LockFreeMailboxRingBuffer<>(1024);
        int producerCount = 4;
        int itemsPerProducer = 200;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(producerCount);

        for (int p = 0; p < producerCount; p++) {
            final int producerId = p;
            Thread producer = new Thread(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < itemsPerProducer; i++) {
                        while (!ringBuffer.offer(producerId * 1000 + i)) {
                            Thread.yield();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
            producer.start();
        }

        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);

        AtomicInteger drainedCount = new AtomicInteger(0);
        int totalExpected = producerCount * itemsPerProducer;

        while (drainedCount.get() < totalExpected) {
            int batch = ringBuffer.drain(item -> drainedCount.incrementAndGet(), 64);
            if (batch == 0) {
                Thread.yield();
            }
        }

        assertThat(drainedCount.get()).isEqualTo(totalExpected);
        assertThat(ringBuffer.isEmpty()).isTrue();
    }
}
