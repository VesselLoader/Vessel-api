package dev.vessel.api.capability;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Sentinel tracking capability references via weak references organized by chunk coordinates,
 * automatically invalidating and evicting capabilities when chunks unload to prevent memory leaks.
 *
 * <p>Addresses Forge's LazyOptional chunk unload leak issue under VUSA Chapter 3.2.</p>
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
public final class WeakCapabilitySentinel {

    private final Map<Long, List<WeakReference<VesselUnifiedCapability<?>>>> chunkRegistry;

    /**
     * Constructs a new sentinel with default concurrent storage.
     */
    public WeakCapabilitySentinel() {
        this.chunkRegistry = new ConcurrentHashMap<>();
    }

    /**
     * Packs chunk coordinates (chunkX, chunkZ) into a single 64-bit coordinate key.
     *
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @return 64-bit packed chunk position key
     */
    public static long packChunkPos(int chunkX, int chunkZ) {
        return (((long) chunkX) << 32) | (((long) chunkZ) & 0xFFFFFFFFL);
    }

    /**
     * Registers a capability under the specified chunk coordinates.
     *
     * @param chunkX     chunk X coordinate
     * @param chunkZ     chunk Z coordinate
     * @param capability unified capability instance to register
     */
    public void registerCapability(int chunkX, int chunkZ, VesselUnifiedCapability<?> capability) {
        registerCapability(packChunkPos(chunkX, chunkZ), capability);
    }

    /**
     * Registers a capability under a packed chunk coordinate key.
     *
     * @param chunkKey   64-bit packed chunk key
     * @param capability unified capability instance to register
     */
    public void registerCapability(long chunkKey, VesselUnifiedCapability<?> capability) {
        Objects.requireNonNull(capability, "capability cannot be null");
        chunkRegistry.computeIfAbsent(chunkKey, k -> new CopyOnWriteArrayList<>())
                .add(new WeakReference<>(capability));
    }

    /**
     * Evicts and invalidates all capabilities registered within the specified chunk coordinates.
     *
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @return count of capabilities that were actively invalidated
     */
    public int evictChunkCapabilities(int chunkX, int chunkZ) {
        return evictChunkCapabilities(packChunkPos(chunkX, chunkZ));
    }

    /**
     * Evicts and invalidates all capabilities registered within the specified packed chunk key.
     *
     * @param chunkKey 64-bit packed chunk coordinate key
     * @return count of capabilities that were actively invalidated
     */
    public int evictChunkCapabilities(long chunkKey) {
        List<WeakReference<VesselUnifiedCapability<?>>> list = chunkRegistry.remove(chunkKey);
        if (list == null || list.isEmpty()) {
            return 0;
        }

        int invalidated = 0;
        for (WeakReference<VesselUnifiedCapability<?>> ref : list) {
            VesselUnifiedCapability<?> cap = ref.get();
            if (cap != null && cap.isValid()) {
                cap.invalidate();
                invalidated++;
            }
        }
        return invalidated;
    }

    /**
     * Purges collected weak references that have been reclaimed by the garbage collector.
     *
     * @return count of dead references removed
     */
    public int purgeDeadReferences() {
        int purged = 0;
        for (Map.Entry<Long, List<WeakReference<VesselUnifiedCapability<?>>>> entry : chunkRegistry.entrySet()) {
            List<WeakReference<VesselUnifiedCapability<?>>> list = entry.getValue();
            boolean removed = list.removeIf(ref -> ref.get() == null);
            if (removed) {
                purged++;
            }
            if (list.isEmpty()) {
                chunkRegistry.remove(entry.getKey(), list);
            }
        }
        return purged;
    }

    /**
     * Returns the number of chunks currently tracked by this sentinel.
     *
     * @return tracked chunk count
     */
    public int getTrackedChunkCount() {
        return chunkRegistry.size();
    }

    /**
     * Returns the total count of active, valid capabilities across all chunks.
     *
     * @return active capability count
     */
    public int getActiveCapabilityCount() {
        int total = 0;
        for (List<WeakReference<VesselUnifiedCapability<?>>> list : chunkRegistry.values()) {
            for (WeakReference<VesselUnifiedCapability<?>> ref : list) {
                VesselUnifiedCapability<?> cap = ref.get();
                if (cap != null && cap.isValid()) {
                    total++;
                }
            }
        }
        return total;
    }

    /**
     * Clears and invalidates all registered capabilities across all chunks.
     */
    public void clearAll() {
        for (Long chunkKey : chunkRegistry.keySet()) {
            evictChunkCapabilities(chunkKey);
        }
        chunkRegistry.clear();
    }
}
