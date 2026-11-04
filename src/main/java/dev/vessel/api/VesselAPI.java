package dev.vessel.api;

import dev.vessel.api.block.VesselBlockBuilder;
import dev.vessel.api.capability.VesselCapabilityLookup;
import dev.vessel.api.event.DynamicEventSubscriptionRouter;
import dev.vessel.api.event.SovereignEventBus;
import dev.vessel.api.item.VesselItemBuilder;
import dev.vessel.api.network.VesselPayloadChannel;
import dev.vessel.api.registry.VesselDeferredRegister;
import dev.vessel.api.registry.VesselRegistryKey;
import dev.vessel.api.registry.VesselUniversalRegistry;

import java.util.Objects;

/**
 * Central developer discovery facade and IDE auto-completion anchor for the VESSEL Super-API.
 *
 * <h2>Quickstart Developer Guide:</h2>
 * <pre>{@code
 * // 1. Create Deferred Registers (Forge-style static declaration, Fabric-light execution)
 * public static final VesselDeferredRegister<VesselBlock> BLOCKS = VesselAPI.createDeferredRegister(VesselRegistryKey.BLOCK, "mymod");
 * public static final VesselDeferredRegister<VesselItem> ITEMS = VesselAPI.createDeferredRegister(VesselRegistryKey.ITEM, "mymod");
 *
 * // 2. Build Content fluently with full IDE auto-completion
 * public static final VesselRegistrySupplier<VesselBlock> QUANTUM_GENERATOR = BLOCKS.register("quantum_generator", () ->
 *     VesselAPI.createBlock("mymod:quantum_generator")
 *         .hardness(5.0f)
 *         .resistance(12.0f)
 *         .luminance(15)
 *         .sound("metal")
 *         .energyStorage(5000000L)
 *         .build()
 * );
 *
 * public static final VesselRegistrySupplier<VesselItem> PLASMA_BLADE = ITEMS.register("plasma_blade", () ->
 *     VesselAPI.createItem("mymod:plasma_blade")
 *         .durability(1500)
 *         .rarity("EPIC")
 *         .fireResistant(true)
 *         .energyCapacity(1000000L)
 *         .onRightClick((player, world, hand) -> true)
 *         .build()
 * );
 *
 * // 3. Network Channels for Type-Safe Custom Packets
 * VesselPayloadChannel channel = VesselAPI.createNetworkChannel("mymod:main");
 *
 * // 4. Subscribe to Events
 * VesselAPI.registerEvents(myEventListenerInstance);
 * }</pre>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.1.0
 * @since 1.0.0
 */
public final class VesselAPI {

    private VesselAPI() {
    }

    /**
     * Creates a new deferred register for lazy static registration.
     *
     * @param <T>         target entry type
     * @param registryKey registry key (e.g. {@link VesselRegistryKey#BLOCK}, {@link VesselRegistryKey#ITEM})
     * @param namespace   mod namespace
     * @return deferred register instance
     */
    public static <T> VesselDeferredRegister<T> createDeferredRegister(VesselRegistryKey<T> registryKey, String namespace) {
        return VesselDeferredRegister.create(registryKey, namespace);
    }

    /**
     * Initializes a fluent builder for constructing a custom {@link dev.vessel.api.block.VesselBlock}.
     *
     * @param id canonical namespaced block id (e.g. {@code "mymod:quantum_generator"})
     * @return fluent block builder
     */
    public static VesselBlockBuilder createBlock(String id) {
        return VesselBlockBuilder.create(id);
    }

    /**
     * Initializes a fluent builder for constructing a custom {@link dev.vessel.api.item.VesselItem}.
     *
     * @param id canonical namespaced item id (e.g. {@code "mymod:plasma_blade"})
     * @return fluent item builder
     */
    public static VesselItemBuilder createItem(String id) {
        return VesselItemBuilder.create(id);
    }

    /**
     * Resolves or creates a type-safe network channel for custom packet payload communication.
     *
     * @param channelId namespaced channel id (e.g. {@code "mymod:main"})
     * @return network payload channel
     */
    public static VesselPayloadChannel createNetworkChannel(String channelId) {
        return VesselPayloadChannel.create(channelId);
    }

    /**
     * Registers an event listener object containing {@link dev.vessel.api.event.VesselSubscribe} methods.
     *
     * @param listener listener target object
     */
    public static void registerEvents(Object listener) {
        Objects.requireNonNull(listener, "listener cannot be null");
        DynamicEventSubscriptionRouter.getInstance().registerSyntheticObject(listener);
    }

    /**
     * Dispatches an event through the native ultra-fast event bus.
     *
     * @param event event instance
     * @return true if event was cancelled
     */
    public static boolean dispatchEvent(Object event) {
        return DynamicEventSubscriptionRouter.getInstance().dispatchDynamicEvent(event);
    }

    /**
     * Resolves the global native sovereign event bus.
     *
     * @return event bus
     */
    public static SovereignEventBus eventBus() {
        return SovereignEventBus.getInstance();
    }

    /**
     * Resolves the global pre-flight universal registry hub.
     *
     * @return registry hub
     */
    public static VesselUniversalRegistry registry() {
        return VesselUniversalRegistry.getInstance();
    }
}
