package dev.vessel.api.block;

import dev.vessel.api.item.VesselItem;
import dev.vessel.api.item.VesselItemBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verification test suite for {@link VesselBlockBuilder} and {@link VesselItemBuilder}.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 */
public final class VesselBlockAndItemBuilderTest {

    @Test
    @DisplayName("Verify VesselBlockBuilder constructs proper block properties")
    void verifyBlockBuilderProperties() {
        VesselBlock block = VesselBlockBuilder.create("mymod:quantum_generator")
                .hardness(5.0f)
                .resistance(15.0f)
                .luminance(14)
                .sound("metal")
                .requiresTool(true)
                .energyStorage(5_000_000L)
                .drops("mymod:quantum_core", 1, 3)
                .build();

        assertThat(block.getId()).isEqualTo("mymod:quantum_generator");
        assertThat(block.getHardness()).isEqualTo(5.0f);
        assertThat(block.getResistance()).isEqualTo(15.0f);
        assertThat(block.getLuminance()).isEqualTo(14);
        assertThat(block.hasEnergyStorage()).isTrue();
        assertThat(block.getProperties().soundGroup()).isEqualTo("metal");
        assertThat(block.getProperties().dropItemId()).isEqualTo("mymod:quantum_core");
        assertThat(block.getProperties().dropMaxCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("Verify VesselItemBuilder constructs proper item properties and interaction handler")
    void verifyItemBuilderProperties() {
        VesselItem item = VesselItemBuilder.create("mymod:plasma_blade")
                .durability(1500)
                .rarity("EPIC")
                .fireResistant(true)
                .energyCapacity(1_000_000L)
                .onRightClick((player, world, hand) -> true)
                .build();

        assertThat(item.getId()).isEqualTo("mymod:plasma_blade");
        assertThat(item.getDurability()).isEqualTo(1500);
        assertThat(item.getMaxStackSize()).isEqualTo(1); // durability automatically sets stack size to 1
        assertThat(item.getRarity()).isEqualTo("EPIC");
        assertThat(item.isFireResistant()).isTrue();
        assertThat(item.hasEnergyStorage()).isTrue();
        assertThat(item.use("Player1", "World", "MAIN_HAND")).isTrue();
    }
}
