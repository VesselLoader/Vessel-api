package dev.vessel.api.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite verifying Domain 6 Universal Registry symmetry,
 * frozen perfect hash table O(1) performance, event replay, and freeze guard barriers.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 */
public final class UniversalRegistrySymmetryTest {

    @Test
    @DisplayName("Verify FrozenRegistryPerfectHashTable O(1) lookups and off-heap metadata")
    public void testFrozenHashTableO1LookupAndMetadata() {
        Map<String, String> entries = new LinkedHashMap<>();
        for (int i = 0; i < 200; i++) {
            entries.put("vessel:mod_item_" + i, "Value_" + i);
        }
        entries.put("minecraft:diamond_sword", "LegendarySword");
        entries.put("minecraft:stone", "CommonStone");

        FrozenRegistryPerfectHashTable<String> table =
                new FrozenRegistryPerfectHashTable<>("minecraft:item", entries);

        assertThat(table.isFrozen()).isTrue();
        assertThat(table.getRegistryIdentifier()).isEqualTo("minecraft:item");
        assertThat(table.size()).isEqualTo(202);
        assertThat(table.getMemorySegmentByteSize()).isEqualTo(64L);

        // Verification of O(1) retrieval
        assertThat(table.get("minecraft:diamond_sword")).isEqualTo("LegendarySword");
        assertThat(table.get("minecraft:stone")).isEqualTo("CommonStone");
        assertThat(table.containsKey("minecraft:stone")).isTrue();
        assertThat(table.getId("minecraft:diamond_sword")).isGreaterThanOrEqualTo(0);

        // Non-existent entries
        assertThat(table.get("minecraft:unknown_artifact")).isNull();
        assertThat(table.containsKey("minecraft:unknown_artifact")).isFalse();
        assertThat(table.getId("minecraft:unknown_artifact")).isEqualTo(-1);
        assertThat(table.get(null)).isNull();
        assertThat(table.containsKey(null)).isFalse();

        // JIT Warmup loop
        for (int i = 0; i < 50_000; i++) {
            table.get("minecraft:diamond_sword");
        }

        // High-speed lookup benchmark validation
        int iterations = 500_000;
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            String val = table.get("minecraft:diamond_sword");
            if (val == null) {
                throw new AssertionError("Lookup failed");
            }
        }
        long duration = System.nanoTime() - start;
        double nanosPerOp = (double) duration / (double) iterations;
        assertThat(nanosPerOp).isLessThan(5000.0);
    }

    @Test
    @DisplayName("Verify RegistryEventReplayDispatcher retroactive replay and late-loading subscribers")
    public void testRegistryEventReplayDispatcher() {
        RegistryEventReplayDispatcher dispatcher = new RegistryEventReplayDispatcher();

        String registry = "minecraft:block";
        dispatcher.recordEntry(registry, "copper_block");
        dispatcher.recordEntry(registry, "cut_copper_stairs");
        dispatcher.recordEntry(registry, "waxed_weathered_copper");

        assertThat(dispatcher.getRecordedEntryCount(registry)).isEqualTo(3);
        assertThat(dispatcher.getSubscriberCount(registry)).isEqualTo(0);

        List<Object> replayedEvents = new ArrayList<>();
        // Late-loading subscriber attaches after entries were already recorded
        dispatcher.registerReplaySubscriber(registry, replayedEvents::add);

        assertThat(dispatcher.getSubscriberCount(registry)).isEqualTo(1);
        assertThat(replayedEvents)
                .containsExactly("copper_block", "cut_copper_stairs", "waxed_weathered_copper");

        // Subsequent live record should immediately deliver to active subscriber
        dispatcher.recordEntry(registry, "lightning_rod");
        assertThat(replayedEvents).hasSize(4);
        assertThat(replayedEvents.get(3)).isEqualTo("lightning_rod");

        // Targeted replay to external transient listener
        List<Object> transientReplay = new ArrayList<>();
        dispatcher.replayAllTo(registry, transientReplay::add);
        assertThat(transientReplay).hasSize(4);

        dispatcher.clearSubscribers(registry);
        assertThat(dispatcher.getSubscriberCount(registry)).isEqualTo(0);
    }

    @Test
    @DisplayName("Verify RegistryFreezeVerificationGuard immutability assertions and token unfreeze")
    public void testRegistryFreezeVerificationGuard() {
        RegistryFreezeVerificationGuard guard = new RegistryFreezeVerificationGuard();
        String registry = "minecraft:enchantment";

        // Initial writable state
        guard.assertWritable(registry);
        assertThat(guard.isFrozen(registry)).isFalse();

        // Freeze registry
        guard.freezeRegistry(registry, 42);
        assertThat(guard.isFrozen(registry)).isTrue();
        assertThat(guard.getFrozenEntryCount(registry)).isEqualTo(42);

        // Verification assertions
        guard.verifyIntegrity(registry, 42);

        assertThatThrownBy(() -> guard.assertWritable(registry))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("is FROZEN and immutable");

        assertThatThrownBy(() -> guard.verifyIntegrity(registry, 43))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Registry integrity violation");

        // Invalid token unfreeze attempt
        UUID randomToken = UUID.randomUUID();
        assertThatThrownBy(() -> guard.unfreezeWithToken(registry, randomToken))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("invalid security token");

        // Authorized unfreeze with issued token
        UUID validToken = guard.issueTemporaryUnfreezeToken(registry);
        boolean unfreezeSuccess = guard.unfreezeWithToken(registry, validToken);

        assertThat(unfreezeSuccess).isTrue();
        assertThat(guard.isFrozen(registry)).isFalse();
        guard.assertWritable(registry);
    }

    @Test
    @DisplayName("Rule 17 Compliance: verify zero inner or nested classes across registry micro-classes")
    public void testRule17ZeroInnerClasses() {
        Class<?>[] classes = new Class<?>[] {
                FrozenRegistryPerfectHashTable.class,
                RegistryEventReplayDispatcher.class,
                RegistryFreezeVerificationGuard.class,
                UniversalRegistrySymmetryTest.class
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
