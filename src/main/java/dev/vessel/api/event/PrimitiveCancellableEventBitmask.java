package dev.vessel.api.event;

/**
 * High-performance stack-based bitmask manager for cancellable event states.
 * <p>
 * Eliminates heap allocation during event cancellation checks by performing branchless
 * bitwise manipulation directly on primitive long variables residing on the JVM execution stack.
 *
 * @version 1.0.1
 */
public final class PrimitiveCancellableEventBitmask {

    public static final long CLEAN = 0L;
    public static final long CANCELLED = 1L << 0;
    public static final long CONSUMED = 1L << 1;
    public static final long PROPAGATION_STOPPED = 1L << 2;
    public static final long RESULT_DENY = 1L << 3;
    public static final long RESULT_DEFAULT = 1L << 4;
    public static final long RESULT_ALLOW = 1L << 5;

    private PrimitiveCancellableEventBitmask() {
        // Pure static utility class
    }

    public static boolean isCanceled(Object event) {
        return EventCancellationStackBridge.isCanceled(event);
    }

    public static void setCanceled(Object event, boolean cancel) {
        EventCancellationStackBridge.setCanceled(event, cancel);
    }

    public static boolean isCancelled(long bitmask) {
        return (bitmask & CANCELLED) != 0L;
    }

    public static long setCancelled(long bitmask, boolean cancelled) {
        return cancelled ? (bitmask | CANCELLED) : (bitmask & ~CANCELLED);
    }

    public static boolean isConsumed(long bitmask) {
        return (bitmask & CONSUMED) != 0L;
    }

    public static long setConsumed(long bitmask, boolean consumed) {
        return consumed ? (bitmask | CONSUMED) : (bitmask & ~CONSUMED);
    }

    public static boolean isPropagationStopped(long bitmask) {
        return (bitmask & PROPAGATION_STOPPED) != 0L;
    }

    public static long setPropagationStopped(long bitmask, boolean stopped) {
        return stopped ? (bitmask | PROPAGATION_STOPPED) : (bitmask & ~PROPAGATION_STOPPED);
    }

    public static boolean hasFlag(long bitmask, long flagBit) {
        return (bitmask & flagBit) != 0L;
    }

    public static long setFlag(long bitmask, long flagBit, boolean value) {
        return value ? (bitmask | flagBit) : (bitmask & ~flagBit);
    }

    public static long combine(long bitmaskA, long bitmaskB) {
        return bitmaskA | bitmaskB;
    }
}
