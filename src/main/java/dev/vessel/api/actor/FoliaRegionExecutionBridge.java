package dev.vessel.api.actor;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Direct opcode receptor and execution bridge for Folia's regional concurrency contracts,
 * including {@code RegionScheduler}, {@code GlobalRegionScheduler}, {@code EntityScheduler},
 * and {@code AsyncScheduler} under VUSA Chapter 3.3 and Method Symmetry Dictionary Domain 3.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
public final class FoliaRegionExecutionBridge implements AutoCloseable {

    /**
     * Standard Minecraft game tick duration in milliseconds (50ms = 20 TPS).
     */
    public static final long DEFAULT_TICK_MILLIS = 50L;

    /**
     * Default thread pool size for asynchronous scheduling.
     */
    public static final int DEFAULT_THREAD_POOL_SIZE = 8;

    /**
     * Default termination await timeout in seconds.
     */
    public static final long DEFAULT_TERMINATION_TIMEOUT_SECONDS = 2L;

    private static final FoliaRegionExecutionBridge DEFAULT_INSTANCE = new FoliaRegionExecutionBridge();

    private final ScheduledExecutorService scheduler;
    private final Map<Long, RegionalActorStrand> regionalStrands;
    private final Map<Long, ScheduledFuture<?>> activeTasks;
    private final AtomicLong taskIdGenerator;

    /**
     * Returns the global default execution bridge instance for opcode receptor dispatch.
     *
     * @return singleton bridge instance
     */
    public static FoliaRegionExecutionBridge getInstance() {
        return DEFAULT_INSTANCE;
    }

    /**
     * Constructs a regional execution bridge with default settings.
     */
    public FoliaRegionExecutionBridge() {
        this(DEFAULT_THREAD_POOL_SIZE);
    }

    /**
     * Constructs a regional execution bridge with configurable pool size.
     *
     * @param poolSize core worker thread pool size
     */
    public FoliaRegionExecutionBridge(int poolSize) {
        if (poolSize <= 0) {
            throw new IllegalArgumentException("poolSize must be strictly positive");
        }
        this.scheduler = Executors.newScheduledThreadPool(poolSize, runnable -> {
            Thread thread = new Thread(runnable, "vessel-folia-bridge-worker");
            thread.setDaemon(true);
            return thread;
        });
        this.regionalStrands = new ConcurrentHashMap<>();
        this.activeTasks = new ConcurrentHashMap<>();
        this.taskIdGenerator = new AtomicLong(1L);
    }

    /**
     * Resolves or initializes the regional actor strand governing the 16x16 chunk region.
     *
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @return dedicated regional strand
     */
    public RegionalActorStrand getOrCreateRegionStrand(int chunkX, int chunkZ) {
        int regX = chunkX >> 4;
        int regZ = chunkZ >> 4;
        long key = RegionalActorStrand.packRegionKey(regX, regZ);
        return regionalStrands.computeIfAbsent(key, k -> new RegionalActorStrand(regX, regZ));
    }

    /**
     * Executes a task on the strand governing the given chunk coordinates (Folia RegionScheduler.execute).
     *
     * @param plugin plugin handle
     * @param world  world handle
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @param task   workload to execute
     * @return task identifier
     */
    public Object executeInRegion(Object plugin, Object world, int chunkX, int chunkZ, Runnable task) {
        Objects.requireNonNull(task, "task cannot be null");
        long taskId = taskIdGenerator.getAndIncrement();
        RegionalActorStrand strand = getOrCreateRegionStrand(chunkX, chunkZ);
        strand.post(task);
        return taskId;
    }

    /**
     * Schedules a delayed task on the strand governing the given chunk coordinates (Folia RegionScheduler.runDelayed).
     *
     * @param plugin     plugin handle
     * @param world      world handle
     * @param chunkX     chunk X coordinate
     * @param chunkZ     chunk Z coordinate
     * @param task       workload to execute
     * @param delayTicks delay measured in game ticks (50ms per tick)
     * @return task identifier
     */
    public Object executeInRegionDelayed(
            Object plugin,
            Object world,
            int chunkX,
            int chunkZ,
            Runnable task,
            long delayTicks
    ) {
        Objects.requireNonNull(task, "task cannot be null");
        long taskId = taskIdGenerator.getAndIncrement();
        RegionalActorStrand strand = getOrCreateRegionStrand(chunkX, chunkZ);
        long delayMillis = Math.max(0L, delayTicks * DEFAULT_TICK_MILLIS);

        ScheduledFuture<?> future = scheduler.schedule(() -> strand.post(task), delayMillis, TimeUnit.MILLISECONDS);
        activeTasks.put(taskId, future);
        return taskId;
    }

