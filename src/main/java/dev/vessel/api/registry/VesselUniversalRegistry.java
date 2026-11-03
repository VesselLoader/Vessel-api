package dev.vessel.api.registry;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Native pre-flight universal registry hub for VESSEL Sovereign Server.
 *
 * <p>Consolidates mod registrations across Forge, NeoForge, Fabric, and Spigot before world startup,
 * linking directly with dynamic datapacks and preventing premature registry freeze collisions.</p>
 *
 * <p>Implements Domain 6 of the VESSEL Method Symmetry Dictionary specification.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
public final class VesselUniversalRegistry {

    public static final String REGISTRY_BLOCK = "minecraft:block";
    public static final String REGISTRY_ITEM = "minecraft:item";
    public static final String REGISTRY_ENTITY_TYPE = "minecraft:entity_type";
    public static final String REGISTRY_BIOME = "minecraft:biome";
    public static final String REGISTRY_SOUND_EVENT = "minecraft:sound_event";
    public static final String REGISTRY_ENCHANTMENT = "minecraft:enchantment";
    public static final String REGISTRY_DATA_COMPONENT_TYPE = "minecraft:data_component_type";
    public static final String REGISTRY_CUSTOM_STAT = "minecraft:custom_stat";

    public static final String LOADER_FORGE = "FORGE";
    public static final String LOADER_NEOFORGE = "NEOFORGE";
    public static final String LOADER_FABRIC = "FABRIC";
    public static final String LOADER_BUKKIT = "BUKKIT";
    public static final String LOADER_UNIVERSAL = "UNIVERSAL";

    public static final int DEFAULT_REGISTRY_CAPACITY = 64;
    public static final int DEFAULT_ENTRIES_PER_REGISTRY = 256;

    private static final VesselUniversalRegistry GLOBAL_INSTANCE = new VesselUniversalRegistry();

    private final Map<String, Map<String, RegistryEntryHolder>> registryTables;
    private final Map<String, Set<RegistryCallbackHandler>> registryCallbacks;
    private final AtomicReference<RegistryLifecyclePhase> currentPhase;

    /**
     * Resolves the singleton global instance of the universal registry hub.
     *
     * @return global registry hub
     */
    public static VesselUniversalRegistry getInstance() {
        return GLOBAL_INSTANCE;
    }

    /**
     * Constructs a standalone registry hub pre-populated with standard registries in PRE_FLIGHT phase.
     */
    public VesselUniversalRegistry() {
        this.registryTables = new ConcurrentHashMap<>(DEFAULT_REGISTRY_CAPACITY);
        this.registryCallbacks = new ConcurrentHashMap<>(DEFAULT_REGISTRY_CAPACITY);
        this.currentPhase = new AtomicReference<>(RegistryLifecyclePhase.PRE_FLIGHT);

        initializeStandardRegistries();
    }

    private void initializeStandardRegistries() {
        registerRegistryTable(REGISTRY_BLOCK);
        registerRegistryTable(REGISTRY_ITEM);
        registerRegistryTable(REGISTRY_ENTITY_TYPE);
        registerRegistryTable(REGISTRY_BIOME);
        registerRegistryTable(REGISTRY_SOUND_EVENT);
        registerRegistryTable(REGISTRY_ENCHANTMENT);
        registerRegistryTable(REGISTRY_DATA_COMPONENT_TYPE);
        registerRegistryTable(REGISTRY_CUSTOM_STAT);
    }

    /**
     * Initializes a named registry table under universal surveillance.
     *
     * @param registryKey namespaced registry identifier
     */
    public void registerRegistryTable(String registryKey) {
        Objects.requireNonNull(registryKey, "registryKey cannot be null");
        registryTables.putIfAbsent(registryKey, new ConcurrentHashMap<>(DEFAULT_ENTRIES_PER_REGISTRY));
    }

    /**
     * Registers a universal entry directly into a specific registry table.
     *
     * @param registryKey canonical registry identifier
     * @param entryKey    namespaced entry identifier
     * @param entry       registered instance or supplier
     * @param loaderKind  originating loader dialect
     * @return registered entry holder
     */
    public RegistryEntryHolder registerEntry(String registryKey, String entryKey, Object entry, String loaderKind) {
        Objects.requireNonNull(registryKey, "registryKey cannot be null");
        Objects.requireNonNull(entryKey, "entryKey cannot be null");
        Objects.requireNonNull(entry, "entry cannot be null");
        Objects.requireNonNull(loaderKind, "loaderKind cannot be null");

        if (!currentPhase.get().isRegistrationAllowed()) {
            throw new IllegalStateException("Cannot register entry '" + entryKey
                    + "' in registry '" + registryKey + "' while in phase " + currentPhase.get());
        }

        Map<String, RegistryEntryHolder> table = registryTables.computeIfAbsent(
                registryKey, k -> new ConcurrentHashMap<>(DEFAULT_ENTRIES_PER_REGISTRY));

        RegistryEntryHolder holder = new RegistryEntryHolder(
                registryKey,
                entryKey,
                entry,
                loaderKind,
                System.currentTimeMillis()
        );

        table.put(entryKey, holder);
        notifyCallbacks(registryKey, entryKey, entry);
        return holder;
    }

