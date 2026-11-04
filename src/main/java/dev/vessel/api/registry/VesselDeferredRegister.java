package dev.vessel.api.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * Universal Deferred Register for the VESSEL Super-API.
 *
 * <p>Emulates the expressive, static-declaration power of MinecraftForge's {@code DeferredRegister}
 * while retaining the lightweight, zero-overhead execution profile of Fabric and Folia.</p>
 *
 * <p>Mod developers can declare blocks, items, entities, and capabilities at class initialization
 * time as static constants. The entries are bound into {@link VesselUniversalRegistry} cleanly
 * across all 8 supported loaders without race conditions or phase violations.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @param <T> the registry entry base type
 * @version 1.1.1
 * @since 1.0.0
 */
public final class VesselDeferredRegister<T> {

    private final VesselRegistryKey<T> registryKey;
    private final String namespace;
    private final List<VesselRegistrySupplier<? extends T>> entries;
    private volatile boolean registered;

    /**
     * Private constructor for controlled factory creation.
     *
     * @param registryKey the target registry key
     * @param namespace   mod namespace
     */
    private VesselDeferredRegister(VesselRegistryKey<T> registryKey, String namespace) {
        this.registryKey = Objects.requireNonNull(registryKey, "registryKey cannot be null");
        this.namespace = Objects.requireNonNull(namespace, "namespace cannot be null");
        this.entries = new CopyOnWriteArrayList<>();
        this.registered = false;
    }

    /**
     * Creates a new deferred register for a specified registry key and namespace.
     *
     * @param <E>         entry type
     * @param registryKey registry key
     * @param namespace   mod namespace
     * @return deferred register instance
     */
    public static <E> VesselDeferredRegister<E> create(VesselRegistryKey<E> registryKey, String namespace) {
        return new VesselDeferredRegister<>(registryKey, namespace);
    }

    /**
     * Registers a new lazy entry under this register's namespace.
     *
     * @param <I>      concrete entry type extending base type
     * @param id       entry identifier (e.g. {@code "plasma_blade"})
     * @param supplier instantiator supplier
     * @return lazy registry supplier
     */
    public <I extends T> VesselRegistrySupplier<I> register(String id, Supplier<? extends I> supplier) {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(supplier, "supplier cannot be null");
        VesselRegistrySupplier<I> holder = new VesselRegistrySupplier<>(namespace, id, supplier);
        entries.add(holder);

        if (registered) {
            bindEntryToHub(holder);
        }
        return holder;
    }

    /**
     * Binds all deferred entries into the global {@link VesselUniversalRegistry} hub.
     * Compatible with Forge/NeoForge mod event buses or stand-alone direct invocation.
     *
     * @param eventBus optional event bus (can be null for direct binding)
     */
    public void register(Object eventBus) {
        this.registered = true;
        for (VesselRegistrySupplier<? extends T> entry : entries) {
            bindEntryToHub(entry);
        }
    }

    /**
     * Convenience method to register without requiring an event bus parameter.
     */
    public void register() {
        register(null);
    }

    private void bindEntryToHub(VesselRegistrySupplier<?> entry) {
        VesselUniversalRegistry hub = VesselUniversalRegistry.getInstance();
        hub.registerEntry(
                registryKey.getLocation(),
                entry.getNamespacedId(),
                entry,
                VesselUniversalRegistry.LOADER_UNIVERSAL
        );
    }

    /**
     * Returns an unmodifiable view of all registered entries.
     *
     * @return list of entries
     */
    public List<VesselRegistrySupplier<? extends T>> getEntries() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    /**
     * Resolves the target registry key.
     *
     * @return registry key
     */
    public VesselRegistryKey<T> getRegistryKey() {
        return registryKey;
    }

    /**
     * Resolves the declaring mod namespace.
     *
     * @return namespace
     */
    public String getNamespace() {
        return namespace;
    }
}
