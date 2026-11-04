package dev.vessel.api.actor;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Dedicated execution strand governing an independent 16x16 chunk world region.
 * <p>
 * Implements the lock-free Actor Model concurrency standard defined in Chapter 3.3 and 6
 * of the VESSEL Universal Super-API specification. All block modifications, entity state ticks,
 * and tile entity operations within this region coordinate are strictly serialized on this strand
 * without locking or lock contention.
 * </p>
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public final class RegionalActorStrand implements AutoCloseable {

    /**
     * Default termination await timeout in seconds.
     */
    public static final long DEFAULT_SHUTDOWN_TIMEOUT_SECONDS = 3L;

    /**
     * Maximum tasks drained in a single execution dispatch.
     */
    public static final int DEFAULT_DRAIN_BATCH_SIZE = 128;

    private static final AtomicLong REQUEST_COUNTER = new AtomicLong(1L);

    private final int regionX;
    private final int regionZ;
    private final long regionKey;
    private final LockFreeMailboxRingBuffer<Runnable> mailbox;
    private final CrossRegionAskTimeoutGuard timeoutGuard;
    private final ExecutorService executor;
    private final AtomicBoolean running;
    private final AtomicLong processedTaskCount;
    private volatile Thread executionThread;

    /**
     * Constructs a strand for the region at the given chunk region coordinates (16x16 chunk grid).
     *
     * @param regionX chunk region X coordinate (chunkX >> 4)
     * @param regionZ chunk region Z coordinate (chunkZ >> 4)
     */
    public RegionalActorStrand(int regionX, int regionZ) {
        this(regionX, regionZ, new CrossRegionAskTimeoutGuard());
    }

    /**
     * Constructs a strand for the given coordinates and custom timeout guard.
     *
     * @param regionX chunk region X coordinate
     * @param regionZ chunk region Z coordinate
     * @param timeoutGuard timeout and circuit breaker guard
     */
    public RegionalActorStrand(int regionX, int regionZ, CrossRegionAskTimeoutGuard timeoutGuard) {
        this.regionX = regionX;
        this.regionZ = regionZ;
        this.regionKey = packRegionKey(regionX, regionZ);
        this.timeoutGuard = Objects.requireNonNull(timeoutGuard, "timeoutGuard cannot be null");
        this.mailbox = new LockFreeMailboxRingBuffer<>();
        this.running = new AtomicBoolean(true);
        this.processedTaskCount = new AtomicLong(0L);

        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "vessel-actor-strand-" + regionX + "-" + regionZ);
            thread.setDaemon(true);
            this.executionThread = thread;
            return thread;
        });
    }

    /**
     * Packs two 32-bit region coordinates into a single 64-bit coordinate key.
     *
     * @param regX region X
     * @param regZ region Z
     * @return 64-bit packed key
     */
    public static long packRegionKey(int regX, int regZ) {
        return (((long) regX) << 32) | (regZ & 0xFFFFFFFFL);
    }

    /**
     * Extracts the 32-bit region X from a packed 64-bit key.
     *
     * @param key 64-bit packed key
     * @return region X
     */
    public static int unpackRegionX(long key) {
        return (int) (key >> 32);
    }

    /**
     * Extracts the 32-bit region Z from a packed 64-bit key.
     *
     * @param key 64-bit packed key
     * @return region Z
     */
    public static int unpackRegionZ(long key) {
        return (int) key;
    }

    /**
     * Posts a fire-and-forget task (Actor Tell pattern) to this region's strand.
     *
     * @param task executable workload to run on this region strand
     * @return {@code true} if accepted into mailbox; {@code false} if rejected due to saturation or shutdown
     */
    public boolean post(Runnable task) {
        Objects.requireNonNull(task, "task cannot be null");
        if (!running.get()) {
            return false;
        }

        boolean enqueued = mailbox.offer(task);
        if (enqueued && running.get()) {
            executor.execute(this::drainMailbox);
        }
        return enqueued;
    }

    /**
     * Submits an asynchronous Request-Reply task (Actor Ask pattern) with default 25ms timeout guard.
     *
     * @param <T> result type
     * @param taskSupplier workload producing result on this region strand
     * @return guarded CompletableFuture
     */
    public <T> CompletableFuture<T> ask(Supplier<T> taskSupplier) {
        return ask(taskSupplier, timeoutGuard.getTimeoutMillis());
    }

    /**
     * Submits an asynchronous Request-Reply task (Actor Ask pattern) with custom deadline.
     *
     * @param <T> result type
     * @param taskSupplier workload producing result on this region strand
     * @param customTimeoutMillis maximum execution latency in milliseconds
     * @return guarded CompletableFuture
     */
    public <T> CompletableFuture<T> ask(Supplier<T> taskSupplier, long customTimeoutMillis) {
        Objects.requireNonNull(taskSupplier, "taskSupplier cannot be null");
        CompletableFuture<T> future = new CompletableFuture<>();

        CrossRegionAskRequest<T> request = new CrossRegionAskRequest<>(
                REQUEST_COUNTER.getAndIncrement(),
                0L,
                regionKey,
                taskSupplier,
                future,
                customTimeoutMillis
        );

        return ask(request);
    }

    /**
     * Submits a formal {@link CrossRegionAskRequest} to this region's strand.
     *
     * @param <T> result type
     * @param request the ask request contract
     * @return guarded CompletableFuture
     */
    public <T> CompletableFuture<T> ask(CrossRegionAskRequest<T> request) {
        Objects.requireNonNull(request, "request cannot be null");
        CompletableFuture<T> future = request.future();

        if (!running.get()) {
            future.completeExceptionally(
                    new IllegalStateException("RegionalActorStrand is stopped for region: " + regionX + ", " + regionZ)
            );
            return future;
        }

        timeoutGuard.guard(future, request.timeoutMillis());

        boolean enqueued = post(request::execute);
        if (!enqueued) {
            future.completeExceptionally(
                    new IllegalStateException("Mailbox saturated for region: " + regionX + ", " + regionZ)
            );
        }

        return future;
    }

    private void drainMailbox() {
        int drained = mailbox.drain(task -> {
            try {
                processedTaskCount.incrementAndGet();
                task.run();
            } catch (Throwable throwable) {
                // Keep strand alive on task failures
            }
        }, DEFAULT_DRAIN_BATCH_SIZE);

        if (drained > 0 && !mailbox.isEmpty() && running.get()) {
            executor.execute(this::drainMailbox);
        }
    }

    /**
     * Checks if the currently calling thread is this region's pinned execution thread.
     *
     * @return {@code true} if invoked from within this strand; {@code false} otherwise
     */
    public boolean isCurrentStrand() {
        return Thread.currentThread() == executionThread;
    }

    /**
     * Returns the 32-bit region X coordinate.
     *
     * @return region X
     */
    public int getRegionX() {
        return regionX;
    }

    /**
     * Returns the 32-bit region Z coordinate.
     *
     * @return region Z
     */
    public int getRegionZ() {
        return regionZ;
    }

    /**
     * Returns the packed 64-bit region coordinate key.
     *
     * @return 64-bit packed key
     */
    public long getRegionKey() {
        return regionKey;
    }

    /**
     * Returns the underlying mailbox ring buffer.
     *
     * @return mailbox ring buffer
     */
    public LockFreeMailboxRingBuffer<Runnable> getMailbox() {
        return mailbox;
    }

    /**
     * Returns the timeout and circuit breaker guard attached to this strand.
     *
     * @return timeout guard
     */
    public CrossRegionAskTimeoutGuard getTimeoutGuard() {
        return timeoutGuard;
    }

    /**
     * Returns the cumulative number of successfully processed tasks on this strand.
     *
     * @return processed task count
     */
    public long getProcessedTaskCount() {
        return processedTaskCount.get();
    }

    /**
     * Initiates orderly shutdown of this region strand.
     */
    public void shutdown() {
        if (running.compareAndSet(true, false)) {
            executor.shutdown();
        }
    }

    /**
     * Checks if this strand has been shut down.
     *
     * @return {@code true} if shut down
     */
    public boolean isShutdown() {
        return !running.get() || executor.isShutdown();
    }

    @Override
    public void close() {
        shutdown();
        try {
            if (!executor.awaitTermination(DEFAULT_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException interruptedException) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
