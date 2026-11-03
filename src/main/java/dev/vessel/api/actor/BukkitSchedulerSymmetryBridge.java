package dev.vessel.api.actor;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Direct opcode receptor and execution bridge for Bukkit's {@code BukkitScheduler} contracts,
 * translating single-threaded synchronous and asynchronous scheduling calls into VESSEL's
 * sovereign actor scheduler under VUSA Chapter 3.3 and Method Symmetry Dictionary Domain 3.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
public final class BukkitSchedulerSymmetryBridge implements AutoCloseable {

    /**
     * Standard Minecraft game tick duration in milliseconds (50ms = 20 TPS).
     */
    public static final long MILLIS_PER_TICK = 50L;

    /**
     * Default thread pool size for synchronous server tasks.
     */
    public static final int DEFAULT_SYNC_POOL_SIZE = 4;

    /**
     * Default thread pool size for asynchronous background tasks.
     */
    public static final int DEFAULT_ASYNC_POOL_SIZE = 8;

    /**
     * Default termination await timeout in seconds.
     */
    public static final long DEFAULT_TERMINATION_TIMEOUT_SECONDS = 2L;

    private static final BukkitSchedulerSymmetryBridge DEFAULT_INSTANCE = new BukkitSchedulerSymmetryBridge();

    private final ScheduledExecutorService syncScheduler;
    private final ScheduledExecutorService asyncScheduler;
    private final AtomicInteger taskIdGenerator;
    private final Map<Integer, ScheduledFuture<?>> activeTasks;
    private final Map<Integer, Object> taskPluginMap;
    private final Map<Object, Set<Integer>> pluginTaskMap;

    /**
     * Returns the global default symmetry bridge instance.
     *
     * @return singleton bridge instance
     */
    public static BukkitSchedulerSymmetryBridge getInstance() {
        return DEFAULT_INSTANCE;
    }

    /**
     * Constructs a symmetry bridge with default thread pool settings.
     */
    public BukkitSchedulerSymmetryBridge() {
        this(DEFAULT_SYNC_POOL_SIZE, DEFAULT_ASYNC_POOL_SIZE);
    }

    /**
     * Constructs a symmetry bridge with custom thread pool allocations.
     *
     * @param syncPoolSize  core thread pool size for synchronous server work
     * @param asyncPoolSize core thread pool size for asynchronous background work
     */
    public BukkitSchedulerSymmetryBridge(int syncPoolSize, int asyncPoolSize) {
        if (syncPoolSize <= 0 || asyncPoolSize <= 0) {
            throw new IllegalArgumentException("Pool sizes must be strictly positive");
        }

        this.syncScheduler = Executors.newScheduledThreadPool(syncPoolSize, runnable -> {
            Thread thread = new Thread(runnable, "vessel-bukkit-sync-worker");
            thread.setDaemon(true);
            return thread;
        });

        this.asyncScheduler = Executors.newScheduledThreadPool(asyncPoolSize, runnable -> {
            Thread thread = new Thread(runnable, "vessel-bukkit-async-worker");
            thread.setDaemon(true);
            return thread;
        });

        this.taskIdGenerator = new AtomicInteger(1);
        this.activeTasks = new ConcurrentHashMap<>();
        this.taskPluginMap = new ConcurrentHashMap<>();
        this.pluginTaskMap = new ConcurrentHashMap<>();
    }

    /**
     * Schedules a synchronous task to run on the next tick (BukkitScheduler.runTask).
     *
     * @param plugin plugin handle
     * @param task   workload to execute
     * @return positive task identifier
     */
    public int runTask(Object plugin, Runnable task) {
        Objects.requireNonNull(task, "task cannot be null");
        int taskId = nextTaskId();
        registerPluginAssociation(taskId, plugin);

        ScheduledFuture<?> future = syncScheduler.schedule(() -> executeAndCleanup(taskId, task), 0L, TimeUnit.MILLISECONDS);
        activeTasks.put(taskId, future);
        return taskId;
    }

    /**
     * Schedules a delayed synchronous task (BukkitScheduler.runTaskLater).
     *
     * @param plugin     plugin handle
     * @param task       workload to execute
     * @param delayTicks delay measured in game ticks
     * @return positive task identifier
     */
    public int runTaskLater(Object plugin, Runnable task, long delayTicks) {
        Objects.requireNonNull(task, "task cannot be null");
        int taskId = nextTaskId();
        registerPluginAssociation(taskId, plugin);

        long delayMillis = Math.max(0L, delayTicks * MILLIS_PER_TICK);
        ScheduledFuture<?> future = syncScheduler.schedule(() -> executeAndCleanup(taskId, task), delayMillis, TimeUnit.MILLISECONDS);
        activeTasks.put(taskId, future);
        return taskId;
    }

