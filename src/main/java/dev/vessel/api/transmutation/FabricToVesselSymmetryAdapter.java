package dev.vessel.api.transmutation;

import java.util.Objects;
import org.objectweb.asm.Opcodes;

/**
 * Symmetry adapter converting Fabric Event, Transfer Storage, and Networking contracts
 * into native Vessel Super-API counterparts under VUSA Chapter 3 and Chapter 6.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0
 */
public final class FabricToVesselSymmetryAdapter {

    public static final String FABRIC_PACKAGE_PREFIX = "net/fabricmc/";
    public static final String FABRIC_EVENT_CLASS = "net/fabricmc/fabric/api/event/Event";
    public static final String FABRIC_STORAGE_CLASS = "net/fabricmc/fabric/api/transfer/v1/storage/Storage";
    public static final String FABRIC_STORAGE_VIEW = "net/fabricmc/fabric/api/transfer/v1/storage/StorageView";
    public static final String FABRIC_TRANSACTION = "net/fabricmc/fabric/api/transfer/v1/transaction/TransactionContext";
    public static final String FABRIC_NETWORKING = "net/fabricmc/fabric/api/networking/v1/ServerPlayNetworking";

    public static final String VESSEL_EVENT_BUS = "dev/vessel/adapter/forge/SovereignEventBusShim";
    public static final String VESSEL_STORAGE = "dev/vessel/adapter/forge/UniversalInventoryBridge";
    public static final String VESSEL_TRANSACTION = "dev/vessel/api/capability/CapabilityTransactionSimulator";
    public static final String VESSEL_NETWORK_DISPATCHER = "dev/vessel/adapter/fabric/UniversalPacketChannelDispatcher";

    /**
     * Constructs a new Fabric-to-Vessel symmetry adapter.
     */
    public FabricToVesselSymmetryAdapter() {
    }

    /**
     * Configures the opcode transmutator with all Fabric-to-Vessel symmetry replacement rules.
     *
     * @param transmutator destination transmutator
     */
    public void configureTransmutations(CrossLoaderOpcodeTransmutator transmutator) {
        Objects.requireNonNull(transmutator, "transmutator cannot be null");

        // Type mappings
        transmutator.registerTypeReplacement(FABRIC_EVENT_CLASS, VESSEL_EVENT_BUS);
        transmutator.registerTypeReplacement(FABRIC_STORAGE_CLASS, VESSEL_STORAGE);
        transmutator.registerTypeReplacement(FABRIC_STORAGE_VIEW, VESSEL_STORAGE);
        transmutator.registerTypeReplacement(FABRIC_TRANSACTION, VESSEL_TRANSACTION);
        transmutator.registerTypeReplacement(FABRIC_NETWORKING, VESSEL_NETWORK_DISPATCHER);

        // Event method mappings
        transmutator.registerMethodReplacement(
                FABRIC_EVENT_CLASS,
                "register",
                "(Ljava/lang/Object;)V",
                VESSEL_EVENT_BUS,
                "registerListenerObject",
                "(Ljava/lang/Object;)V",
                Opcodes.INVOKEVIRTUAL
        );

        transmutator.registerMethodReplacement(
                FABRIC_EVENT_CLASS,
                "invoker",
                "()Ljava/lang/Object;",
                VESSEL_EVENT_BUS,
                "getInvoker",
                "()Ljava/lang/Object;",
                Opcodes.INVOKEVIRTUAL
        );

        // Transfer API method mappings
        transmutator.registerMethodReplacement(
                FABRIC_STORAGE_CLASS,
                "supportsExtraction",
                "()Z",
                VESSEL_STORAGE,
                "supportsExtraction",
                "()Z",
                Opcodes.INVOKEVIRTUAL
        );
    }

    /**
     * Inspects whether a given class or descriptor references the Fabric loader ecosystem.
     *
     * @param internalName internal class name or descriptor
     * @return true if Fabric specific
     */
    public boolean isFabricClass(String internalName) {
        if (internalName == null) {
            return false;
        }
        String normalized = internalName.replace('.', '/');
        return normalized.startsWith(FABRIC_PACKAGE_PREFIX);
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
     * Returns the target Vessel storage bridge class.
     *
     * @return target storage bridge internal name
     */
    public String getTargetStorageBridgeClass() {
        return VESSEL_STORAGE;
    }

    /**
     * Returns the target Vessel network dispatcher class.
     *
     * @return target network dispatcher internal name
     */
    public String getTargetNetworkDispatcherClass() {
        return VESSEL_NETWORK_DISPATCHER;
    }
}
