package dev.vessel.api.datapack;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Universal resource and datapack reload dispatcher coordinating {@code /reload} lifecycle events
 * across Fabric, Forge, and NeoForge reload listeners under Wave 3 &amp; 4.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0
 */
public final class ResourceReloadDispatcher {

    private final List<Object> rawListeners = new CopyOnWriteArrayList<>();
    private final Map<String, Consumer<Object>> namedCallbacks = new ConcurrentHashMap<>();
    private final AtomicInteger reloadCount = new AtomicInteger(0);
    private final AtomicLong lastReloadTimestamp = new AtomicLong(0L);

    /**
     * Constructs a new resource reload dispatcher.
     */
    public ResourceReloadDispatcher() {
    }

    /**
     * Registers an arbitrary reload listener object (such as a Fabric IdentifiableResourceReloadListener
     * or Forge ISelectiveResourceReloadListener).
     *
     * @param listener reload listener instance
     */
    public void registerReloadListener(Object listener) {
        Objects.requireNonNull(listener, "reload listener cannot be null");
        this.rawListeners.add(listener);
    }

    /**
     * Registers a named functional reload callback.
     *
     * @param listenerId unique listener identifier
     * @param callback   consumer receiving the active ResourceManager during reload
     */
    public void registerReloadListener(String listenerId, Consumer<Object> callback) {
        Objects.requireNonNull(listenerId, "listenerId cannot be null");
        Objects.requireNonNull(callback, "callback cannot be null");
        this.namedCallbacks.put(listenerId, callback);
    }

    /**
     * Dispatches a reload event to all registered listeners and callbacks.
     *
     * @param resourceManager active resource manager context
     */
    public void dispatchReload(Object resourceManager) {
        long timestamp = System.currentTimeMillis();
        this.lastReloadTimestamp.set(timestamp);
        this.reloadCount.incrementAndGet();

        // 1. Dispatch to functional named callbacks
        for (Consumer<Object> callback : this.namedCallbacks.values()) {
            try {
                callback.accept(resourceManager);
            } catch (Throwable ignored) {
                // Keep dispatch loop resilient across mod errors
            }
        }

        // 2. Dispatch to raw listener objects
        for (Object listener : this.rawListeners) {
            if (listener instanceof Consumer<?>) {
                @SuppressWarnings("unchecked")
                Consumer<Object> consumer = (Consumer<Object>) listener;
                try {
                    consumer.accept(resourceManager);
                } catch (Throwable ignored) {
                }
            } else {
                invokeReflectiveReload(listener, resourceManager);
            }
        }
    }

    private void invokeReflectiveReload(Object listener, Object resourceManager) {
        try {
            Method reloadMethod = null;
            for (Method m : listener.getClass().getMethods()) {
                if (("onResourceManagerReload".equals(m.getName()) || "reload".equals(m.getName()))
                        && m.getParameterCount() == 1) {
                    reloadMethod = m;
                    break;
                }
            }
            if (reloadMethod != null) {
                reloadMethod.invoke(listener, resourceManager);
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * Unregisters a named reload listener callback.
     *
     * @param listenerId unique listener identifier
     * @return true if found and removed
     */
    public boolean unregisterReloadListener(String listenerId) {
        if (listenerId == null) {
            return false;
        }
        return this.namedCallbacks.remove(listenerId) != null;
    }

    /**
     * Returns total number of registered reload listeners (named callbacks + raw listener objects).
     *
     * @return active listener count
     */
    public int getListenerCount() {
        return this.namedCallbacks.size() + this.rawListeners.size();
    }

    /**
     * Returns total count of reload operations performed.
     *
     * @return reload count
     */
    public int getReloadCount() {
        return this.reloadCount.get();
    }

    /**
     * Returns epoch millisecond timestamp of the last executed reload.
     *
     * @return epoch milliseconds, or 0 if never reloaded
     */
    public long getLastReloadTimestamp() {
        return this.lastReloadTimestamp.get();
    }

    /**
     * Clears all registered reload listeners and callbacks.
     */
    public void clearListeners() {
        this.namedCallbacks.clear();
        this.rawListeners.clear();
    }
}