    /**
     * Schedules a repeating synchronous task (BukkitScheduler.runTaskTimer).
     *
     * @param plugin      plugin handle
     * @param task        workload to execute
     * @param delayTicks  initial delay in game ticks
     * @param periodTicks period between executions in game ticks
     * @return positive task identifier
     */
    public int runTaskTimer(Object plugin, Runnable task, long delayTicks, long periodTicks) {
        Objects.requireNonNull(task, "task cannot be null");
        int taskId = nextTaskId();
        registerPluginAssociation(taskId, plugin);

        long initialDelayMillis = Math.max(0L, delayTicks * MILLIS_PER_TICK);
        long periodMillis = Math.max(MILLIS_PER_TICK, periodTicks * MILLIS_PER_TICK);

        ScheduledFuture<?> future = syncScheduler.scheduleAtFixedRate(
                task,
                initialDelayMillis,
                periodMillis,
                TimeUnit.MILLISECONDS
        );
        activeTasks.put(taskId, future);
        return taskId;
    }

    /**
     * Schedules an asynchronous background task (BukkitScheduler.runTaskAsynchronously).
     *
     * @param plugin plugin handle
     * @param task   workload to execute
     * @return positive task identifier
     */
    public int runTaskAsynchronously(Object plugin, Runnable task) {
        Objects.requireNonNull(task, "task cannot be null");
        int taskId = nextTaskId();
        registerPluginAssociation(taskId, plugin);

        ScheduledFuture<?> future = asyncScheduler.schedule(() -> executeAndCleanup(taskId, task), 0L, TimeUnit.MILLISECONDS);
        activeTasks.put(taskId, future);
        return taskId;
    }

    /**
     * Schedules a delayed asynchronous task (BukkitScheduler.runTaskLaterAsynchronously).
     *
     * @param plugin     plugin handle
     * @param task       workload to execute
     * @param delayTicks delay measured in game ticks
     * @return positive task identifier
     */
    public int runTaskLaterAsynchronously(Object plugin, Runnable task, long delayTicks) {
        Objects.requireNonNull(task, "task cannot be null");
        int taskId = nextTaskId();
        registerPluginAssociation(taskId, plugin);

        long delayMillis = Math.max(0L, delayTicks * MILLIS_PER_TICK);
        ScheduledFuture<?> future = asyncScheduler.schedule(() -> executeAndCleanup(taskId, task), delayMillis, TimeUnit.MILLISECONDS);
        activeTasks.put(taskId, future);
        return taskId;
    }

    /**
     * Schedules a repeating asynchronous task (BukkitScheduler.runTaskTimerAsynchronously).
     *
     * @param plugin      plugin handle
     * @param task        workload to execute
     * @param delayTicks  initial delay in game ticks
     * @param periodTicks period between executions in game ticks
     * @return positive task identifier
     */
    public int runTaskTimerAsynchronously(Object plugin, Runnable task, long delayTicks, long periodTicks) {
        Objects.requireNonNull(task, "task cannot be null");
        int taskId = nextTaskId();
        registerPluginAssociation(taskId, plugin);

        long initialDelayMillis = Math.max(0L, delayTicks * MILLIS_PER_TICK);
        long periodMillis = Math.max(MILLIS_PER_TICK, periodTicks * MILLIS_PER_TICK);

        ScheduledFuture<?> future = asyncScheduler.scheduleAtFixedRate(
                task,
                initialDelayMillis,
                periodMillis,
                TimeUnit.MILLISECONDS
        );
        activeTasks.put(taskId, future);
        return taskId;
    }

    /**
     * Cancels a scheduled task by its numeric ID (BukkitScheduler.cancelTask).
     *
     * @param taskId task identifier
     */
    public void cancelTask(int taskId) {
        ScheduledFuture<?> future = activeTasks.remove(taskId);
        if (future != null) {
            future.cancel(false);
        }
        Object plugin = taskPluginMap.remove(taskId);
        if (plugin != null) {
            Set<Integer> tasks = pluginTaskMap.get(plugin);
            if (tasks != null) {
                tasks.remove(taskId);
            }
        }
    }

    /**
     * Cancels all active tasks registered by the specified plugin (BukkitScheduler.cancelTasks).
     *
     * @param plugin plugin handle
     */
    public void cancelTasks(Object plugin) {
        if (plugin == null) {
            return;
        }
        Set<Integer> tasks = pluginTaskMap.remove(plugin);
        if (tasks != null) {
            for (Integer taskId : tasks) {
                ScheduledFuture<?> future = activeTasks.remove(taskId);
                if (future != null) {
                    future.cancel(false);
                }
                taskPluginMap.remove(taskId);
            }
        }
    }

