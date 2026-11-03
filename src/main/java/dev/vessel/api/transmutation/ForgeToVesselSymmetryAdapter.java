package dev.vessel.api.transmutation;

import java.util.Objects;
import org.objectweb.asm.Opcodes;

/**
 * Symmetry adapter converting Forge EventBus, DeferredRegister, and Capability contracts
 * into native Vessel Super-API counterparts under VUSA Chapter 3 and Chapter 6.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0
 */
public final class ForgeToVesselSymmetryAdapter {

    public static final String FORGE_PACKAGE_PREFIX = "net/minecraftforge/";
    public static final String FORGE_EVENT_BUS_INTERFACE = "net/minecraftforge/eventbus/api/IEventBus";
    public static final String FORGE_EVENT_BUS_CLASS = "net/minecraftforge/eventbus/EventBus";
    public static final String FORGE_EVENT_CLASS = "net/minecraftforge/eventbus/api/Event";
    public static final String FORGE_DEFERRED_REGISTER = "net/minecraftforge/registries/DeferredRegister";
    public static final String FORGE_CAPABILITY_MANAGER = "net/minecraftforge/common/capabilities/CapabilityManager";
    public static final String FORGE_ITEM_HANDLER = "net/minecraftforge/items/IItemHandler";

    public static final String VESSEL_EVENT_BUS = "dev/vessel/adapter/forge/SovereignEventBusShim";
    public static final String VESSEL_REGISTRY = "dev/vessel/adapter/forge/DeferredRegisterBridge";
    public static final String VESSEL_CAPABILITY = "dev/vessel/api/capability/VesselUnifiedCapability";
    public static final String VESSEL_INVENTORY = "dev/vessel/adapter/forge/UniversalInventoryBridge";

    /**
     * Constructs a new Forge-to-Vessel symmetry adapter.
     */
    public ForgeToVesselSymmetryAdapter() {
    }

    /**
     * Configures the opcode transmutator with all Forge-to-Vessel symmetry replacement rules.
     *
     * @param transmutator destination transmutator
     */
    public void configureTransmutations(CrossLoaderOpcodeTransmutator transmutator) {
        Objects.requireNonNull(transmutator, "transmutator cannot be null");

        // Type mappings
        transmutator.registerTypeReplacement(FORGE_EVENT_BUS_INTERFACE, VESSEL_EVENT_BUS);
        transmutator.registerTypeReplacement(FORGE_EVENT_BUS_CLASS, VESSEL_EVENT_BUS);
        transmutator.registerTypeReplacement(FORGE_DEFERRED_REGISTER, VESSEL_REGISTRY);
        transmutator.registerTypeReplacement(FORGE_CAPABILITY_MANAGER, VESSEL_CAPABILITY);
        transmutator.registerTypeReplacement(FORGE_ITEM_HANDLER, VESSEL_INVENTORY);

        // EventBus method mappings
        transmutator.registerMethodReplacement(
                FORGE_EVENT_BUS_INTERFACE,
                "register",
                "(Ljava/lang/Object;)V",
                VESSEL_EVENT_BUS,
                "registerListenerObject",
                "(Ljava/lang/Object;)V",
                Opcodes.INVOKEVIRTUAL
        );

        transmutator.registerMethodReplacement(
                FORGE_EVENT_BUS_INTERFACE,
                "post",
                "(L" + FORGE_EVENT_CLASS + ";)Z",
                VESSEL_EVENT_BUS,
                "post",
                "(Ljava/lang/Object;)Z",
                Opcodes.INVOKEVIRTUAL
        );

        // DeferredRegister method mappings
        transmutator.registerMethodReplacement(
                FORGE_DEFERRED_REGISTER,
                "create",
                "(Ljava/lang/String;Ljava/lang/String;)L" + FORGE_DEFERRED_REGISTER + ";",
                VESSEL_REGISTRY,
                "create",
                "(Ljava/lang/String;Ljava/lang/String;)L" + VESSEL_REGISTRY + ";",
                Opcodes.INVOKESTATIC
        );
    }

    /**
     * Inspects whether a given class or descriptor references the Forge loader ecosystem.
     *
     * @param internalName internal class name or descriptor
     * @return true if Forge specific
     */
    public boolean isForgeClass(String internalName) {
        if (internalName == null) {
            return false;
        }
        String normalized = internalName.replace('.', '/');
        return normalized.startsWith(FORGE_PACKAGE_PREFIX);
    }

    /**
     * Returns the target Vessel event bus class.
     *
     * @return target event bus internal name
     */
    public String getTargetEventBusClass() {
        return VESSEL_EVENT_BUS;
    }

    /**
     * Returns the target Vessel registry bridge class.
     *
     * @return target registry internal name
     */
    public String getTargetRegistryClass() {
        return VESSEL_REGISTRY;
    }

    /**
     * Returns the target Vessel capability class.
     *
     * @return target capability internal name
     */
    public String getTargetCapabilityClass() {
        return VESSEL_CAPABILITY;
    }
}
