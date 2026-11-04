package dev.vessel.api.registry;

import dev.vessel.api.block.VesselBlock;
import dev.vessel.api.item.VesselItem;

import java.util.Objects;

/**
 * Scoped immutable identifier key for a registry space within the VESSEL Super-API ecosystem.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @param <T> the base type of elements held in this registry
 * @version 1.1.1
 * @since 1.0.0
 */
public final class VesselRegistryKey<T> {

    public static final VesselRegistryKey<VesselBlock> BLOCK = new VesselRegistryKey<>(VesselUniversalRegistry.REGISTRY_BLOCK);
    public static final VesselRegistryKey<VesselItem> ITEM = new VesselRegistryKey<>(VesselUniversalRegistry.REGISTRY_ITEM);
    public static final VesselRegistryKey<Object> ENTITY_TYPE = new VesselRegistryKey<>(VesselUniversalRegistry.REGISTRY_ENTITY_TYPE);
    public static final VesselRegistryKey<Object> BIOME = new VesselRegistryKey<>(VesselUniversalRegistry.REGISTRY_BIOME);
    public static final VesselRegistryKey<Object> SOUND_EVENT = new VesselRegistryKey<>(VesselUniversalRegistry.REGISTRY_SOUND_EVENT);
    public static final VesselRegistryKey<Object> ENCHANTMENT = new VesselRegistryKey<>(VesselUniversalRegistry.REGISTRY_ENCHANTMENT);
    public static final VesselRegistryKey<Object> DATA_COMPONENT_TYPE = new VesselRegistryKey<>(VesselUniversalRegistry.REGISTRY_DATA_COMPONENT_TYPE);
    public static final VesselRegistryKey<Object> CUSTOM_STAT = new VesselRegistryKey<>(VesselUniversalRegistry.REGISTRY_CUSTOM_STAT);

    private final String location;

    /**
     * Constructs a scoped registry key.
     *
     * @param location canonical namespaced resource location
     */
    public VesselRegistryKey(String location) {
        this.location = Objects.requireNonNull(location, "location cannot be null");
    }

    /**
     * Creates a typed registry key.
     *
     * @param <E>      element type
     * @param location namespaced key identifier
     * @return typed key
     */
    public static <E> VesselRegistryKey<E> of(String location) {
        return new VesselRegistryKey<>(location);
    }

    /**
     * Resolves the canonical namespaced string location.
     *
     * @return registry location
     */
    public String getLocation() {
        return location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VesselRegistryKey<?> that = (VesselRegistryKey<?>) o;
        return Objects.equals(location, that.location);
    }

    @Override
    public int hashCode() {
        return location.hashCode();
    }

    @Override
    public String toString() {
        return "VesselRegistryKey[" + location + "]";
    }
}
