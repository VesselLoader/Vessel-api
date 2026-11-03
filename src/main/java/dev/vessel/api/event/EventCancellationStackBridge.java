package dev.vessel.api.event;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Stack-allocated primitive bitmask manipulator and object cancellation bridge.
 * <p>
 * Implements Domain 2 Event symmetry specifications from {@code VESSEL-METHOD-SYMMETRY-DICTIONARY-v1.0.md}.
 * Provides zero-heap-allocation bitmask manipulation on the execution stack and reflective/polymorphic
 * bridge operations for cross-loader event cancellation.
 *
 * @version 1.0.1
 */
public final class EventCancellationStackBridge {

    public static final long CLEAN = 0L;
    public static final long CANCELLED = 1L << 0;
    public static final long CONSUMED = 1L << 1;
    public static final long PROPAGATION_STOPPED = 1L << 2;
    public static final long RESULT_DENY = 1L << 3;
    public static final long RESULT_DEFAULT = 1L << 4;
    public static final long RESULT_ALLOW = 1L << 5;

    private EventCancellationStackBridge() {
        // Pure static utility facade
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

    public static boolean isDenied(long bitmask) {
        return (bitmask & RESULT_DENY) != 0L;
    }

    public static long setDenied(long bitmask, boolean denied) {
        return denied ? (bitmask | RESULT_DENY) : (bitmask & ~RESULT_DENY);
    }

    public static boolean isAllowed(long bitmask) {
        return (bitmask & RESULT_ALLOW) != 0L;
    }

    public static long setAllowed(long bitmask, boolean allowed) {
        return allowed ? (bitmask | RESULT_ALLOW) : (bitmask & ~RESULT_ALLOW);
    }

    public static boolean isDefault(long bitmask) {
        return (bitmask & RESULT_DEFAULT) != 0L;
    }

    public static long setDefault(long bitmask, boolean def) {
        return def ? (bitmask | RESULT_DEFAULT) : (bitmask & ~RESULT_DEFAULT);
    }

    public static boolean isCanceled(Object event) {
        if (event == null) {
            return false;
        }
        if (event instanceof VesselCancellable cancellable) {
            return cancellable.isCancelled();
        }
        try {
            Method m = event.getClass().getMethod("isCancelled");
            Object res = m.invoke(event);
            return res instanceof Boolean b && b;
        } catch (ReflectiveOperationException ignored) {
            // Check alternative spelling 'isCanceled'
        }
        try {
            Method m = event.getClass().getMethod("isCanceled");
            Object res = m.invoke(event);
            return res instanceof Boolean b && b;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    public static void setCanceled(Object event, boolean cancel) {
        if (event == null) {
            return;
        }
        if (event instanceof VesselCancellable cancellable) {
            cancellable.setCancelled(cancel);
            return;
        }
        try {
            Method m = event.getClass().getMethod("setCancelled", boolean.class);
            m.invoke(event, cancel);
            return;
        } catch (ReflectiveOperationException ignored) {
            // Check alternative spelling 'setCanceled'
        }
        try {
            Method m = event.getClass().getMethod("setCanceled", boolean.class);
            m.invoke(event, cancel);
        } catch (ReflectiveOperationException ignored) {
            // Not a cancellable event type
        }
    }
}
