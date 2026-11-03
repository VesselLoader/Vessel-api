package dev.vessel.api.datapack;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Universal registry managing Fabric {@code ExtendedScreenHandlerType} contracts and extra data
 * packet synchronization for modded GUIs under Wave 3 &amp; 4.
 *
 * <p>Enables seamless synchronization of machine state, RF/FE energy levels, and fluid tank metrics
 * between server-side containers and client-side screens during {@code player.openHandledScreen()}.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0
 */
public final class ExtendedScreenHandlerRegistry {

    private final Map<String, Object> screenHandlers = new ConcurrentHashMap<>();
    private final Map<String, BiConsumer<Object, Object>> syncConsumers = new ConcurrentHashMap<>();

    /**
     * Constructs a new extended screen handler registry.
     */
    public ExtendedScreenHandlerRegistry() {
    }

    /**
     * Registers an extended screen handler type under a unique namespaced identifier.
     *
     * @param handlerId         namespaced screen handler identifier (e.g. "techmod:crusher_menu")
     * @param screenHandlerType Fabric ExtendedScreenHandlerType or MenuType instance
     */
    public void registerExtendedScreenHandler(String handlerId, Object screenHandlerType) {
        Objects.requireNonNull(handlerId, "handlerId cannot be null");
        Objects.requireNonNull(screenHandlerType, "screenHandlerType cannot be null");
        this.screenHandlers.put(handlerId, screenHandlerType);
    }

    /**
     * Retrieves the registered extended screen handler type.
     *
     * @param handlerId namespaced screen handler identifier
     * @return registered screen handler type, or null if absent
     */
    public Object getExtendedScreenHandler(String handlerId) {
        if (handlerId == null) {
            return null;
        }
        return this.screenHandlers.get(handlerId);
    }

    /**
     * Checks if an extended screen handler type is registered.
     *
     * @param handlerId namespaced screen handler identifier
     * @return true if registered
     */
    public boolean hasExtendedScreenHandler(String handlerId) {
        return getExtendedScreenHandler(handlerId) != null;
    }

    /**
     * Registers a custom payload synchronization callback for the specified screen handler.
     *
     * @param handlerId    namespaced screen handler identifier
     * @param syncConsumer bi-consumer receiving the player context and extra payload
     */
    public void registerSyncConsumer(String handlerId, BiConsumer<Object, Object> syncConsumer) {
        Objects.requireNonNull(handlerId, "handlerId cannot be null");
        Objects.requireNonNull(syncConsumer, "syncConsumer cannot be null");
        this.syncConsumers.put(handlerId, syncConsumer);
    }

    /**
     * Synchronizes extra container payload data to the client when opening an extended screen.
     *
     * @param handlerId    namespaced screen handler identifier
     * @param player       target player entity context
     * @param extraPayload packet payload or buffer containing initial machine state
     */
    public void syncExtraData(String handlerId, Object player, Object extraPayload) {
        Objects.requireNonNull(handlerId, "handlerId cannot be null");
        BiConsumer<Object, Object> consumer = this.syncConsumers.get(handlerId);
        if (consumer != null) {
            consumer.accept(player, extraPayload);
        }
    }

    /**
     * Returns total count of registered extended screen handlers.
     *
     * @return handler count
     */
    public int getRegisteredHandlerCount() {
        return this.screenHandlers.size();
    }

    /**
     * Clears all registered screen handler types and sync consumers.
     */
    public void clear() {
        this.screenHandlers.clear();
        this.syncConsumers.clear();
    }
}