    /**
     * ASM target: Registers a legacy Forge entry (MC 1.7.10 - 1.12.2 GameRegistry.register).
     *
     * @param entry raw forge registry entry
     */
    public static void registerLegacyForgeEntry(Object entry) {
        Objects.requireNonNull(entry, "entry cannot be null");
        String entryName = entry.toString();
        getInstance().registerEntry(REGISTRY_ITEM, entryName, entry, LOADER_FORGE);
    }

    /**
     * ASM target: Registers a modern Forge DeferredRegister entry (MC 1.16.5 - 1.20.1).
     *
     * @param deferredRegister DeferredRegister instance reference
     * @param name             registry entry name
     * @param supplier         lazy supplier supplying the object
     * @return registry entry holder
     */
    public static Object registerForgeDeferred(Object deferredRegister, String name, Supplier<?> supplier) {
        Objects.requireNonNull(deferredRegister, "deferredRegister cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(supplier, "supplier cannot be null");

        String registryKey = deferredRegister.toString();
        if (!registryKey.contains(":")) {
            registryKey = REGISTRY_ITEM;
        }
        return getInstance().registerEntry(registryKey, name, supplier, LOADER_FORGE);
    }

    /**
     * ASM target: Mounts a DeferredRegister to an EventBus (MC 1.16.5 - 1.20.1).
     *
     * @param deferredRegister DeferredRegister instance
     * @param eventBus         IEventBus instance
     */
    public static void mountDeferredRegister(Object deferredRegister, Object eventBus) {
        Objects.requireNonNull(deferredRegister, "deferredRegister cannot be null");
        Objects.requireNonNull(eventBus, "eventBus cannot be null");
        // No-op or notification in universal registry; deferred entries are already tracked
    }

    /**
     * ASM target: Registers a Fabric entry (Registry.register(registry, id, entry)).
     *
     * @param registry   registry instance
     * @param identifier namespaced identifier
     * @param entry      registered entry value
     * @return registered entry value
     */
    public static Object registerFabricEntry(Object registry, Object identifier, Object entry) {
        Objects.requireNonNull(registry, "registry cannot be null");
        Objects.requireNonNull(identifier, "identifier cannot be null");
        Objects.requireNonNull(entry, "entry cannot be null");

        String registryKey = registry.toString();
        String entryKey = identifier.toString();

        getInstance().registerEntry(registryKey, entryKey, entry, LOADER_FABRIC);
        return entry;
    }

    /**
     * ASM target: Registers a NeoForge entry via RegisterEvent (MC 1.21.1).
     *
     * @param registerEvent    RegisterEvent instance
     * @param resourceKey      target registry ResourceKey
     * @param resourceLocation namespaced entry identifier
     * @param supplier         entry supplier
     */
    public static void registerNeoForgeEntry(Object registerEvent, Object resourceKey, Object resourceLocation, Supplier<?> supplier) {
        Objects.requireNonNull(resourceKey, "resourceKey cannot be null");
        Objects.requireNonNull(resourceLocation, "resourceLocation cannot be null");
        Objects.requireNonNull(supplier, "supplier cannot be null");

        String regKey = resourceKey.toString();
        String entryKey = resourceLocation.toString();

        getInstance().registerEntry(regKey, entryKey, supplier, LOADER_NEOFORGE);
    }

    /**
     * ASM target: Registers a NeoForge entry via Consumer helper (MC 1.21.1).
     *
     * @param registerEvent  RegisterEvent instance
     * @param resourceKey    target registry ResourceKey
     * @param consumerHelper consumer callback helper
     */
    @SuppressWarnings("unchecked")
    public static void registerNeoForgeConsumer(Object registerEvent, Object resourceKey, Object consumerHelper) {
        Objects.requireNonNull(resourceKey, "resourceKey cannot be null");
        Objects.requireNonNull(consumerHelper, "consumerHelper cannot be null");

        if (consumerHelper instanceof Consumer consumer) {
            consumer.accept(registerEvent);
        }
    }

    /**
     * Resolves a registered entry by registry key and entry key.
     *
     * @param registryKey canonical registry identifier
     * @param entryKey    namespaced entry identifier
     * @return Optional containing the registered value if found
     */
    public Optional<Object> getEntry(String registryKey, String entryKey) {
        Objects.requireNonNull(registryKey, "registryKey cannot be null");
        Objects.requireNonNull(entryKey, "entryKey cannot be null");

        Map<String, RegistryEntryHolder> table = registryTables.get(registryKey);
        if (table == null) {
            return Optional.empty();
        }
        RegistryEntryHolder holder = table.get(entryKey);
        if (holder == null) {
            return Optional.empty();
        }

        Object val = holder.value();
        if (val instanceof Supplier<?> sup) {
            return Optional.ofNullable(sup.get());
        }
        return Optional.of(val);
    }

    /**
     * Resolves the raw RegistryEntryHolder metadata wrapper.
     *
     * @param registryKey canonical registry identifier
     * @param entryKey    namespaced entry identifier
     * @return Optional containing holder
     */
    public Optional<RegistryEntryHolder> getEntryHolder(String registryKey, String entryKey) {
        Objects.requireNonNull(registryKey, "registryKey cannot be null");
        Objects.requireNonNull(entryKey, "entryKey cannot be null");

        Map<String, RegistryEntryHolder> table = registryTables.get(registryKey);
        if (table == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(table.get(entryKey));
    }

    /**
     * Checks if an entry is registered.
     *
     * @param registryKey canonical registry identifier
     * @param entryKey    namespaced entry identifier
     * @return true if entry exists
     */
    public boolean hasEntry(String registryKey, String entryKey) {
        Objects.requireNonNull(registryKey, "registryKey cannot be null");
        Objects.requireNonNull(entryKey, "entryKey cannot be null");

        Map<String, RegistryEntryHolder> table = registryTables.get(registryKey);
        return table != null && table.containsKey(entryKey);
    }

    /**
     * Resolves an unmodifiable map of all entries in a specific registry.
     *
     * @param registryKey canonical registry identifier
     * @return map of entryKey -> RegistryEntryHolder
     */
    public Map<String, RegistryEntryHolder> getEntriesForRegistry(String registryKey) {
        Objects.requireNonNull(registryKey, "registryKey cannot be null");
        Map<String, RegistryEntryHolder> table = registryTables.get(registryKey);
        if (table == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(table);
    }

    /**
     * Resolves the set of all monitored registry identifiers.
     *
     * @return unmodifiable set of registry keys
     */
    public Set<String> getAllRegistries() {
        return Collections.unmodifiableSet(registryTables.keySet());
    }

    /**
     * Calculates total registered entries across all registries.
     *
     * @return total entry count
     */
    public int totalRegisteredEntries() {
        int total = 0;
        for (Map<String, RegistryEntryHolder> table : registryTables.values()) {
            total += table.size();
        }
        return total;
    }

    /**
     * Registers a callback handler for registry entry additions.
     *
     * @param registryKey target registry identifier
     * @param handler     callback handler
     */
    public void registerCallback(String registryKey, RegistryCallbackHandler handler) {
        Objects.requireNonNull(registryKey, "registryKey cannot be null");
        Objects.requireNonNull(handler, "handler cannot be null");

        registryCallbacks.computeIfAbsent(registryKey, k -> ConcurrentHashMap.newKeySet())
                .add(handler);
    }

    private void notifyCallbacks(String registryKey, String entryKey, Object entry) {
        Set<RegistryCallbackHandler> handlers = registryCallbacks.get(registryKey);
        if (handlers != null && !handlers.isEmpty()) {
            for (RegistryCallbackHandler handler : handlers) {
                handler.onEntryAdded(registryKey, entryKey, entry);
            }
        }
    }

    /**
     * Transitions the registry into FROZEN state post-boot.
     */
    public void freezeRegistry() {
        currentPhase.set(RegistryLifecyclePhase.FROZEN);
    }

    /**
     * Unfreezes registry during dynamic datapack reload.
     */
    public void unfreezeForReload() {
        currentPhase.set(RegistryLifecyclePhase.DYNAMIC_RELOAD);
    }

    /**
     * Returns the current lifecycle phase.
     *
     * @return current phase
     */
    public RegistryLifecyclePhase currentPhase() {
        return currentPhase.get();
    }

    /**
     * Clears all entries and resets to PRE_FLIGHT state (for testing).
     */
    public void clearAll() {
        registryTables.clear();
        registryCallbacks.clear();
        currentPhase.set(RegistryLifecyclePhase.PRE_FLIGHT);
        initializeStandardRegistries();
    }
}