    /**
     * Schedules a repeating task on the strand governing the given chunk coordinates (Folia RegionScheduler.runAtFixedRate).
     *
     * @param plugin      plugin handle
     * @param world       world handle
     * @param chunkX      chunk X coordinate
     * @param chunkZ      chunk Z coordinate
     * @param task        workload to execute
     * @param delayTicks  initial delay in game ticks
     * @param periodTicks period between ticks
     * @return task identifier
     */
    public Object executeInRegionRepeating(
            Object plugin,
            Object world,
            int chunkX,
            int chunkZ,
            Runnable task,
            long delayTicks,
            long periodTicks
    ) {
        Objects.requireNonNull(task, "task cannot be null");
        long taskId = taskIdGenerator.getAndIncrement();
        RegionalActorStrand strand = getOrCreateRegionStrand(chunkX, chunkZ);
        long initialDelayMillis = Math.max(0L, delayTicks * DEFAULT_TICK_MILLIS);
        long periodMillis = Math.max(DEFAULT_TICK_MILLIS, periodTicks * DEFAULT_TICK_MILLIS);

        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                () -> strand.post(task),
                initialDelayMillis,
                periodMillis,
                TimeUnit.MILLISECONDS
        );
        activeTasks.put(taskId, future);
        return taskId;
    }

    /**
     * Schedules a task on the global region strand (Folia GlobalRegionScheduler.execute).
     *
     * @param plugin plugin handle
     * @param task   workload to execute
     * @return task identifier
     */
    public Object scheduleGlobalTask(Object plugin, Runnable task) {
        Objects.requireNonNull(task, "task cannot be null");
        long taskId = taskIdGenerator.getAndIncrement();
        scheduler.execute(task);
        return taskId;
    }

    /**
     * Schedules a delayed task on the global region strand (Folia GlobalRegionScheduler.runDelayed).
     *
     * @param plugin     plugin handle
     * @param task       workload to execute
     * @param delayTicks delay in game ticks
     * @return task identifier
     */
    public Object scheduleGlobalDelayed(Object plugin, Runnable task, long delayTicks) {
        Objects.requireNonNull(task, "task cannot be null");
        long taskId = taskIdGenerator.getAndIncrement();
        long delayMillis = Math.max(0L, delayTicks * DEFAULT_TICK_MILLIS);

        ScheduledFuture<?> future = scheduler.schedule(task, delayMillis, TimeUnit.MILLISECONDS);
        activeTasks.put(taskId, future);
        return taskId;
    }

    /**
     * Schedules a repeating task on the global region strand (Folia GlobalRegionScheduler.runAtFixedRate).
     *
     * @param plugin      plugin handle
     * @param task        workload to execute
     * @param delayTicks  initial delay in ticks
     * @param periodTicks period in ticks
     * @return task identifier
     */
    public Object scheduleGlobalRepeating(Object plugin, Runnable task, long delayTicks, long periodTicks) {
        Objects.requireNonNull(task, "task cannot be null");
        long taskId = taskIdGenerator.getAndIncrement();
        long initialDelayMillis = Math.max(0L, delayTicks * DEFAULT_TICK_MILLIS);
        long periodMillis = Math.max(DEFAULT_TICK_MILLIS, periodTicks * DEFAULT_TICK_MILLIS);

        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                task,
                initialDelayMillis,
                periodMillis,
                TimeUnit.MILLISECONDS
        );
        activeTasks.put(taskId, future);
        return taskId;
    }

    /**
     * Executes a task attached to an entity's lifecycle (Folia EntityScheduler.execute).
     *
     * @param entity     entity instance
     * @param plugin     plugin handle
     * @param task       workload to run
     * @param retired    fallback runnable if entity is despawned/retired
     * @param delayTicks delay in ticks
     * @return task identifier
     */
    public Object executeForEntity(Object entity, Object plugin, Runnable task, Runnable retired, long delayTicks) {
        Objects.requireNonNull(task, "task cannot be null");
        if (entity == null && retired != null) {
            retired.run();
            return 0L;
        }
        if (delayTicks <= 0) {
            return scheduleGlobalTask(plugin, task);
        }
        return scheduleGlobalDelayed(plugin, task, delayTicks);
    }

    /**
     * Cancels an active scheduled task by its numeric ID.
     *
     * @param taskId task identifier
     * @return true if successfully cancelled
     */
    public boolean cancelTask(long taskId) {
        ScheduledFuture<?> future = activeTasks.remove(taskId);
        if (future != null) {
            return future.cancel(false);
        }
        return false;
    }

    /**
     * Cancels a scheduled task by arbitrary task token object.
     *
     * @param taskToken token or Long ID
     * @return true if cancelled
     */
    public boolean cancelTask(Object taskToken) {
        if (taskToken instanceof Long id) {
            return cancelTask(id.longValue());
        }
        if (taskToken instanceof Number num) {
            return cancelTask(num.longValue());
        }
        return false;
    }

    // =========================================================================
    // STATIC ASM OP-CODE RECEPTORS (INVOKEINTERFACE -> INVOKESTATIC)
    // =========================================================================

    /**
     * Opcode receptor for Folia {@code RegionScheduler.execute}.
     */
    public static Object execute(Object plugin, Object world, int chunkX, int chunkZ, Runnable task) {
        return getInstance().executeInRegion(plugin, world, chunkX, chunkZ, task);
    }

    /**
     * Opcode receptor for Folia {@code RegionScheduler.runDelayed}.
     */
    public static Object runDelayed(Object plugin, Object world, int chunkX, int chunkZ, Runnable task, long delayTicks) {
        return getInstance().executeInRegionDelayed(plugin, world, chunkX, chunkZ, task, delayTicks);
    }

    /**
     * Opcode receptor for Folia {@code RegionScheduler.runAtFixedRate}.
     */
    public static Object runAtFixedRate(
            Object plugin,
            Object world,
            int chunkX,
            int chunkZ,
            Runnable task,
            long delayTicks,
            long periodTicks
    ) {
        return getInstance().executeInRegionRepeating(plugin, world, chunkX, chunkZ, task, delayTicks, periodTicks);
    }

    /**
     * Opcode receptor for Folia {@code GlobalRegionScheduler.execute}.
     */
    public static Object globalExecute(Object plugin, Runnable task) {
        return getInstance().scheduleGlobalTask(plugin, task);
    }

    /**
     * Opcode receptor for Folia {@code GlobalRegionScheduler.runDelayed}.
     */
    public static Object globalRunDelayed(Object plugin, Runnable task, long delayTicks) {
        return getInstance().scheduleGlobalDelayed(plugin, task, delayTicks);
    }

    /**
     * Opcode receptor for Folia {@code GlobalRegionScheduler.runAtFixedRate}.
     */
    public static Object globalRunAtFixedRate(Object plugin, Runnable task, long delayTicks, long periodTicks) {
        return getInstance().scheduleGlobalRepeating(plugin, task, delayTicks, periodTicks);
    }

    /**
     * Opcode receptor for Folia {@code EntityScheduler.execute}.
     */
    public static Object entityExecute(Object entity, Object plugin, Runnable task, Runnable retired, long delayTicks) {
        return getInstance().executeForEntity(entity, plugin, task, retired, delayTicks);
    }

    /**
     * Opcode receptor for task cancellation.
     */
    public static boolean cancel(long taskId) {
        return getInstance().cancelTask(taskId);
    }

    /**
     * Returns the count of actively tracked regional strands.
     *
     * @return strand count
     */
    public int getActiveStrandCount() {
        return regionalStrands.size();
    }

    /**
     * Returns the count of actively scheduled tasks.
     *
     * @return active task count
     */
    public int getActiveTaskCount() {
        return activeTasks.size();
    }

    /**
     * Checks if the bridge scheduler is currently shut down.
     *
     * @return true if shut down
     */
    public boolean isShutdown() {
        return scheduler.isShutdown();
    }

    @Override
    public void close() {
        activeTasks.values().forEach(future -> future.cancel(false));
        activeTasks.clear();
        regionalStrands.values().forEach(RegionalActorStrand::close);
        regionalStrands.clear();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(DEFAULT_TERMINATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
