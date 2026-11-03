package dev.vessel.api.actor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verification test suite for Folia and Bukkit regional scheduler bridges under VUSA Chapter 3.3
 * and Method Symmetry Dictionary Domain 3.
 *
 * <p>Strictly adheres to Rule 17: Zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
public final class ActorSchedulerSymmetryBridgeTest {

    private FoliaRegionExecutionBridge foliaBridge;
    private BukkitSchedulerSymmetryBridge bukkitBridge;

    @BeforeEach
    public void setUp() {
        this.foliaBridge = new FoliaRegionExecutionBridge(4);
        this.bukkitBridge = new BukkitSchedulerSymmetryBridge(2, 4);
    }

    @AfterEach
    public void tearDown() {
        if (foliaBridge != null) {
            foliaBridge.close();
        }
        if (bukkitBridge != null) {
            bukkitBridge.close();
        }
    }

    @Test
    @DisplayName("FoliaRegionExecutionBridge executes in regional strand correctly")
    public void testFoliaRegionalExecution() throws InterruptedException {
        int chunkX = 32;
        int chunkZ = 48;
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean executed = new AtomicBoolean(false);

        Object taskId = foliaBridge.executeInRegion("mock_plugin", "world", chunkX, chunkZ, () -> {
            executed.set(true);
            latch.countDown();
        });

        assertThat(taskId).isNotNull();
        boolean completed = latch.await(2, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        assertThat(executed.get()).isTrue();
        assertThat(foliaBridge.getActiveStrandCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("FoliaRegionExecutionBridge schedules and executes global tasks")
    public void testFoliaGlobalExecution() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger counter = new AtomicInteger(0);

        Object taskId = foliaBridge.scheduleGlobalTask("mock_plugin", () -> {
            counter.incrementAndGet();
            latch.countDown();
        });

        assertThat(taskId).isNotNull();
        boolean completed = latch.await(2, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("FoliaRegionExecutionBridge schedules delayed tasks and supports cancellation")
    public void testFoliaDelayedAndCancel() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean executed = new AtomicBoolean(false);

        // Schedule delayed task (10 ticks = 500ms)
        Object taskId = foliaBridge.scheduleGlobalDelayed("mock_plugin", () -> {
            executed.set(true);
            latch.countDown();
        }, 10L);

        assertThat(taskId).isNotNull();
        assertThat(foliaBridge.getActiveTaskCount()).isGreaterThanOrEqualTo(1);

        // Cancel task before execution
        boolean cancelled = foliaBridge.cancelTask(taskId);
        assertThat(cancelled).isTrue();

        boolean ran = latch.await(600, TimeUnit.MILLISECONDS);
        assertThat(ran).isFalse();
        assertThat(executed.get()).isFalse();
    }

    @Test
    @DisplayName("FoliaRegionExecutionBridge handles EntityScheduler lifecycle execution")
    public void testFoliaEntityExecution() throws InterruptedException {
        CountDownLatch taskLatch = new CountDownLatch(1);
        CountDownLatch retiredLatch = new CountDownLatch(1);

        // Non-null entity executes task
        Object entity = new Object();
        foliaBridge.executeForEntity(entity, "mock_plugin", taskLatch::countDown, retiredLatch::countDown, 0L);
        assertThat(taskLatch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(retiredLatch.getCount()).isEqualTo(1L);

        // Null entity invokes retired callback
        CountDownLatch retiredOnlyLatch = new CountDownLatch(1);
        foliaBridge.executeForEntity(null, "mock_plugin", () -> {}, retiredOnlyLatch::countDown, 0L);
        assertThat(retiredOnlyLatch.await(2, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @DisplayName("BukkitSchedulerSymmetryBridge executes sync and async tasks")
    public void testBukkitSyncAndAsyncExecution() throws InterruptedException {
        CountDownLatch syncLatch = new CountDownLatch(1);
        CountDownLatch asyncLatch = new CountDownLatch(1);

        int syncTaskId = bukkitBridge.runTask("mock_plugin", syncLatch::countDown);
        int asyncTaskId = bukkitBridge.runTaskAsynchronously("mock_plugin", asyncLatch::countDown);

        assertThat(syncTaskId).isPositive();
        assertThat(asyncTaskId).isPositive();
        assertThat(syncTaskId).isNotEqualTo(asyncTaskId);

        assertThat(syncLatch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(asyncLatch.await(2, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @DisplayName("BukkitSchedulerSymmetryBridge cancels individual tasks and plugin tasks")
    public void testBukkitTaskCancellation() throws InterruptedException {
        String pluginA = "plugin_A";
        String pluginB = "plugin_B";

        AtomicBoolean taskA1Ran = new AtomicBoolean(false);
        AtomicBoolean taskA2Ran = new AtomicBoolean(false);
        AtomicBoolean taskB1Ran = new AtomicBoolean(false);

        // Schedule delayed tasks (20 ticks = 1000ms)
        int idA1 = bukkitBridge.runTaskLater(pluginA, () -> taskA1Ran.set(true), 20L);
        int idA2 = bukkitBridge.runTaskLater(pluginA, () -> taskA2Ran.set(true), 20L);
        int idB1 = bukkitBridge.runTaskLater(pluginB, () -> taskB1Ran.set(true), 20L);

        assertThat(bukkitBridge.getActiveTaskCount()).isEqualTo(3);

        // Cancel single task idA1
        bukkitBridge.cancelTask(idA1);
        assertThat(bukkitBridge.getActiveTaskCount()).isEqualTo(2);

        // Cancel all tasks for pluginA (should cancel idA2)
        bukkitBridge.cancelTasks(pluginA);
        assertThat(bukkitBridge.getActiveTaskCount()).isEqualTo(1);

        // idB1 should still be queued
        assertThat(bukkitBridge.isQueued(idB1)).isTrue();

        // Cancel idB1
        bukkitBridge.cancelTask(idB1);
        assertThat(bukkitBridge.getActiveTaskCount()).isEqualTo(0);

        Thread.sleep(100);
        assertThat(taskA1Ran.get()).isFalse();
        assertThat(taskA2Ran.get()).isFalse();
        assertThat(taskB1Ran.get()).isFalse();
    }

    @Test
    @DisplayName("Static opcode receptors forward to singleton bridge without error")
    public void testStaticOpcodeReceptors() throws InterruptedException {
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);

        Object foliaTask = FoliaRegionExecutionBridge.globalExecute("static_plugin", latch1::countDown);
        int bukkitTask = BukkitSchedulerSymmetryBridge.schedule("static_plugin", latch2::countDown);

        assertThat(foliaTask).isNotNull();
        assertThat(bukkitTask).isPositive();

        assertThat(latch1.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(latch2.await(2, TimeUnit.SECONDS)).isTrue();
    }
}
