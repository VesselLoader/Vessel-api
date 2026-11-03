// ==============================================================================
// VESSEL-API — VesselWaveSevenApiContractsTest.java
// Version: 1.0.0
// Changelog:
// - 1.0.0: Initial creation of JUnit 5 test suite verifying Wave 7 VUSA contracts
//          VesselRecipeDescriptor and VesselResourceReloadSnapshot.
// ==============================================================================

package dev.vessel.api.recipe;

import dev.vessel.api.resource.VesselResourceReloadSnapshot;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for Wave 7 VUSA recipe and resource reload contracts
 * ({@link VesselRecipeDescriptor} and {@link VesselResourceReloadSnapshot}).
 *
 * @version 1.0.0
 */
final class VesselWaveSevenApiContractsTest {

    @Test
    void testRecipeDescriptorClassificationsAndImmutability() {
        List<String> mutableIngredients = new ArrayList<>();
        mutableIngredients.add("minecraft:iron_ingot");
        mutableIngredients.add("minecraft:redstone");

        VesselRecipeDescriptor shaped = new VesselRecipeDescriptor(
                "vessel:alloy_gear",
                "SHAPED",
                "vessel:alloy_gear_item",
                2,
                mutableIngredients);

        mutableIngredients.add("minecraft:coal");

        assertEquals("vessel:alloy_gear", shaped.recipeKey());
        assertEquals("SHAPED", shaped.recipeType());
        assertEquals("vessel:alloy_gear_item", shaped.resultMaterialKey());
        assertEquals(2, shaped.resultAmount());
        assertEquals(2, shaped.ingredientKeys().size());
        assertTrue(shaped.isShaped());
        assertFalse(shaped.isShapeless());
        assertFalse(shaped.isSmelting());

        assertThrows(
                UnsupportedOperationException.class,
                () -> shaped.ingredientKeys().add("minecraft:diamond"));

        VesselRecipeDescriptor shapeless = new VesselRecipeDescriptor(
                "vessel:bronze_blend",
                "shapeless",
                "vessel:bronze_dust",
                4,
                null);
        assertFalse(shapeless.isShaped());
        assertTrue(shapeless.isShapeless());
        assertFalse(shapeless.isSmelting());
        assertTrue(shapeless.ingredientKeys().isEmpty());

        VesselRecipeDescriptor smelting = new VesselRecipeDescriptor(
                "vessel:smelt_titanium",
                "SMELTING",
                "vessel:titanium_ingot",
                1,
                List.of("vessel:raw_titanium"));
        assertFalse(smelting.isShaped());
        assertFalse(smelting.isShapeless());
        assertTrue(smelting.isSmelting());

        VesselRecipeDescriptor furnace = new VesselRecipeDescriptor(
                "vessel:furnace_titanium",
                "furnace",
                "vessel:titanium_ingot",
                1,
                List.of("vessel:raw_titanium"));
        assertTrue(furnace.isSmelting());
    }

    @Test
    void testRecipeDescriptorValidation() {
        assertThrows(
                NullPointerException.class,
                () -> new VesselRecipeDescriptor(null, "SHAPED", "minecraft:stone", 1, List.of()));
        assertThrows(
                NullPointerException.class,
                () -> new VesselRecipeDescriptor("vessel:r", null, "minecraft:stone", 1, List.of()));
        assertThrows(
                NullPointerException.class,
                () -> new VesselRecipeDescriptor("vessel:r", "SHAPED", null, 1, List.of()));

        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselRecipeDescriptor("   ", "SHAPED", "minecraft:stone", 1, List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselRecipeDescriptor("vessel:r", "  ", "minecraft:stone", 1, List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselRecipeDescriptor("vessel:r", "SHAPED", "", 1, List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselRecipeDescriptor("vessel:r", "SHAPED", "minecraft:stone", 0, List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselRecipeDescriptor("vessel:r", "SHAPED", "minecraft:stone", -2, List.of()));
    }

    @Test
    void testResourceReloadSnapshotPropertiesAndValidation() {
        List<String> mutableListeners = new ArrayList<>();
        mutableListeners.add("vessel:recipe_sync");
        mutableListeners.add("vessel:tag_bridge");

        VesselResourceReloadSnapshot snapshot = new VesselResourceReloadSnapshot(
                "SERVER_DATA",
                mutableListeners,
                14250L);

        mutableListeners.add("vessel:extra");

        assertEquals("SERVER_DATA", snapshot.resourceType());
        assertEquals(2, snapshot.listenerCount());
        assertEquals(14250L, snapshot.durationMicros());

        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.listenerIds().add("vessel:mutated"));

        VesselResourceReloadSnapshot emptyListeners = new VesselResourceReloadSnapshot(
                "CLIENT_RESOURCES",
                null,
                0L);
        assertEquals(0, emptyListeners.listenerCount());

        assertThrows(
                NullPointerException.class,
                () -> new VesselResourceReloadSnapshot(null, List.of(), 100L));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselResourceReloadSnapshot("   ", List.of(), 100L));
        assertThrows(
                IllegalArgumentException.class,
                () -> new VesselResourceReloadSnapshot("SERVER_DATA", List.of(), -1L));
    }
}
