package dev.vessel.api.transmutation;

import java.util.Objects;
import org.objectweb.asm.Opcodes;

/**
 * Symmetry adapter converting Quilt Standard Libraries (QSL) Event and Loader contracts
 * into native Vessel Super-API counterparts under VUSA Chapter 3 and Chapter 6.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0
 */
public final class QuiltToVesselSymmetryAdapter {

    public static final String QUILT_PACKAGE_PREFIX = "org/quiltmc/";
    public static final String QUILT_EVENT_CLASS = "org/quiltmc/qsl/base/api/event/Event";
    public static final String QUILT_LOADER_CLASS = "org/quiltmc/loader/api/QuiltLoader";
    public static final String QUILT_MOD_CONTAINER = "org/quiltmc/loader/api/ModContainer";

    public static final String VESSEL_EVENT_BUS = "dev/vessel/adapter/forge/SovereignEventBusShim";
    public static final String VESSEL_LOADER_SHIM = "dev/vessel/adapter/fabric/FabricLoaderShim";

    /**
     * Constructs a new Quilt-to-Vessel symmetry adapter.
     */
    public QuiltToVesselSymmetryAdapter() {
    }

    /**
     * Configures the opcode transmutator with all Quilt-to-Vessel symmetry replacement rules.
     *
     * @param transmutator destination transmutator
     */
    public void configureTransmutations(CrossLoaderOpcodeTransmutator transmutator) {
        Objects.requireNonNull(transmutator, "transmutator cannot be null");

        // Type mappings
        transmutator.registerTypeReplacement(QUILT_EVENT_CLASS, VESSEL_EVENT_BUS);
        transmutator.registerTypeReplacement(QUILT_LOADER_CLASS, VESSEL_LOADER_SHIM);

        // Event method mappings
        transmutator.registerMethodReplacement(
                QUILT_EVENT_CLASS,
                "register",
                "(Ljava/lang/Object;)V",
                VESSEL_EVENT_BUS,
                "registerListenerObject",
                "(Ljava/lang/Object;)V",
                Opcodes.INVOKEVIRTUAL
        );

        // Loader method mappings
        transmutator.registerMethodReplacement(
                QUILT_LOADER_CLASS,
                "isModLoaded",
                "(Ljava/lang/String;)Z",
                VESSEL_LOADER_SHIM,
                "isModLoaded",
                "(Ljava/lang/String;)Z",
                Opcodes.INVOKESTATIC
        );
    }

    /**
     * Inspects whether a given class or descriptor references the Quilt loader ecosystem.
     *
     * @param internalName internal class name or descriptor
     * @return true if Quilt specific
     */
    public boolean isQuiltClass(String internalName) {
        if (internalName == null) {
            return false;
        }
        String normalized = internalName.replace('.', '/');
        return normalized.startsWith(QUILT_PACKAGE_PREFIX);
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
     * Returns the target Vessel loader shim class.
     *
     * @return target loader internal name
     */
    public String getTargetLoaderClass() {
        return VESSEL_LOADER_SHIM;
    }
}
