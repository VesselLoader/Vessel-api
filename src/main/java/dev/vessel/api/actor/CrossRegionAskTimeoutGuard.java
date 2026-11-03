package dev.vessel.api.actor;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Circuit-breaker and timeout protection mechanism enforcing strict execution deadlines.
 * <p>
 * Implements the canonical 25-millisecond deadline guard defined in Chapter 3.3 of the VESSEL
 * Universal Super-API specification. If cross-region actor operations stall or experience
 * cascading locks, the circuit breaker opens to shed load and protect the server tick loop
 * from freezing.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class CrossRegionAskTimeoutGuard {

    /**
     * Canonical default execution deadline for cross-region requests (25 milliseconds).
     */
    public static final long DEFAULT_TIMEOUT_MILLIS = 25L;

    /**
     * Default number of consecutive timeout failures required to trip the circuit open.
     */
    public static final int DEFAULT_FAILURE_THRESHOLD = 5;

    /**
     * Default cooldown duration in milliseconds before an open circuit attempts half-open reset.
     */
    public static final long DEFAULT_RESET_TIMEOUT_MILLIS = 1000L;

    private final long timeoutMillis;
    private final int failureThreshold;
    private final long resetTimeoutMillis;

    private final AtomicInteger consecutiveTimeouts;
    private final AtomicBoolean circuitOpen;
    private final AtomicLong lastTripTimestamp;
    private final AtomicLong totalProtectedRequests;
    private final AtomicLong totalTimeoutCount;
    private final AtomicLong totalCircuitTrips;

    /**
     * Constructs a guard with default 25ms deadline, 5 consecutive failure threshold, and 1000ms reset cooldown.
     */
    public CrossRegionAskTimeoutGuard() {
        this(DEFAULT_TIMEOUT_MILLIS, DEFAULT_FAILURE_THRESHOLD, DEFAULT_RESET_TIMEOUT_MILLIS);
    }

    /**
     * Constructs a guard with custom parameters.
     *
     * @param timeoutMillis maximum allowable latency in milliseconds
     * @param failureThreshold number of consecutive timeouts before circuit trips
     * @param resetTimeoutMillis cooldown before attempting reset
     */
    public CrossRegionAskTimeoutGuard(long timeoutMillis, int failureThreshold, long resetTimeoutMillis) {
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("timeoutMillis must be positive: " + timeoutMillis);
        }
        if (failureThreshold <= 0) {
            throw new IllegalArgumentException("failureThreshold must be positive: " + failureThreshold);
        }
        if (resetTimeoutMillis <= 0) {
            throw new IllegalArgumentException("resetTimeoutMillis must be positive: " + resetTimeoutMillis);
        }

        this.timeoutMillis = timeoutMillis;
        this.failureThreshold = failureThreshold;
        this.resetTimeoutMillis = resetTimeoutMillis;

        this.consecutiveTimeouts = new AtomicInteger(0);
        this.circuitOpen = new AtomicBoolean(false);
        this.lastTripTimestamp = new AtomicLong(0L);
        this.totalProtectedRequests = new AtomicLong(0L);
        this.totalTimeoutCount = new AtomicLong(0L);
        this.totalCircuitTrips = new AtomicLong(0L);
    }

    /**
     * Applies deadline timeout protection to the provided CompletableFuture using default timeout.
     *
     * @param <T> payload type
     * @param future target CompletableFuture to guard
     * @return guarded CompletableFuture
     */
    public <T> CompletableFuture<T> guard(CompletableFuture<T> future) {
        return guard(future, timeoutMillis);
    }

    /**
     * Applies deadline timeout protection to the provided CompletableFuture using a specified timeout.
     *
     * @param <T> payload type
     * @param future target CompletableFuture to guard
     * @param customTimeoutMillis timeout in milliseconds
     * @return guarded CompletableFuture
     */
    public <T> CompletableFuture<T> guard(CompletableFuture<T> future, long customTimeoutMillis) {
        Objects.requireNonNull(future, "future cannot be null");
        totalProtectedRequests.incrementAndGet();

        if (isCircuitOpen()) {
            future.completeExceptionally(
                    new IllegalStateException("Cross-region actor circuit breaker is OPEN (protection active)")
            );
            return future;
        }

        future.orTimeout(customTimeoutMillis, TimeUnit.MILLISECONDS);
        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                if (throwable instanceof java.util.concurrent.TimeoutException) {
                    recordTimeout();
                }
            } else {
                recordSuccess();
            }
        });

        return future;
    }

    /**
     * Checks if the circuit breaker is currently open (blocking new requests).
     *
     * @return {@code true} if open; {@code false} if closed
     */
    public boolean isCircuitOpen() {
        if (!circuitOpen.get()) {
            return false;
        }
        long now = System.currentTimeMillis();
        long tripTime = lastTripTimestamp.get();
        if (now - tripTime >= resetTimeoutMillis) {
            // Half-open transition attempt
            if (circuitOpen.compareAndSet(true, false)) {
                consecutiveTimeouts.set(0);
                return false;
            }
        }
        return true;
    }

    /**
     * Records a successful operation, resetting consecutive timeout tally.
     */
    public void recordSuccess() {
        consecutiveTimeouts.set(0);
    }

    /**
     * Records a timeout occurrence, incrementing failure tally and potentially tripping circuit.
     */
    public void recordTimeout() {
        totalTimeoutCount.incrementAndGet();
        int currentFails = consecutiveTimeouts.incrementAndGet();
        if (currentFails >= failureThreshold) {
            if (circuitOpen.compareAndSet(false, true)) {
                lastTripTimestamp.set(System.currentTimeMillis());
                totalCircuitTrips.incrementAndGet();
            }
        }
    }

    /**
     * Manually resets the circuit breaker back to closed healthy state.
     */
    public void reset() {
        consecutiveTimeouts.set(0);
        circuitOpen.set(false);
        lastTripTimestamp.set(0L);
    }

    /**
     * Returns the configured timeout deadline in milliseconds.
     *
     * @return timeout in milliseconds
     */
    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    /**
     * Returns the consecutive failure threshold.
     *
     * @return failure threshold
     */
    public int getFailureThreshold() {
        return failureThreshold;
    }

    /**
     * Returns the circuit reset cooldown duration in milliseconds.
     *
     * @return reset timeout in milliseconds
     */
    public long getResetTimeoutMillis() {
        return resetTimeoutMillis;
    }

    /**
     * Returns the current consecutive timeout count.
     *
     * @return count of consecutive timeouts
     */
    public int getConsecutiveTimeouts() {
        return consecutiveTimeouts.get();
    }

    /**
     * Returns cumulative count of protected requests.
     *
     * @return total requests guarded
     */
    public long getTotalProtectedRequests() {
        return totalProtectedRequests.get();
    }

    /**
     * Returns cumulative count of timed out requests.
     *
     * @return total timeouts
     */
    public long getTotalTimeoutCount() {
        return totalTimeoutCount.get();
    }

    /**
     * Returns cumulative count of circuit breaker trip events.
     *
     * @return total circuit trips
     */
    public long getTotalCircuitTrips() {
        return totalCircuitTrips.get();
    }
}