    /**
     * Checks if a task is currently executing.
     *
     * @param taskId task identifier
     * @return true if currently running
     */
    public boolean isCurrentlyRunning(int taskId) {
        ScheduledFuture<?> future = activeTasks.get(taskId);
        return future != null && !future.isDone() && !future.isCancelled();
    }

    /**
     * Checks if a task is queued for future execution.
     *
     * @param taskId task identifier
     * @return true if pending execution
     */
    public boolean isQueued(int taskId) {
        ScheduledFuture<?> future = activeTasks.get(taskId);
        return future != null && future.getDelay(TimeUnit.MILLISECONDS) > 0;
    }

    /**
     * Returns the total count of actively scheduled tasks.
     *
     * @return active task count
     */
    public int getActiveTaskCount() {
        return activeTasks.size();
    }

    // =========================================================================
    // STATIC ASM OP-CODE RECEPTORS (INVOKEINTERFACE -> INVOKESTATIC)
    // =========================================================================

    /**
     * Opcode receptor for {@code BukkitScheduler.runTask}.
     */
    public static int schedule(Object plugin, Runnable task) {
        return getInstance().runTask(plugin, task);
    }

    /**
     * Opcode receptor for {@code BukkitScheduler.runTaskLater}.
     */
    public static int scheduleLater(Object plugin, Runnable task, long delayTicks) {
        return getInstance().runTaskLater(plugin, task, delayTicks);
    }

    /**
     * Opcode receptor for {@code BukkitScheduler.runTaskTimer}.
     */
    public static int scheduleTimer(Object plugin, Runnable task, long delayTicks, long periodTicks) {
        return getInstance().runTaskTimer(plugin, task, delayTicks, periodTicks);
    }

    /**
     * Opcode receptor for {@code BukkitScheduler.runTaskAsynchronously}.
     */
    public static int scheduleAsync(Object plugin, Runnable task) {
        return getInstance().runTaskAsynchronously(plugin, task);
    }

    /**
     * Opcode receptor for {@code BukkitScheduler.runTaskLaterAsynchronously}.
     */
    public static int scheduleLaterAsync(Object plugin, Runnable task, long delayTicks) {
        return getInstance().runTaskLaterAsynchronously(plugin, task, delayTicks);
    }

    /**
     * Opcode receptor for {@code BukkitScheduler.runTaskTimerAsynchronously}.
     */
    public static int scheduleTimerAsync(Object plugin, Runnable task, long delayTicks, long periodTicks) {
        return getInstance().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
    }

    /**
     * Opcode receptor for {@code BukkitScheduler.cancelTask}.
     */
    public static void cancel(int taskId) {
        getInstance().cancelTask(taskId);
    }

    /**
     * Opcode receptor for {@code BukkitScheduler.cancelTasks}.
     */
    public static void cancelAll(Object plugin) {
        getInstance().cancelTasks(plugin);
    }

    private int nextTaskId() {
        return taskIdGenerator.getAndIncrement();
    }

    private void registerPluginAssociation(int taskId, Object plugin) {
        if (plugin != null) {
            taskPluginMap.put(taskId, plugin);
            pluginTaskMap.computeIfAbsent(plugin, k -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(taskId);
        }
    }

    private void executeAndCleanup(int taskId, Runnable task) {
        try {
            task.run();
        } finally {
            activeTasks.remove(taskId);
            Object plugin = taskPluginMap.remove(taskId);
            if (plugin != null) {
                Set<Integer> set = pluginTaskMap.get(plugin);
                if (set != null) {
                    set.remove(taskId);
                }
            }
        }
    }

    /**
     * Checks if the scheduler pools are shut down.
     *
     * @return true if shut down
     */
    public boolean isShutdown() {
        return syncScheduler.isShutdown() && asyncScheduler.isShutdown();
    }

    @Override
    public void close() {
        activeTasks.values().forEach(future -> future.cancel(false));
        activeTasks.clear();
        taskPluginMap.clear();
        pluginTaskMap.clear();

        syncScheduler.shutdown();
        asyncScheduler.shutdown();
        try {
            if (!syncScheduler.awaitTermination(DEFAULT_TERMINATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                syncScheduler.shutdownNow();
            }
            if (!asyncScheduler.awaitTermination(DEFAULT_TERMINATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                asyncScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            syncScheduler.shutdownNow();
            asyncScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
