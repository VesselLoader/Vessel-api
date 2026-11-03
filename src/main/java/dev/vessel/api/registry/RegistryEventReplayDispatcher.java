package dev.vessel.api.registry;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Event replay dispatcher delivering retroactive registry event replay for late-loading
 * plugins and dynamic datapack reloads under VUSA Chapter 3.4.
 *
 * <p>Eliminates timing mismatches when mods or datapacks initialize asynchronously after the
 * initial server boot phase by guaranteeing strict FIFO retroactive replay.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0
 */
public final class RegistryEventReplayDispatcher {

    private final Map<String, List<Object>> recordedEntries = new ConcurrentHashMap<>();
    private final Map<String, List<Consumer<Object>>> subscribers = new ConcurrentHashMap<>();

    /**
     * Constructs a new registry event replay dispatcher.
     */
    public RegistryEventReplayDispatcher() {
    }

    /**
     * Records a newly registered entry under the specified registry and broadcasts it to any
     * currently active subscribers.
     *
     * @param registryName namespaced registry identifier (e.g. "minecraft:block")
     * @param entry        the registered object or descriptor
     */
    public void recordEntry(String registryName, Object entry) {
        Objects.requireNonNull(registryName, "registryName cannot be null");
        Objects.requireNonNull(entry, "entry cannot be null");

        List<Object> entries = this.recordedEntries.computeIfAbsent(
                registryName,
                k -> new CopyOnWriteArrayList<>()
        );
        entries.add(entry);

        List<Consumer<Object>> subs = this.subscribers.get(registryName);
        if (subs != null) {
            for (Consumer<Object> subscriber : subs) {
                subscriber.accept(entry);
            }
        }
    }

    /**
     * Registers a replay subscriber for the specified registry.
     * <p>Immediately retroactively replays all previously recorded entries to the subscriber in FIFO order.</p>
     *
     * @param registryName namespaced registry identifier
     * @param subscriber   consumer receiving registered entries
     */
    public void registerReplaySubscriber(String registryName, Consumer<Object> subscriber) {
        Objects.requireNonNull(registryName, "registryName cannot be null");
        Objects.requireNonNull(subscriber, "subscriber cannot be null");

        List<Consumer<Object>> subs = this.subscribers.computeIfAbsent(
                registryName,
                k -> new CopyOnWriteArrayList<>()
        );
        subs.add(subscriber);

        // Immediate retroactive replay of historical entries
        List<Object> entries = this.recordedEntries.get(registryName);
        if (entries != null) {
            for (Object entry : entries) {
                subscriber.accept(entry);
            }
        }
    }

    /**
     * Replays all recorded entries for a given registry to all currently registered subscribers.
     *
     * @param registryName namespaced registry identifier
     */
    public void replayAll(String registryName) {
        Objects.requireNonNull(registryName, "registryName cannot be null");

        List<Object> entries = this.recordedEntries.get(registryName);
        List<Consumer<Object>> subs = this.subscribers.get(registryName);

        if (entries == null || subs == null || entries.isEmpty() || subs.isEmpty()) {
            return;
        }

        for (Object entry : entries) {
            for (Consumer<Object> subscriber : subs) {
                subscriber.accept(entry);
            }
        }
    }

    /**
     * Replays all recorded entries of a registry to a specific targeted consumer without adding it
     * as a permanent subscriber.
     *
     * @param registryName     namespaced registry identifier
     * @param targetSubscriber targeted consumer callback
     */
    public void replayAllTo(String registryName, Consumer<Object> targetSubscriber) {
        Objects.requireNonNull(registryName, "registryName cannot be null");
        Objects.requireNonNull(targetSubscriber, "targetSubscriber cannot be null");

        List<Object> entries = this.recordedEntries.get(registryName);
        if (entries != null) {
            for (Object entry : entries) {
                targetSubscriber.accept(entry);
            }
        }
    }

    /**
     * Returns the count of recorded entries for the given registry.
     *
     * @param registryName namespaced registry identifier
     * @return entry count
     */
    public int getRecordedEntryCount(String registryName) {
        if (registryName == null) {
            return 0;
        }
        List<Object> entries = this.recordedEntries.get(registryName);
        return entries != null ? entries.size() : 0;
    }

    /**
     * Returns the number of active replay subscribers for the given registry.
     *
     * @param registryName namespaced registry identifier
     * @return subscriber count
     */
    public int getSubscriberCount(String registryName) {
        if (registryName == null) {
            return 0;
        }
        List<Consumer<Object>> subs = this.subscribers.get(registryName);
        return subs != null ? subs.size() : 0;
    }

    /**
     * Clears all active subscribers for the given registry.
     *
     * @param registryName namespaced registry identifier
     */
    public void clearSubscribers(String registryName) {
        if (registryName != null) {
            this.subscribers.remove(registryName);
        }
    }

    /**
     * Clears all recorded entries and subscribers across all registries.
     */
    public void clearAll() {
        this.recordedEntries.clear();
        this.subscribers.clear();
    }
}
