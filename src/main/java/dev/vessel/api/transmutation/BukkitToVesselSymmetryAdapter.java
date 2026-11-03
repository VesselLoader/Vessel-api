package dev.vessel.api.transmutation;

import java.util.Objects;
import org.objectweb.asm.Opcodes;

/**
 * Symmetry adapter converting Bukkit, Spigot, Paper, and Folia Event and Scheduler contracts
 * into native Vessel Super-API counterparts under VUSA Chapter 3 and Chapter 6.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0
 */
public final class BukkitToVesselSymmetryAdapter {

    public static final String BUKKIT_PACKAGE_PREFIX = "org/bukkit/";
    public static final String PAPER_PACKAGE_PREFIX = "io/papermc/paper/";
    public static final String BUKKIT_PLUGIN_MANAGER = "org/bukkit/plugin/PluginManager";
    public static final String BUKKIT_SCHEDULER = "org/bukkit/scheduler/BukkitScheduler";
    public static final String BUKKIT_EVENT_CLASS = "org/bukkit/event/Event";
    public static final String BUKKIT_LISTENER = "org/bukkit/event/Listener";
    public static final String BUKKIT_PDC = "org/bukkit/persistence/PersistentDataContainer";

    public static final String VESSEL_EVENT_BUS = "dev/vessel/adapter/forge/SovereignEventBusShim";
    public static final String VESSEL_SCHEDULER = "dev/vessel/adapter/bukkit/FoliaSchedulerShim";
    public static final String VESSEL_DATA_ATTACHMENT = "dev/vessel/adapter/bridge/UnifiedDataAttachmentBridge";

    /**
     * Constructs a new Bukkit-to-Vessel symmetry adapter.
     */
    public BukkitToVesselSymmetryAdapter() {
    }

    /**
     * Configures the opcode transmutator with all Bukkit/Paper/Folia symmetry replacement rules.
     *
     * @param transmutator destination transmutator
     */
    public void configureTransmutations(CrossLoaderOpcodeTransmutator transmutator) {
        Objects.requireNonNull(transmutator, "transmutator cannot be null");

        // Type mappings
        transmutator.registerTypeReplacement(BUKKIT_PLUGIN_MANAGER, VESSEL_EVENT_BUS);
        transmutator.registerTypeReplacement(BUKKIT_SCHEDULER, VESSEL_SCHEDULER);
        transmutator.registerTypeReplacement(BUKKIT_PDC, VESSEL_DATA_ATTACHMENT);

        // Event dispatch method mappings
        transmutator.registerMethodReplacement(
                BUKKIT_PLUGIN_MANAGER,
                "callEvent",
                "(L" + BUKKIT_EVENT_CLASS + ";)V",
                VESSEL_EVENT_BUS,
                "post",
                "(Ljava/lang/Object;)Z",
                Opcodes.INVOKEVIRTUAL
        );

        transmutator.registerMethodReplacement(
                BUKKIT_PLUGIN_MANAGER,
                "registerEvents",
                "(L" + BUKKIT_LISTENER + ";Lorg/bukkit/plugin/Plugin;)V",
                VESSEL_EVENT_BUS,
                "registerListenerObject",
                "(Ljava/lang/Object;)V",
                Opcodes.INVOKEVIRTUAL
        );

        // Scheduler method mappings
        transmutator.registerMethodReplacement(
                BUKKIT_SCHEDULER,
                "runTask",
                "(Lorg/bukkit/plugin/Plugin;Ljava/lang/Runnable;)Lorg/bukkit/scheduler/BukkitTask;",
                VESSEL_SCHEDULER,
                "scheduleGlobalRegionTask",
                "(Ljava/lang/Runnable;)Z",
                Opcodes.INVOKEVIRTUAL
        );
    }

    /**
     * Inspects whether a given class or descriptor references the Bukkit or Paper ecosystem.
     *
     * @param internalName internal class name or descriptor
     * @return true if Bukkit or Paper specific
     */
    public boolean isBukkitClass(String internalName) {
        if (internalName == null) {
            return false;
        }
        String normalized = internalName.replace('.', '/');
        return normalized.startsWith(BUKKIT_PACKAGE_PREFIX) || normalized.startsWith(PAPER_PACKAGE_PREFIX);
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
     * Returns the target Vessel scheduler class.
     *
     * @return target scheduler internal name
     */
    public String getTargetSchedulerClass() {
        return VESSEL_SCHEDULER;
    }

    /**
     * Returns the target Vessel data attachment class.
     *
     * @return target data attachment internal name
     */
    public String getTargetDataAttachmentClass() {
        return VESSEL_DATA_ATTACHMENT;
    }
}
