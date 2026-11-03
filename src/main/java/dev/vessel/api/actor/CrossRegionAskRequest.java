package dev.vessel.api.actor;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Immutable request contract for asynchronous Request-Reply (Actor Ask pattern) cross-region operations.
 * <p>
 * Encapsulates the originating strand, target strand, executable workload supplier, completion future,
 * and execution deadline timestamp.
 * </p>
 *
 * @param <T> result payload type
 * @param requestId unique sequential identifier of this request
 * @param sourceRegionKey packed 64-bit coordinate key of the requesting strand
 * @param targetRegionKey packed 64-bit coordinate key of the destination strand
 * @param taskSupplier task logic to be executed on the target strand
 * @param future CompletableFuture receiving the result or exception
 * @param creationNanoTime monotonic start timestamp in nanoseconds
 * @param timeoutMillis execution deadline in milliseconds
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record CrossRegionAskRequest<T>(
        long requestId,
        long sourceRegionKey,
        long targetRegionKey,
        Supplier<T> taskSupplier,
        CompletableFuture<T> future,
        long creationNanoTime,
        long timeoutMillis
) {

    /**
     * Compact constructor validating non-null mandatory fields.
     */
    public CrossRegionAskRequest {
        Objects.requireNonNull(taskSupplier, "taskSupplier cannot be null");
        Objects.requireNonNull(future, "future cannot be null");
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("timeoutMillis must be positive: " + timeoutMillis);
        }
    }

    /**
     * Factory constructor initializing creation timestamp to the current monotonic time.
     *
     * @param requestId unique sequential identifier
     * @param sourceRegionKey originating strand key
     * @param targetRegionKey destination strand key
     * @param taskSupplier task logic supplier
     * @param future completion future
     * @param timeoutMillis execution deadline in milliseconds
     */
    public CrossRegionAskRequest(
            long requestId,
            long sourceRegionKey,
            long targetRegionKey,
            Supplier<T> taskSupplier,
            CompletableFuture<T> future,
            long timeoutMillis
    ) {
        this(requestId, sourceRegionKey, targetRegionKey, taskSupplier, future, System.nanoTime(), timeoutMillis);
    }

    /**
     * Determines whether this request has exceeded its execution deadline.
     *
     * @param currentNanoTime current monotonic timestamp in nanoseconds
     * @return {@code true} if deadline has passed; {@code false} otherwise
     */
    public boolean isExpired(long currentNanoTime) {
        long elapsedNanos = currentNanoTime - creationNanoTime;
        long deadlineNanos = TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        return elapsedNanos >= deadlineNanos;
    }

    /**
     * Executes the workload supplier on the current thread and delivers the result
     * to the completion future.
     */
    public void execute() {
        if (future.isDone()) {
            return;
        }
        try {
            T result = taskSupplier.get();
            future.complete(result);
        } catch (Throwable throwable) {
            future.completeExceptionally(throwable);
        }
    }

    /**
     * Completes the future exceptionally indicating a deadline expiration.
     *
     * @param timeoutMessage explanatory diagnosis of the timeout
     */
    public void failWithTimeout(String timeoutMessage) {
        future.completeExceptionally(new java.util.concurrent.TimeoutException(timeoutMessage));
    }
}
