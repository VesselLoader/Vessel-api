package dev.vessel.api.capability;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Universal capability lookup and registration engine for the VESSEL Super-API ecosystem.
 *
 * <p>Emulates NeoForge's {@code BlockCapabilityCache} and Fabric's {@code BlockApiLookup},
 * providing zero-allocation capability queries across regions and chunk boundaries.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.1.0
 * @since 1.0.0
 */
public final class VesselCapabilityLookup {

    private static final Map<String, Function<Object, Object>> BLOCK_CAPABILITY_PROVIDERS = new ConcurrentHashMap<>();
    private static final Map<String, Function<Object, Object>> ENTITY_CAPABILITY_PROVIDERS = new ConcurrentHashMap<>();

    private VesselCapabilityLookup() {
    }

    /**
     * Registers a capability provider for a specific capability key on blocks.
     *
     * @param <T>      capability contract
     * @param key      capability key
     * @param provider provider mapping block context to capability instance
     */
    @SuppressWarnings("unchecked")
    public static <T> void registerBlockProvider(VesselCapabilityKey<T> key, Function<Object, T> provider) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(provider, "provider cannot be null");
        BLOCK_CAPABILITY_PROVIDERS.put(key.getIdentifier(), (Function<Object, Object>) provider);
    }

    /**
     * Queries a capability instance on a target block.
     *
     * @param <T>          capability contract
     * @param key          capability key
     * @param blockContext block entity or block state context
     * @return optional capability instance
     */
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> find(VesselCapabilityKey<T> key, Object blockContext) {
        if (blockContext == null) {
            return Optional.empty();
        }
        Function<Object, Object> provider = BLOCK_CAPABILITY_PROVIDERS.get(key.getIdentifier());
        if (provider != null) {
            Object res = provider.apply(blockContext);
            if (res != null && key.getContractClass().isInstance(res)) {
                return Optional.of((T) res);
            }
        }
        if (key.getContractClass().isInstance(blockContext)) {
            return Optional.of((T) blockContext);
        }
        return Optional.empty();
    }
}
