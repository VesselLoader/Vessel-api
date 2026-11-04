package dev.vessel.api.registry;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Thread-safe immutable lazy holder for an entry registered through {@link VesselDeferredRegister}.
 *
 * <p>Provides immediate handle resolution post-registration while maintaining lazy initialization
 * until the core registry pipeline freezes the entry.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @param <T> the registered entry type
 * @version 1.1.0
 * @since 1.0.0
 */
public final class VesselRegistrySupplier<T> implements Supplier<T> {

    private final String id;
    private final String namespace;
    private final Supplier<? extends T> factory;
    private volatile T resolvedValue;

    /**
     * Constructs a lazy registry supplier.
     *
     * @param namespace mod namespace
     * @param id        registry entry identifier
     * @param factory   entry instantiator factory
     */
    public VesselRegistrySupplier(String namespace, String id, Supplier<? extends T> factory) {
        this.namespace = Objects.requireNonNull(namespace, "namespace cannot be null");
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.factory = Objects.requireNonNull(factory, "factory cannot be null");
    }

    /**
     * Resolves the entry identifier within the mod namespace.
     *
     * @return entry id
     */
    public String getId() {
        return id;
    }

    /**
     * Resolves the declaring mod namespace.
     *
     * @return namespace
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Returns the full namespaced string key (e.g. {@code "mymod:quantum_generator"}).
     *
     * @return canonical namespaced identifier
     */
    public String getNamespacedId() {
        return namespace + ":" + id;
    }

    /**
     * Resolves and caches the materialized object.
     *
     * @return instantiated entry
     */
    @Override
    public T get() {
        T val = resolvedValue;
        if (val == null) {
            synchronized (this) {
                val = resolvedValue;
                if (val == null) {
                    val = factory.get();
                    resolvedValue = Objects.requireNonNull(val, "Factory produced null for entry: " + getNamespacedId());
                }
            }
        }
        return val;
    }

    /**
     * Checks whether the underlying entry has been materialized.
     *
     * @return true if initialized
     */
    public boolean isPresent() {
        return resolvedValue != null;
    }

    /**
     * Returns an optional containing the resolved entry if initialized.
     *
     * @return optional entry
     */
    public Optional<T> asOptional() {
        return Optional.ofNullable(resolvedValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VesselRegistrySupplier<?> that = (VesselRegistrySupplier<?>) o;
        return Objects.equals(id, that.id) && Objects.equals(namespace, that.namespace);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, id);
    }

    @Override
    public String toString() {
        return "VesselRegistrySupplier[" + getNamespacedId() + "]";
    }
}
