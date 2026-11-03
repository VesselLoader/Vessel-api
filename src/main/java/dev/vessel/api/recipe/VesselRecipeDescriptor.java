// ==============================================================================
// VESSEL-API — VesselRecipeDescriptor.java
// Version: 1.0.0
// Changelog:
// - 1.0.0: Initial creation of immutable VesselRecipeDescriptor record
//          describing cross-loader crafting and smelting recipes in VUSA.
// ==============================================================================

package dev.vessel.api.recipe;

import java.util.List;
import java.util.Objects;

/**
 * Immutable descriptor of a recipe registered across Vanilla {@code RecipeManager},
 * NeoForge/Fabric data-driven recipe registries, and Bukkit/Paper {@code Server#addRecipe}
 * bridges within the VESSEL Universal Super-API (VUSA).
 *
 * @param recipeKey         namespaced recipe identifier (must not be {@code null} or blank)
 * @param recipeType        recipe category such as {@code SHAPED}, {@code SHAPELESS}, {@code SMELTING},
 *                          or {@code FURNACE} (must not be {@code null} or blank)
 * @param resultMaterialKey namespaced material/item identifier of the crafted output (must not be {@code null} or blank)
 * @param resultAmount      output stack count (must be {@code > 0})
 * @param ingredientKeys    immutable list of ingredient item/tag keys
 * @version 1.0.0
 */
public record VesselRecipeDescriptor(
        String recipeKey,
        String recipeType,
        String resultMaterialKey,
        int resultAmount,
        List<String> ingredientKeys) {

    /**
     * Validates all components of {@link VesselRecipeDescriptor} and defensively copies {@code ingredientKeys}.
     *
     * @throws NullPointerException     if {@code recipeKey}, {@code recipeType}, {@code resultMaterialKey},
     *                                  or any element in {@code ingredientKeys} is {@code null}
     * @throws IllegalArgumentException if any string identifier is blank or {@code resultAmount <= 0}
     */
    public VesselRecipeDescriptor {
        Objects.requireNonNull(recipeKey, "recipeKey must not be null");
        Objects.requireNonNull(recipeType, "recipeType must not be null");
        Objects.requireNonNull(resultMaterialKey, "resultMaterialKey must not be null");
        if (recipeKey.isBlank()) {
            throw new IllegalArgumentException("recipeKey must not be blank");
        }
        if (recipeType.isBlank()) {
            throw new IllegalArgumentException("recipeType must not be blank");
        }
        if (resultMaterialKey.isBlank()) {
            throw new IllegalArgumentException("resultMaterialKey must not be blank");
        }
        if (resultAmount <= 0) {
            throw new IllegalArgumentException("resultAmount must be positive");
        }
        ingredientKeys = ingredientKeys == null ? List.of() : List.copyOf(ingredientKeys);
    }

    /**
     * Checks whether this recipe is a shaped crafting recipe.
     *
     * @return {@code true} if {@code "SHAPED".equalsIgnoreCase(recipeType)}
     */
    public boolean isShaped() {
        return "SHAPED".equalsIgnoreCase(this.recipeType);
    }

    /**
     * Checks whether this recipe is a shapeless crafting recipe.
     *
     * @return {@code true} if {@code "SHAPELESS".equalsIgnoreCase(recipeType)}
     */
    public boolean isShapeless() {
        return "SHAPELESS".equalsIgnoreCase(this.recipeType);
    }

    /**
     * Checks whether this recipe is a smelting/furnace recipe.
     *
     * @return {@code true} if {@code recipeType} matches {@code SMELTING} or {@code FURNACE} (case-insensitive)
     */
    public boolean isSmelting() {
        return "SMELTING".equalsIgnoreCase(this.recipeType) || "FURNACE".equalsIgnoreCase(this.recipeType);
    }
}
