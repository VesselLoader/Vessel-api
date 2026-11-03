package dev.vessel.api.actor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive JUnit 5 test suite validating Domain 3 (Regional Concurrency & Scheduling Symmetry):
 * FoliaRegionExecutionBridge, BukkitSchedulerSymmetryBridge, and NUMA-aware ActorMailboxPaddedRingQueue.
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public class ActorMethodSymmetryTest {

    private FoliaRegionExecutionBridge foliaBridge;
    private BukkitSchedulerSymmetryBridge bukkitBridge;
    private ActorMailboxPaddedRingQueue<String> paddedQueue;

    @BeforeEach
    void setUp() {
        foliaBridge = new FoliaRegionExecutionBridge(4);
        bukkitBridge = new BukkitSchedulerSymmetryBridge(2, 4);
        paddedQueue = new ActorMailboxPaddedRingQueue<>(512);
    }

    @AfterEach
    void tearDown() {
        if (foliaBridge != null) {
            foliaBridge.close();
        }
        if (bukkitBridge != null) {
            bukkitBridge.close();
        }
    }

    // =========================================================================
    // 1. ACTOR MAILBOX PADDED RING QUEUE (NUMA / FALSE-SHARING IMMUNITY)
    // =========================================================================

    @Test
    @DisplayName("ActorMailboxPaddedRingQueue basic FIFO offer, poll, and metrics")
    void testPaddedRingQueueBasicFifo() {
        assertThat(paddedQueue.isEmpty()).isTrue();
        assertThat(paddedQueue.capacity()).isGreaterThanOrEqualTo(512);

        boolean offered1 = paddedQueue.offer("msg-1");
        boolean offered2 = paddedQueue.offer("msg-2");

        assertThat(offered1).isTrue();
        assertThat(offered2).isTrue();
        assertThat(paddedQueue.size()).isEqualTo(2);

        String poll1 = paddedQueue.poll();
        String poll2 = paddedQueue.poll();
        String poll3 = paddedQueue.poll();

        assertThat(poll1).isEqualTo("msg-1");
        assertThat(poll2).isEqualTo("msg-2");
        assertThat(poll3).isNull();
        assertThat(paddedQueue.isEmpty()).isTrue();
        assertThat(paddedQueue.getTotalEnqueuedCount()).isEqualTo(2L);
        assertThat(paddedQueue.getTotalDequeuedCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("ActorMailboxPaddedRingQueue batch draining")
    void testPaddedRingQueueBatchDrain() {
        for (int i = 0; i < 10; i++) {
            paddedQueue.offer("item-" + i);
        }

        AtomicInteger drainedCount = new AtomicInteger(0);
        int drained = paddedQueue.drain(item -> drainedCount.incrementAndGet(), 5);

        assertThat(drained).isEqualTo(5);
        assertThat(drainedCount.get()).isEqualTo(5);
        assertThat(paddedQueue.size()).isEqualTo(5);

        paddedQueue.clear();
        assertThat(paddedQueue.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("ActorMailboxPaddedRingQueue high-throughput concurrent MPSC execution")
    void testPaddedRingQueueConcurrentMpsc() throws InterruptedException {
        int producerCount = 4;
        int itemsPerProducer = 100;
        int totalExpected = producerCount * itemsPerProducer;

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch producersDone = new CountDownLatch(producerCount);

        for (int p = 0; p < producerCount; p++) {
            final int producerId = p;
            Thread producer = new Thread(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < itemsPerProducer; i++) {
                        while (!paddedQueue.offer("producer-" + producerId + "-item-" + i)) {
                            Thread.yield();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    producersDone.countDown();
                }
            });
            producer.start();
        }

        startLatch.countDown();
        assertThat(producersDone.await(5, TimeUnit.SECONDS)).isTrue();

        AtomicInteger totalReceived = new AtomicInteger(0);
        while (totalReceived.get() < totalExpected) {
            int batch = paddedQueue.drain(item -> totalReceived.incrementAndGet(), 64);
            if (batch == 0) {
                Thread.yield();
            }
        }

        assertThat(totalReceived.get()).isEqualTo(totalExpected);
        assertThat(paddedQueue.isEmpty()).isTrue();
    }

    // =========================================================================
    // 2. FOLIA REGION EXECUTION BRIDGE
    // =========================================================================

    @Test
    @DisplayName("FoliaRegionExecutionBridge executeInRegion executes on dedicated regional strand")
    void testFoliaExecuteInRegion() throws InterruptedException {
        String mockPlugin = "TestFoliaPlugin";
        String mockWorld = "world_the_end";
        int chunkX = 32; // Region 2
        int chunkZ = 48; // Region 3

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean ran = new AtomicBoolean(false);

        Object taskId = foliaBridge.executeInRegion(mockPlugin, mockWorld, chunkX, chunkZ, () -> {
            ran.set(true);
            latch.countDown();
        });

        assertThat(taskId).isNotNull();
        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(ran.get()).isTrue();
    }

    @Test
    @DisplayName("FoliaRegionExecutionBridge executeInRegionDelayed executes after tick delay")
    void testFoliaExecuteInRegionDelayed() throws InterruptedException {
        String mockPlugin = "TestFoliaPlugin";
        String mockWorld = "world";
        CountDownLatch latch = new CountDownLatch(1);
        long startNanos = System.nanoTime();
        AtomicLong elapsedMillis = new AtomicLong(0L);

        Object taskId = foliaBridge.executeInRegionDelayed(mockPlugin, mockWorld, 0, 0, () -> {
            elapsedMillis.set(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
            latch.countDown();
        }, 1L); // 1 tick = 50ms

        assertThat(taskId).isNotNull();
        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(elapsedMillis.get()).isGreaterThanOrEqualTo(20L);
    }

    @Test
    @DisplayName("FoliaRegionExecutionBridge scheduleGlobalTask runs on global pool")
    void testFoliaScheduleGlobalTask() throws InterruptedException {
        String mockPlugin = "TestFoliaPlugin";
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean ran = new AtomicBoolean(false);

        Object taskId = foliaBridge.scheduleGlobalTask(mockPlugin, () -> {
            ran.set(true);
            latch.countDown();
        });

        assertThat(taskId).isNotNull();
        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(ran.get()).isTrue();
    }

    @Test
    @DisplayName("FoliaRegionExecutionBridge executeForEntity runs retired callback if entity is retired")
    void testFoliaExecuteForEntityRetired() throws InterruptedException {
        String mockPlugin = "TestFoliaPlugin";
        CountDownLatch retiredLatch = new CountDownLatch(1);
        AtomicBoolean retiredRan = new AtomicBoolean(false);

        Object taskId = foliaBridge.executeForEntity(
                null, // null / retired entity
                mockPlugin,
                () -> {},
                () -> {
                    retiredRan.set(true);
                    retiredLatch.countDown();
                },
                0L
        );

        assertThat(taskId).isNotNull();
        assertThat(retiredLatch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(retiredRan.get()).isTrue();
    }

    @Test
    @DisplayName("FoliaRegionExecutionBridge cancelTask cancels active scheduled tasks")
    void testFoliaCancelTask() throws InterruptedException {
        String mockPlugin = "TestFoliaPlugin";
        AtomicBoolean ran = new AtomicBoolean(false);

        Object taskId = foliaBridge.executeInRegionDelayed(mockPlugin, "world", 0, 0, () -> {
            ran.set(true);
        }, 20L); // 1000ms delay

        boolean cancelled = foliaBridge.cancelTask(taskId);
        assertThat(cancelled).isTrue();

        Thread.sleep(100L);
        assertThat(ran.get()).isFalse();
    }

    // =========================================================================
    // 3. BUKKIT SCHEDULER SYMMETRY BRIDGE
    // =========================================================================

    @Test
    @DisplayName("BukkitSchedulerSymmetryBridge runTask synchronously executes workload")
    void testBukkitRunTask() throws InterruptedException {
        String mockPlugin = "LegacyBukkitPlugin";
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean executed = new AtomicBoolean(false);

        int taskId = bukkitBridge.runTask(mockPlugin, () -> {
            executed.set(true);
            latch.countDown();
        });

        assertThat(taskId).isPositive();
        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(executed.get()).isTrue();
    }

    @Test
    @DisplayName("BukkitSchedulerSymmetryBridge runTaskLater respects tick delay")
    void testBukkitRunTaskLater() throws InterruptedException {
        String mockPlugin = "LegacyBukkitPlugin";
        CountDownLatch latch = new CountDownLatch(1);
        long startNanos = System.nanoTime();
        AtomicLong elapsedMillis = new AtomicLong(0L);

        int taskId = bukkitBridge.runTaskLater(mockPlugin, () -> {
            elapsedMillis.set(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos));
            latch.countDown();
        }, 1L); // 1 tick = 50ms

        assertThat(taskId).isPositive();
        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(elapsedMillis.get()).isGreaterThanOrEqualTo(20L);
    }

    @Test
    @DisplayName("BukkitSchedulerSymmetryBridge runTaskAsynchronously executes on async worker pool")
    void testBukkitRunTaskAsynchronously() throws InterruptedException {
        String mockPlugin = "LegacyBukkitPlugin";
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean isAsync = new AtomicBoolean(false);

        int taskId = bukkitBridge.runTaskAsynchronously(mockPlugin, () -> {
            isAsync.set(Thread.currentThread().getName().contains("async"));
            latch.countDown();
        });

        assertThat(taskId).isPositive();
        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(isAsync.get()).isTrue();
    }

    @Test
    @DisplayName("BukkitSchedulerSymmetryBridge cancelTask and cancelTasks by plugin handle")
    void testBukkitCancelTasks() throws InterruptedException {
        String mockPlugin1 = "PluginAlpha";
        String mockPlugin2 = "PluginBeta";

        AtomicInteger alphaRuns = new AtomicInteger(0);
        AtomicInteger betaRuns = new AtomicInteger(0);

        int taskAlpha1 = bukkitBridge.runTaskLater(mockPlugin1, alphaRuns::incrementAndGet, 20L);
        int taskAlpha2 = bukkitBridge.runTaskLater(mockPlugin1, alphaRuns::incrementAndGet, 20L);
        int taskBeta = bukkitBridge.runTaskLater(mockPlugin2, betaRuns::incrementAndGet, 20L);

        assertThat(bukkitBridge.isQueued(taskAlpha1)).isTrue();
        assertThat(bukkitBridge.isQueued(taskAlpha2)).isTrue();
        assertThat(bukkitBridge.isQueued(taskBeta)).isTrue();

        // Cancel specific task
        bukkitBridge.cancelTask(taskBeta);
        assertThat(bukkitBridge.isQueued(taskBeta)).isFalse();

        // Cancel all tasks for PluginAlpha
        bukkitBridge.cancelTasks(mockPlugin1);
        assertThat(bukkitBridge.isQueued(taskAlpha1)).isFalse();
        assertThat(bukkitBridge.isQueued(taskAlpha2)).isFalse();

        Thread.sleep(100L);
        assertThat(alphaRuns.get()).isEqualTo(0);
        assertThat(betaRuns.get()).isEqualTo(0);
    }
}
