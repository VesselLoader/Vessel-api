package dev.vessel.api.classification;

import java.util.Objects;

/**
 * Formal Mod Taxonomy Classification Standard Tiers (Chapter 2.1).
 * Categorizes loaded artifacts into three architectural tiers to govern
 * execution shims, hardware acceleration, and concurrency models.
 */
public enum ModTaxonomyTier {

    /**
     * Tier 1: Pure Vessel Mod.
     * Written 100% exclusively in Vessel Super-API (dev.vessel.api.*) and/or
     * official Vanilla Minecraft Mojang mappings (net.minecraft.*).
     * Bypasses all legacy compatibility shims and runs directly on native microkernel.
     */
    VESSEL(1, "VESSEL", "Tier 1: Pure Vessel Mod (Native Super-API)"),

    /**
     * Tier 2: Mixed-Vessel Mod.
     * References native Vessel Super-API while simultaneously invoking
     * external loader APIs (Forge, NeoForge, Fabric, Quilt, Paper, Bukkit).
     */
    MIXED_VESSEL(2, "MIXED_VESSEL", "Tier 2: Mixed-Vessel Hybrid Mod"),

    /**
     * Tier 3: Single-Loader Mod / Plugin.
     * Contains zero references to dev.vessel.api.* and relies strictly
     * on an upstream loader API (Bukkit, Paper, Purpur, Folia, Fabric, Quilt, Forge, NeoForge).
     */
    SINGLE_LOADER(3, "SINGLE_LOADER", "Tier 3: Upstream Single-Loader Mod or Plugin");

    private final int tierLevel;
    private final String identifier;
    private final String description;

    ModTaxonomyTier(int tierLevel, String identifier, String description) {
        this.tierLevel = tierLevel;
        this.identifier = Objects.requireNonNull(identifier, "identifier cannot be null");
        this.description = Objects.requireNonNull(description, "description cannot be null");
    }

    public int tierLevel() {
        return this.tierLevel;
    }

    public String identifier() {
        return this.identifier;
    }

    public String description() {
        return this.description;
    }

    public boolean isPureVessel() {
        return this == VESSEL;
    }

    public boolean isMixedVessel() {
        return this == MIXED_VESSEL;
    }

    public boolean isSingleLoader() {
        return this == SINGLE_LOADER;
    }
}
