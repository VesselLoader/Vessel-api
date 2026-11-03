package dev.vessel.api.transmutation;

import java.util.Objects;
import org.objectweb.asm.Opcodes;

/**
 * Symmetry adapter converting NeoForge Attachment, EventBus, and Event contracts
 * into native Vessel Super-API counterparts under VUSA Chapter 3 and Chapter 6.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0
 */
public final class NeoForgeToVesselSymmetryAdapter {

    public static final String NEOFORGE_PACKAGE_PREFIX = "net/neoforged/";
    public static final String NEOFORGE_BUS_INTERFACE = "net/neoforged/bus/api/IEventBus";
    public static final String NEOFORGE_EVENT_CLASS = "net/neoforged/bus/api/Event";
    public static final String NEOFORGE_ATTACHMENT_HOLDER = "net/neoforged/neoforge/attachment/IAttachmentHolder";
    public static final String NEOFORGE_ATTACHMENT_TYPE = "net/neoforged/neoforge/attachment/AttachmentType";

    public static final String VESSEL_EVENT_BUS = "dev/vessel/adapter/forge/SovereignEventBusShim";
    public static final String VESSEL_DATA_ATTACHMENT = "dev/vessel/adapter/bridge/UnifiedDataAttachmentBridge";
    public static final String VESSEL_EVENT = "dev/vessel/kernel/event/VesselEvent";

    /**
     * Constructs a new NeoForge-to-Vessel symmetry adapter.
     */
    public NeoForgeToVesselSymmetryAdapter() {
    }

    /**
     * Configures the opcode transmutator with all NeoForge-to-Vessel symmetry replacement rules.
     *
     * @param transmutator destination transmutator
     */
    public void configureTransmutations(CrossLoaderOpcodeTransmutator transmutator) {
        Objects.requireNonNull(transmutator, "transmutator cannot be null");

        // Type mappings
        transmutator.registerTypeReplacement(NEOFORGE_BUS_INTERFACE, VESSEL_EVENT_BUS);
        transmutator.registerTypeReplacement(NEOFORGE_EVENT_CLASS, VESSEL_EVENT);
        transmutator.registerTypeReplacement(NEOFORGE_ATTACHMENT_HOLDER, VESSEL_DATA_ATTACHMENT);
        transmutator.registerTypeReplacement(NEOFORGE_ATTACHMENT_TYPE, VESSEL_DATA_ATTACHMENT);

        // EventBus method mappings
        transmutator.registerMethodReplacement(
                NEOFORGE_BUS_INTERFACE,
                "register",
                "(Ljava/lang/Object;)V",
                VESSEL_EVENT_BUS,
                "registerListenerObject",
                "(Ljava/lang/Object;)V",
                Opcodes.INVOKEVIRTUAL
        );

        transmutator.registerMethodReplacement(
                NEOFORGE_BUS_INTERFACE,
                "post",
                "(L" + NEOFORGE_EVENT_CLASS + ";)L" + NEOFORGE_EVENT_CLASS + ";",
                VESSEL_EVENT_BUS,
                "postEvent",
                "(Ljava/lang/Object;)Ljava/lang/Object;",
                Opcodes.INVOKEVIRTUAL
        );

        // Attachment method mappings
        transmutator.registerMethodReplacement(
                NEOFORGE_ATTACHMENT_HOLDER,
                "hasData",
                "(L" + NEOFORGE_ATTACHMENT_TYPE + ";)Z",
                VESSEL_DATA_ATTACHMENT,
                "hasAttachment",
                "(Ljava/lang/Object;)Z",
                Opcodes.INVOKEVIRTUAL
        );
    }

    /**
     * Inspects whether a given class or descriptor references the NeoForge loader ecosystem.
     *
     * @param internalName internal class name or descriptor
     * @return true if NeoForge specific
     */
    public boolean isNeoForgeClass(String internalName) {
        if (internalName == null) {
            return false;
        }
        String normalized = internalName.replace('.', '/');
        return normalized.startsWith(NEOFORGE_PACKAGE_PREFIX);
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
     * Returns the target Vessel attachment bridge class.
     *
     * @return target attachment internal name
     */
    public String getTargetAttachmentBridgeClass() {
        return VESSEL_DATA_ATTACHMENT;
    }
}
