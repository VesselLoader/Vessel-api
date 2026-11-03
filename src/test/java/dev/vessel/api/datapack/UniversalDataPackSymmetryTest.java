package dev.vessel.api.datapack;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite verifying Universal DataPack, Loot, Screen Handler &amp; Reload Substrate
 * under Wave 3 &amp; 4 specification.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 */
public final class UniversalDataPackSymmetryTest {

    @Test
    @DisplayName("Verify VesselDataPackHub datapacks, tags, and recipe conditions")
    public void testVesselDataPackHub() {
        VesselDataPackHub hub = new VesselDataPackHub();

        // 1. Datapack lifecycle
        hub.registerDataPack("modid:virtual_pack", "VirtualPackContent");
        assertThat(hub.isDataPackActive("modid:virtual_pack")).isTrue();
        assertThat(hub.getDataPack("modid:virtual_pack")).isEqualTo("VirtualPackContent");
        assertThat(hub.getTotalDataPackCount()).isEqualTo(1);
        assertThat(hub.getActiveDataPackCount()).isEqualTo(1);

        hub.deactivateDataPack("modid:virtual_pack");
        assertThat(hub.isDataPackActive("modid:virtual_pack")).isFalse();
        assertThat(hub.getActiveDataPackCount()).isEqualTo(0);

        hub.activateDataPack("modid:virtual_pack");
        assertThat(hub.isDataPackActive("modid:virtual_pack")).isTrue();

        // 2. Tag registry
        List<String> oreTags = List.of("minecraft:iron_ore", "minecraft:deepslate_iron_ore");
        hub.registerTag("minecraft:block", "c:ores/iron", oreTags);
        assertThat(hub.hasTag("minecraft:block", "c:ores/iron")).isTrue();
        assertThat(hub.getTag("minecraft:block", "c:ores/iron")).isEqualTo(oreTags);
        assertThat(hub.hasTag("minecraft:block", "c:ores/copper")).isFalse();

        // 3. Recipe condition testing
        hub.registerRecipeCondition("forge:mod_loaded", ctx -> "create".equals(ctx));
        assertThat(hub.testRecipeCondition("forge:mod_loaded", "create")).isTrue();
        assertThat(hub.testRecipeCondition("forge:mod_loaded", "mekanism")).isFalse();
        assertThat(hub.testRecipeCondition("unknown:condition", "any")).isFalse();
    }

    @Test
    @DisplayName("Verify LootModifierPipeline fast-path in-place mutations without reallocations")
    public void testLootModifierPipeline() {
        LootModifierPipeline pipeline = new LootModifierPipeline();

        String tableId = "minecraft:chests/simple_dungeon";

        // Table-specific modifier
        pipeline.registerModifier(tableId, loot -> {
            if (loot instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<String> list = (List<String>) loot;
                list.add("modid:rare_gem");
            }
        });

        // Contextual modifier
        pipeline.registerContextualModifier(tableId, (loot, ctx) -> {
            if (loot instanceof List<?> && Integer.valueOf(3).equals(ctx)) {
                @SuppressWarnings("unchecked")
                List<String> list = (List<String>) loot;
                list.add("modid:fortune_bonus");
            }
        });

        // Global modifier
        pipeline.registerGlobalModifier(loot -> {
            if (loot instanceof List<?>) {
                @SuppressWarnings("unchecked")
                List<String> list = (List<String>) loot;
                list.add("modid:universal_shard");
            }
        });

        assertThat(pipeline.getModifierCount(tableId)).isEqualTo(2);
        assertThat(pipeline.getGlobalModifierCount()).isEqualTo(1);

        List<String> drops = new ArrayList<>();
        drops.add("minecraft:bone");
        drops.add("minecraft:saddle");

        Object result = pipeline.applyModifiers(tableId, drops, 3);
        assertThat(result).isSameAs(drops); // Guarantees zero list re-allocation

        assertThat(drops).containsExactly(
                "minecraft:bone",
                "minecraft:saddle",
                "modid:rare_gem",
                "modid:fortune_bonus",
                "modid:universal_shard"
        );
        assertThat(pipeline.getTotalModificationsApplied()).isEqualTo(3L);
    }

