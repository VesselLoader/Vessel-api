package dev.vessel.api.datapack;

import java.util.Objects;

/**
 * Symmetry receptor and adapter converting Fabric {@code ResourceManagerHelper.registerReloadListener()}
 * invocations directly into Vessel's {@link ResourceReloadDispatcher} under Wave 3 &amp; 4.
 *
 * <p>Serves as the ASM opcode transmutation receptor target for Fabric resource-loader-v0/v1 APIs.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0
 */
public final class FabricResourceReloadSymmetryAdapter {

    public static final String FABRIC_RESOURCE_MANAGER_HELPER =
            "net/fabricmc/fabric/api/resource/ResourceManagerHelper";

    private static volatile ResourceReloadDispatcher globalDispatcher = new ResourceReloadDispatcher();

    private final ResourceReloadDispatcher localDispatcher;

    /**
     * Constructs a symmetry adapter backed by the global default reload dispatcher.
     */
    public FabricResourceReloadSymmetryAdapter() {
        this(globalDispatcher);
    }

    /**
     * Constructs a symmetry adapter backed by a specific reload dispatcher instance.
     *
     * @param dispatcher target reload dispatcher
     */
    public FabricResourceReloadSymmetryAdapter(ResourceReloadDispatcher dispatcher) {
        this.localDispatcher = Objects.requireNonNull(dispatcher, "dispatcher cannot be null");
    }

    /**
     * Registers a reload listener into this adapter's dispatcher.
     *
     * @param reloadListener Fabric IdentifiableResourceReloadListener or generic listener object
     */
    public void registerReloadListener(Object reloadListener) {
        Objects.requireNonNull(reloadListener, "reloadListener cannot be null");
        this.localDispatcher.registerReloadListener(reloadListener);
    }

    /**
     * Registers a reload listener filtered by Fabric ResourceType.
     *
     * @param resourceType   Fabric ResourceType (e.g. SERVER_DATA or CLIENT_RESOURCES)
     * @param reloadListener Fabric IdentifiableResourceReloadListener
     */
    public void registerReloadListener(Object resourceType, Object reloadListener) {
        // Vessel universal loader registers both server and client listeners dynamically
        registerReloadListener(reloadListener);
    }

    /**
     * Static ASM transmutation receptor for Fabric {@code registerReloadListener(listener)}.
     *
     * @param reloadListener reload listener instance
     */
    public static void registerReloadListenerStatic(Object reloadListener) {
        Objects.requireNonNull(reloadListener, "reloadListener cannot be null");
        globalDispatcher.registerReloadListener(reloadListener);
    }

    /**
     * Static ASM transmutation receptor for Fabric {@code registerReloadListener(resourceType, listener)}.
     *
     * @param resourceType   resource type enum or descriptor
     * @param reloadListener reload listener instance
     */
    public static void registerReloadListenerStatic(Object resourceType, Object reloadListener) {
        registerReloadListenerStatic(reloadListener);
    }

    /**
     * Returns the underlying reload dispatcher instance.
     *
     * @return active reload dispatcher
     */
    public ResourceReloadDispatcher getUnderlyingDispatcher() {
        return this.localDispatcher;
    }

    /**
     * Retrieves the global default reload dispatcher.
     *
     * @return global dispatcher instance
     */
    public static ResourceReloadDispatcher getGlobalDispatcher() {
        return globalDispatcher;
    }

    /**
     * Sets the global default reload dispatcher.
     *
     * @param dispatcher new global dispatcher instance
     */
    public static void setGlobalDispatcher(ResourceReloadDispatcher dispatcher) {
        globalDispatcher = Objects.requireNonNull(dispatcher, "dispatcher cannot be null");
    }
}
