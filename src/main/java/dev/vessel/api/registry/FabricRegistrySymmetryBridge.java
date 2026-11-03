package dev.vessel.api.registry;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Direct opcode receptor and ASM transmutation bridge for Fabric Registry.register() calls.
 *
 * <p>Safely intercepts Fabric mod registration calls during pre-flight boot and routes them directly
 * into the Vessel Universal Registry hub, bypassing premature frozen checks and dispatching
 * RegistryEntryAddedCallback notifications.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
public final class FabricRegistrySymmetryBridge {

    public static final String FABRIC_LOADER_KIND = "FABRIC";
    public static final int DEFAULT_MAP_CAPACITY = 64;

    private static final FabricRegistrySymmetryBridge INSTANCE = new FabricRegistrySymmetryBridge();

    private final VesselUniversalRegistry registryHub;
    private final Map<String, Set<FabricEntryAddedListener<Object>>> listenersByRegistry;
    private final Map<String, AtomicInteger> rawIdCounters;

    /**
     * Resolves the global singleton bridge instance.
     *
     * @return global bridge instance
     */
    public static FabricRegistrySymmetryBridge getInstance() {
        return INSTANCE;
    }

    /**
     * Constructs a Fabric registry symmetry bridge using the global universal registry.
     */
    public FabricRegistrySymmetryBridge() {
        this(VesselUniversalRegistry.getInstance());
    }

    /**
     * Constructs a Fabric registry symmetry bridge with a custom target universal registry.
     *
     * @param registryHub destination universal registry hub
     */
    public FabricRegistrySymmetryBridge(VesselUniversalRegistry registryHub) {
        this.registryHub = Objects.requireNonNull(registryHub, "registryHub cannot be null");
        this.listenersByRegistry = new ConcurrentHashMap<>(DEFAULT_MAP_CAPACITY);
        this.rawIdCounters = new ConcurrentHashMap<>(DEFAULT_MAP_CAPACITY);
    }

    /**
     * ASM target: Intercepts Fabric Registry.register(Registry, Identifier, Entry).
     *
     * @param registry   Fabric registry instance (or identifier string)
     * @param identifier namespaced identifier object (Identifier or ResourceLocation)
     * @param entry      registered object entry
     * @param <V>        entry value type
     * @return registered entry instance
     */
    @SuppressWarnings("unchecked")
    public static <V> V register(Object registry, Object identifier, V entry) {
        Objects.requireNonNull(registry, "registry cannot be null");
        Objects.requireNonNull(identifier, "identifier cannot be null");
        Objects.requireNonNull(entry, "entry cannot be null");

        return (V) getInstance().registerEntry(registry.toString(), identifier.toString(), entry);
    }

    /**
     * Direct registration entrypoint with string identifiers.
     *
     * @param registryKey     canonical registry identifier
     * @param entryIdentifier namespaced entry identifier
     * @param entry           registered instance
     * @return registered instance
     */
    public Object registerEntry(String registryKey, String entryIdentifier, Object entry) {
        Objects.requireNonNull(registryKey, "registryKey cannot be null");
        Objects.requireNonNull(entryIdentifier, "entryIdentifier cannot be null");
        Objects.requireNonNull(entry, "entry cannot be null");

        // 1. Mount into universal registry
        registryHub.registerEntry(registryKey, entryIdentifier, entry, FABRIC_LOADER_KIND);

        // 2. Generate raw numeric ID and dispatch to Fabric listeners
        AtomicInteger counter = rawIdCounters.computeIfAbsent(registryKey, k -> new AtomicInteger(1));
        int rawId = counter.getAndIncrement();

        notifyListeners(registryKey, rawId, entryIdentifier, entry);

        return entry;
    }

    /**
     * Registers a Fabric RegistryEntryAddedCallback listener for a specific registry.
     *
     * @param registryKey target registry key
     * @param listener    entry added listener
     * @param <T>         entry value type
     */
    @SuppressWarnings("unchecked")
    public <T> void addEntryListener(String registryKey, FabricEntryAddedListener<T> listener) {
        Objects.requireNonNull(registryKey, "registryKey cannot be null");
        Objects.requireNonNull(listener, "listener cannot be null");

        listenersByRegistry.computeIfAbsent(registryKey, k -> ConcurrentHashMap.newKeySet())
                .add((FabricEntryAddedListener<Object>) listener);
    }

    private void notifyListeners(String registryKey, int rawId, String id, Object entry) {
        Set<FabricEntryAddedListener<Object>> listeners = listenersByRegistry.get(registryKey);
        if (listeners != null && !listeners.isEmpty()) {
            for (FabricEntryAddedListener<Object> listener : listeners) {
                listener.onEntryAdded(rawId, id, entry);
            }
        }
    }

    /**
     * Resolves the current raw ID counter for a registry.
     *
     * @param registryKey canonical registry identifier
     * @return current counter value
     */
    public int getRawIdCounter(String registryKey) {
        Objects.requireNonNull(registryKey, "registryKey cannot be null");
        AtomicInteger counter = rawIdCounters.get(registryKey);
        return counter != null ? counter.get() : 0;
    }

    /**
     * Clears all listeners and counters (for testing).
     */
    public void clearAll() {
        listenersByRegistry.clear();
        rawIdCounters.clear();
    }
}