    @Test
    @DisplayName("Verify ResourceReloadDispatcher reload notifications and lifecycle metrics")
    public void testResourceReloadDispatcher() {
        ResourceReloadDispatcher dispatcher = new ResourceReloadDispatcher();

        AtomicBoolean callbackFired = new AtomicBoolean(false);
        AtomicInteger listenerCallCount = new AtomicInteger(0);

        dispatcher.registerReloadListener("test_callback", rm -> {
            callbackFired.set(true);
            assertThat(rm).isEqualTo("ActiveResourceManager");
        });

        dispatcher.registerReloadListener((Consumer<Object>) rm -> listenerCallCount.incrementAndGet());

        assertThat(dispatcher.getListenerCount()).isEqualTo(2);
        assertThat(dispatcher.getReloadCount()).isEqualTo(0);
        assertThat(dispatcher.getLastReloadTimestamp()).isEqualTo(0L);

        dispatcher.dispatchReload("ActiveResourceManager");

        assertThat(callbackFired.get()).isTrue();
        assertThat(listenerCallCount.get()).isEqualTo(1);
        assertThat(dispatcher.getReloadCount()).isEqualTo(1);
        assertThat(dispatcher.getLastReloadTimestamp()).isGreaterThan(0L);

        dispatcher.unregisterReloadListener("test_callback");
        assertThat(dispatcher.getListenerCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Verify ExtendedScreenHandlerRegistry custom GUI extra data synchronization")
    public void testExtendedScreenHandlerRegistry() {
        ExtendedScreenHandlerRegistry registry = new ExtendedScreenHandlerRegistry();

        String handlerId = "techmod:electric_furnace";
        Object dummyMenuType = new Object();
        registry.registerExtendedScreenHandler(handlerId, dummyMenuType);

        assertThat(registry.hasExtendedScreenHandler(handlerId)).isTrue();
        assertThat(registry.getExtendedScreenHandler(handlerId)).isSameAs(dummyMenuType);
        assertThat(registry.getRegisteredHandlerCount()).isEqualTo(1);

        AtomicBoolean syncCalled = new AtomicBoolean(false);
        registry.registerSyncConsumer(handlerId, (player, payload) -> {
            assertThat(player).isEqualTo("TestPlayer");
            assertThat(payload).isEqualTo("EnergyLevel=5000");
            syncCalled.set(true);
        });

        registry.syncExtraData(handlerId, "TestPlayer", "EnergyLevel=5000");
        assertThat(syncCalled.get()).isTrue();
    }

    @Test
    @DisplayName("Verify FabricResourceReloadSymmetryAdapter instance and static receptors")
    public void testFabricResourceReloadSymmetryAdapter() {
        ResourceReloadDispatcher customDispatcher = new ResourceReloadDispatcher();
        FabricResourceReloadSymmetryAdapter adapter = new FabricResourceReloadSymmetryAdapter(customDispatcher);

        AtomicInteger callCounter = new AtomicInteger(0);
        adapter.registerReloadListener((Consumer<Object>) rm -> callCounter.incrementAndGet());

        assertThat(customDispatcher.getListenerCount()).isEqualTo(1);
        customDispatcher.dispatchReload("ResourceManagerInstance");
        assertThat(callCounter.get()).isEqualTo(1);

        // Test static ASM receptor
        AtomicBoolean staticCallFired = new AtomicBoolean(false);
        FabricResourceReloadSymmetryAdapter.registerReloadListenerStatic(
                (Consumer<Object>) rm -> staticCallFired.set(true)
        );
        FabricResourceReloadSymmetryAdapter.getGlobalDispatcher().dispatchReload("StaticRM");
        assertThat(staticCallFired.get()).isTrue();
    }

    @Test
    @DisplayName("Rule 17 Compliance: verify zero inner or nested classes across datapack micro-classes")
    public void testRule17ZeroInnerClasses() {
        Class<?>[] classes = new Class<?>[] {
                VesselDataPackHub.class,
                LootModifierPipeline.class,
                ResourceReloadDispatcher.class,
                ExtendedScreenHandlerRegistry.class,
                FabricResourceReloadSymmetryAdapter.class,
                UniversalDataPackSymmetryTest.class
        };

        for (Class<?> clazz : classes) {
            assertThat(clazz.getDeclaredClasses())
                    .withFailMessage("Class %s must have 0 declared inner/nested classes per Rule 17", clazz.getName())
                    .isEmpty();
            assertThat(clazz.isAnonymousClass())
                    .withFailMessage("Class %s must not be anonymous", clazz.getName())
                    .isFalse();
        }
    }
}
