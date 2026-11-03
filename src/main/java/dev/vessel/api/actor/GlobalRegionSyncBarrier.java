package dev.vessel.api.actor;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Safe synchronization barrier bridging single-threaded legacy plugins with Folia regional strands.
 * <p>
 * Directly resolves the canonical multi-threading incompatibility where legacy Bukkit, Spigot,
 * and Paper plugins assume a single global server thread and crash with {@link IllegalStateException}
 * or deadlock when accessing world entities across region boundaries.
 * </p>
 * <p>
 * If called from within the target region strand, execution proceeds directly without overhead.
 * If called from a foreign or global thread, the barrier routes a lock-free Request-Reply (Actor Ask)
 * packet to the destination strand with a strict 25ms timeout guard. If the deadline expires, the
 * barrier returns a safe fallback rather than crashing or freezing the server.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class GlobalRegionSyncBarrier {

    /**
     * Canonical execution deadline for synchronous barrier operations (25 milliseconds).
     */
    public static final long DEFAULT_BARRIER_TIMEOUT_MILLIS = 25L;

    private final CrossRegionAskTimeoutGuard timeoutGuard;
    private final AtomicLong totalBarrierInvocations;
    private final AtomicLong totalFallbackInvocations;
    private final AtomicLong totalDirectInvocations;

    /**
     * Constructs a synchronization barrier with default 25ms timeout guard.
     */
    public GlobalRegionSyncBarrier() {
        this(new CrossRegionAskTimeoutGuard());
    }

    /**
     * Constructs a synchronization barrier with a custom timeout guard.
     *
     * @param timeoutGuard the timeout guard to enforce
     */
    public GlobalRegionSyncBarrier(CrossRegionAskTimeoutGuard timeoutGuard) {
        this.timeoutGuard = Objects.requireNonNull(timeoutGuard, "timeoutGuard cannot be null");
        this.totalBarrierInvocations = new AtomicLong(0L);
        this.totalFallbackInvocations = new AtomicLong(0L);
        this.totalDirectInvocations = new AtomicLong(0L);
    }

    /**
     * Synchronously executes a task producing a value on the target strand with default 25ms deadline.
     *
     * @param <T> result type
     * @param targetStrand destination regional strand
     * @param taskSupplier task logic to execute
     * @param fallbackValue default value returned if deadline expires or error occurs
     * @return computed result, or fallbackValue if timed out or failed
     */
    public <T> T syncCall(RegionalActorStrand targetStrand, Supplier<T> taskSupplier, T fallbackValue) {
        return syncCall(targetStrand, taskSupplier, fallbackValue, DEFAULT_BARRIER_TIMEOUT_MILLIS);
    }

    /**
     * Synchronously executes a task producing a value on the target strand with custom deadline.
     *
     * @param <T> result type
     * @param targetStrand destination regional strand
     * @param taskSupplier task logic to execute
     * @param fallbackValue default value returned if deadline expires or error occurs
     * @param timeoutMillis maximum allowable latency in milliseconds
     * @return computed result, or fallbackValue if timed out or failed
     */
    public <T> T syncCall(
            RegionalActorStrand targetStrand,
            Supplier<T> taskSupplier,
            T fallbackValue,
            long timeoutMillis
    ) {
        Objects.requireNonNull(targetStrand, "targetStrand cannot be null");
        Objects.requireNonNull(taskSupplier, "taskSupplier cannot be null");
        totalBarrierInvocations.incrementAndGet();

        // Direct fast-path: already executing on target strand
        if (targetStrand.isCurrentStrand()) {
            totalDirectInvocations.incrementAndGet();
            try {
                return taskSupplier.get();
            } catch (Throwable throwable) {
                totalFallbackInvocations.incrementAndGet();
                return fallbackValue;
            }
        }

        // Cross-region slow-path: submit ask request with timeout guard
        CompletableFuture<T> future = targetStrand.ask(taskSupplier, timeoutMillis);
        try {
            return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (Throwable throwable) {
            totalFallbackInvocations.incrementAndGet();
            return fallbackValue;
        }
    }

    /**
     * Synchronously executes a runnable task on the target strand with default 25ms deadline.
     *
     * @param targetStrand destination regional strand
     * @param task runnable workload
     * @return {@code true} if completed within deadline; {@code false} if timed out or failed
     */
    public boolean syncRun(RegionalActorStrand targetStrand, Runnable task) {
        return syncRun(targetStrand, task, DEFAULT_BARRIER_TIMEOUT_MILLIS);
    }

    /**
     * Synchronously executes a runnable task on the target strand with custom deadline.
     *
     * @param targetStrand destination regional strand
     * @param task runnable workload
     * @param timeoutMillis maximum allowable latency in milliseconds
     * @return {@code true} if completed within deadline; {@code false} if timed out or failed
     */
    public boolean syncRun(RegionalActorStrand targetStrand, Runnable task, long timeoutMillis) {
        Objects.requireNonNull(targetStrand, "targetStrand cannot be null");
        Objects.requireNonNull(task, "task cannot be null");

        Supplier<Boolean> supplier = () -> {
            task.run();
            return Boolean.TRUE;
        };

        Boolean result = syncCall(targetStrand, supplier, Boolean.FALSE, timeoutMillis);
        return Boolean.TRUE.equals(result);
    }

    /**
     * Returns the underlying timeout guard.
     *
     * @return timeout guard
     */
    public CrossRegionAskTimeoutGuard getTimeoutGuard() {
        return timeoutGuard;
    }

    /**
     * Returns cumulative barrier invocations.
     *
     * @return total invocations
     */
    public long getTotalBarrierInvocations() {
        return totalBarrierInvocations.get();
    }

    /**
     * Returns cumulative fallback invocations due to timeout or error.
     *
     * @return total fallbacks
     */
    public long getTotalFallbackInvocations() {
        return totalFallbackInvocations.get();
    }

    /**
     * Returns cumulative direct path invocations when caller was already on strand.
     *
     * @return total direct invocations
     */
    public long getTotalDirectInvocations() {
        return totalDirectInvocations.get();
    }
}
