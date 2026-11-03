package dev.vessel.api.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * Replay engine and coordinator for Forge and NeoForge DeferredRegister instances.
 *
 * <p>Captures static mod registrations queued during class loading and executes
 * deterministic pre-flight replaying into the Vessel Universal Registry.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
public final class DeferredRegisterSymmetryReplay {

    public static final String DEFAULT_MOD_NAMESPACE = "vessel";

    private final Map<String, List<DeferredRegistrationEntry>> deferredQueueByRegistry;

    /**
     * Constructs a deferred register replay coordinator.
     */
    public DeferredRegisterSymmetryReplay() {
        this.deferredQueueByRegistry = new ConcurrentHashMap<>();
    }

    /**
     * Enqueues a deferred registry entry supplier for future replay.
     *
     * @param registryKey canonical registry identifier (e.g., "minecraft:item")
     * @param entryKey    namespaced entry name (e.g., "create:wrench")
     * @param supplier    supplier yielding the entry instance
     * @param loaderKind  loader dialect (FORGE or NEOFORGE)
     */
    public void enqueueDeferredEntry(String registryKey, String entryKey, Supplier<?> supplier, String loaderKind) {
        Objects.requireNonNull(registryKey, "registryKey cannot be null");
        Objects.requireNonNull(entryKey, "entryKey cannot be null");
        Objects.requireNonNull(supplier, "supplier cannot be null");
        Objects.requireNonNull(loaderKind, "loaderKind cannot be null");

        deferredQueueByRegistry.computeIfAbsent(registryKey, k -> new CopyOnWriteArrayList<>())
                .add(new DeferredRegistrationEntry(registryKey, entryKey, supplier, loaderKind));
    }

    /**
     * Replays all queued deferred registrations into the target universal registry hub.
     *
     * @param targetRegistry destination universal registry
     * @return replay execution result metrics
     */
    public RegistryReplayResult replayAll(VesselUniversalRegistry targetRegistry) {
        Objects.requireNonNull(targetRegistry, "targetRegistry cannot be null");

        long startNanos = System.nanoTime();
        int total = 0;
        int success = 0;
        int failure = 0;

        for (Map.Entry<String, List<DeferredRegistrationEntry>> entryList : deferredQueueByRegistry.entrySet()) {
            String registryKey = entryList.getKey();
            targetRegistry.registerRegistryTable(registryKey);

            for (DeferredRegistrationEntry entry : entryList.getValue()) {
                total++;
                try {
                    Object resolved = entry.supplier().get();
                    if (resolved != null) {
                        targetRegistry.registerEntry(
                                entry.registryKey(),
                                entry.entryKey(),
                                resolved,
                                entry.loaderKind()
                        );
                        success++;
                    } else {
                        failure++;
                    }
                } catch (Exception e) {
                    failure++;
                }
            }
        }

        long duration = System.nanoTime() - startNanos;
        return new RegistryReplayResult(total, success, failure, duration);
    }

    /**
     * Replays deferred registrations for a single specific registry key.
     *
     * @param registryKey    target registry key
     * @param targetRegistry destination universal registry
     * @return replay execution result metrics
     */
    public RegistryReplayResult replayForRegistry(String registryKey, VesselUniversalRegistry targetRegistry) {
        Objects.requireNonNull(registryKey, "registryKey cannot be null");
        Objects.requireNonNull(targetRegistry, "targetRegistry cannot be null");

        long startNanos = System.nanoTime();
        List<DeferredRegistrationEntry> list = deferredQueueByRegistry.get(registryKey);
        if (list == null || list.isEmpty()) {
            return new RegistryReplayResult(0, 0, 0, System.nanoTime() - startNanos);
        }

        targetRegistry.registerRegistryTable(registryKey);
        int total = list.size();
        int success = 0;
        int failure = 0;

        for (DeferredRegistrationEntry entry : list) {
            try {
                Object resolved = entry.supplier().get();
                if (resolved != null) {
                    targetRegistry.registerEntry(
                            entry.registryKey(),
                            entry.entryKey(),
                            resolved,
                            entry.loaderKind()
                    );
                    success++;
                } else {
                    failure++;
                }
            } catch (Exception e) {
                failure++;
            }
        }

        long duration = System.nanoTime() - startNanos;
        return new RegistryReplayResult(total, success, failure, duration);
    }

    /**
     * Total count of pending deferred entries across all registries.
     *
     * @return queued entry count
     */
    public int totalQueuedEntries() {
        int count = 0;
        for (List<DeferredRegistrationEntry> list : deferredQueueByRegistry.values()) {
            count += list.size();
        }
        return count;
    }

    /**
     * Clears all queued deferred registrations.
     */
    public void clearQueue() {
        deferredQueueByRegistry.clear();
    }
}
