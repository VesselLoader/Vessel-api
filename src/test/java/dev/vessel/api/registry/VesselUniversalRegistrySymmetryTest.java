package dev.vessel.api.registry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Enterprise test suite verifying Domain 6 Universal Registry & Deferred Registration Symmetry
 * across Forge, NeoForge, and Fabric loaders.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner or nested classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
public class VesselUniversalRegistrySymmetryTest {

    private VesselUniversalRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new VesselUniversalRegistry();
    }

    @Test
    @DisplayName("VesselUniversalRegistry: Validates initialization, standard tables, lifecycle phases and freeze barrier")
    void testVesselUniversalRegistryStandardInitializationAndPhases() {
        assertThat(registry.currentPhase()).isEqualTo(RegistryLifecyclePhase.PRE_FLIGHT);
        assertThat(registry.getAllRegistries()).contains(
                VesselUniversalRegistry.REGISTRY_ITEM,
                VesselUniversalRegistry.REGISTRY_BLOCK,
                VesselUniversalRegistry.REGISTRY_ENTITY_TYPE
        );

        // Register valid entry
        registry.registerEntry(VesselUniversalRegistry.REGISTRY_ITEM, "vessel:wrench", "ItemInstance_Wrench", VesselUniversalRegistry.LOADER_UNIVERSAL);
        assertThat(registry.hasEntry(VesselUniversalRegistry.REGISTRY_ITEM, "vessel:wrench")).isTrue();

        // Freeze registry
        registry.freezeRegistry();
        assertThat(registry.currentPhase()).isEqualTo(RegistryLifecyclePhase.FROZEN);

        // Mutating frozen registry throws IllegalStateException
        assertThatThrownBy(() -> registry.registerEntry(VesselUniversalRegistry.REGISTRY_ITEM, "vessel:late_item", "LateItem", VesselUniversalRegistry.LOADER_UNIVERSAL))
                .isInstanceOf(IllegalStateException.class);

        // Unfreeze for reload
        registry.unfreezeForReload();
        assertThat(registry.currentPhase()).isEqualTo(RegistryLifecyclePhase.DYNAMIC_RELOAD);
        registry.registerEntry(VesselUniversalRegistry.REGISTRY_ITEM, "vessel:late_item", "LateItem", VesselUniversalRegistry.LOADER_UNIVERSAL);
        assertThat(registry.hasEntry(VesselUniversalRegistry.REGISTRY_ITEM, "vessel:late_item")).isTrue();
    }

    @Test
    @DisplayName("VesselUniversalRegistry: Cross-loader static ASM entrypoints register Forge, NeoForge, and Fabric entries")
    void testCrossLoaderEntryRegistrationAndQueries() {
        VesselUniversalRegistry global = VesselUniversalRegistry.getInstance();
        global.clearAll();

        // 1. Legacy Forge
        VesselUniversalRegistry.registerLegacyForgeEntry("legacy_forge_sword");
        assertThat(global.hasEntry(VesselUniversalRegistry.REGISTRY_ITEM, "legacy_forge_sword")).isTrue();

        // 2. Modern Forge DeferredRegister
        Object deferredRef = "minecraft:item";
        VesselUniversalRegistry.registerForgeDeferred(deferredRef, "create:cogwheel", () -> "CogwheelItem");
        assertThat(global.hasEntry(VesselUniversalRegistry.REGISTRY_ITEM, "create:cogwheel")).isTrue();
        assertThat(global.getEntry(VesselUniversalRegistry.REGISTRY_ITEM, "create:cogwheel")).contains("CogwheelItem");

        // 3. Fabric Register
        Object fabricResult = VesselUniversalRegistry.registerFabricEntry("minecraft:block", "tech:generator", "GeneratorBlock");
        assertThat(fabricResult).isEqualTo("GeneratorBlock");
        assertThat(global.hasEntry("minecraft:block", "tech:generator")).isTrue();

        // 4. NeoForge RegisterEvent
        VesselUniversalRegistry.registerNeoForgeEntry("eventRef", "minecraft:entity_type", "neoforge:custom_boss", () -> "BossEntity");
        assertThat(global.hasEntry("minecraft:entity_type", "neoforge:custom_boss")).isTrue();

        // Holder metadata inspection
        Optional<RegistryEntryHolder> holderOpt = global.getEntryHolder("minecraft:block", "tech:generator");
        assertThat(holderOpt).isPresent();
        RegistryEntryHolder holder = holderOpt.get();
        assertThat(holder.loaderKind()).isEqualTo(VesselUniversalRegistry.LOADER_FABRIC);
        assertThat(holder.asType(String.class)).isEqualTo("GeneratorBlock");

        Map<String, RegistryEntryHolder> items = global.getEntriesForRegistry(VesselUniversalRegistry.REGISTRY_ITEM);
        assertThat(items).isNotEmpty();
    }

    @Test
    @DisplayName("VesselUniversalRegistry: Validates callback notification upon entry registration")
    void testRegistryCallbackNotification() {
        AtomicBoolean callbackFired = new AtomicBoolean(false);
        AtomicReference<String> registeredEntryKey = new AtomicReference<>();

        registry.registerCallback(VesselUniversalRegistry.REGISTRY_BLOCK, (regKey, entryKey, entry) -> {
            callbackFired.set(true);
            registeredEntryKey.set(entryKey);
        });

        registry.registerEntry(VesselUniversalRegistry.REGISTRY_BLOCK, "vessel:sovereign_core", "SovereignCoreBlock", VesselUniversalRegistry.LOADER_UNIVERSAL);
        assertThat(callbackFired.get()).isTrue();
        assertThat(registeredEntryKey.get()).isEqualTo("vessel:sovereign_core");
    }

    @Test
    @DisplayName("DeferredRegisterSymmetryReplay: Queues and deterministically replays deferred entries into universal registry")
    void testDeferredRegisterSymmetryReplayEngine() {
        DeferredRegisterSymmetryReplay replayCoordinator = new DeferredRegisterSymmetryReplay();

        replayCoordinator.enqueueDeferredEntry(
                VesselUniversalRegistry.REGISTRY_ITEM,
                "create:flywheel",
                () -> "FlywheelItemInstance",
                VesselUniversalRegistry.LOADER_FORGE
        );
        replayCoordinator.enqueueDeferredEntry(
                VesselUniversalRegistry.REGISTRY_BLOCK,
                "create:gearbox",
                () -> "GearboxBlockInstance",
                VesselUniversalRegistry.LOADER_FORGE
        );
        replayCoordinator.enqueueDeferredEntry(
                VesselUniversalRegistry.REGISTRY_ITEM,
                "botania:livingwood",
                () -> "LivingwoodItemInstance",
                VesselUniversalRegistry.LOADER_NEOFORGE
        );

        assertThat(replayCoordinator.totalQueuedEntries()).isEqualTo(3);

        // Replay all
        RegistryReplayResult result = replayCoordinator.replayAll(registry);
        assertThat(result.totalEntriesReplayed()).isEqualTo(3);
        assertThat(result.successCount()).isEqualTo(3);
        assertThat(result.failureCount()).isEqualTo(0);
        assertThat(result.isFullySuccessful()).isTrue();

        // Verify entries in registry
        assertThat(registry.hasEntry(VesselUniversalRegistry.REGISTRY_ITEM, "create:flywheel")).isTrue();
        assertThat(registry.hasEntry(VesselUniversalRegistry.REGISTRY_BLOCK, "create:gearbox")).isTrue();
        assertThat(registry.hasEntry(VesselUniversalRegistry.REGISTRY_ITEM, "botania:livingwood")).isTrue();

        // Test single registry replay
        DeferredRegisterSymmetryReplay partialReplay = new DeferredRegisterSymmetryReplay();
        partialReplay.enqueueDeferredEntry(
                VesselUniversalRegistry.REGISTRY_ENTITY_TYPE,
                "custom:robot",
                () -> "RobotEntityType",
                VesselUniversalRegistry.LOADER_NEOFORGE
        );

        RegistryReplayResult partialResult = partialReplay.replayForRegistry(VesselUniversalRegistry.REGISTRY_ENTITY_TYPE, registry);
        assertThat(partialResult.successCount()).isEqualTo(1);
        assertThat(registry.hasEntry(VesselUniversalRegistry.REGISTRY_ENTITY_TYPE, "custom:robot")).isTrue();
    }

    @Test
    @DisplayName("FabricRegistrySymmetryBridge: Intercepts Fabric Registry.register, increments rawId, and notifies listeners")
    void testFabricRegistrySymmetryBridgeOpcodeReceptor() {
        FabricRegistrySymmetryBridge bridge = new FabricRegistrySymmetryBridge(registry);

        AtomicInteger lastRawId = new AtomicInteger(-1);
        AtomicReference<String> lastEntryId = new AtomicReference<>();

        bridge.addEntryListener(VesselUniversalRegistry.REGISTRY_ITEM, (rawId, id, object) -> {
            lastRawId.set(rawId);
            lastEntryId.set(id);
        });

        // 1. First registration
        Object item1 = bridge.registerEntry(VesselUniversalRegistry.REGISTRY_ITEM, "fabric_mod:ruby", "RubyItem");
        assertThat(item1).isEqualTo("RubyItem");
        assertThat(lastRawId.get()).isEqualTo(1);
        assertThat(lastEntryId.get()).isEqualTo("fabric_mod:ruby");
        assertThat(registry.hasEntry(VesselUniversalRegistry.REGISTRY_ITEM, "fabric_mod:ruby")).isTrue();

        // 2. Second registration (increments raw ID)
        Object item2 = bridge.registerEntry(VesselUniversalRegistry.REGISTRY_ITEM, "fabric_mod:sapphire", "SapphireItem");
        assertThat(item2).isEqualTo("SapphireItem");
        assertThat(lastRawId.get()).isEqualTo(2);
        assertThat(lastEntryId.get()).isEqualTo("fabric_mod:sapphire");
        assertThat(registry.hasEntry(VesselUniversalRegistry.REGISTRY_ITEM, "fabric_mod:sapphire")).isTrue();

        // 3. Static helper
        String res = FabricRegistrySymmetryBridge.register("minecraft:block", "fabric_mod:ruby_block", "RubyBlock");
        assertThat(res).isEqualTo("RubyBlock");
    }
}
