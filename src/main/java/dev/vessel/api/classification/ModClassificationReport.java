package dev.vessel.api.classification;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable classification report representing the static analysis result
 * of an artifact container under Chapter 2 Mod Taxonomy specification.
 */
public record ModClassificationReport(
    String containerId,
    ModTaxonomyTier tier,
    String classifiedLoader,
    boolean foliaThreadSafe,
    boolean demotedFromFolia,
    Set<String> detectedExternalLoaders,
    List<String> auditViolations,
    String summary
) {

    public ModClassificationReport {
        Objects.requireNonNull(containerId, "containerId cannot be null");
        Objects.requireNonNull(tier, "tier cannot be null");
        Objects.requireNonNull(classifiedLoader, "classifiedLoader cannot be null");
        detectedExternalLoaders = Set.copyOf(detectedExternalLoaders != null ? detectedExternalLoaders : Set.of());
        auditViolations = List.copyOf(auditViolations != null ? auditViolations : List.of());
        Objects.requireNonNull(summary, "summary cannot be null");
    }

    public boolean isVesselNative() {
        return this.tier == ModTaxonomyTier.VESSEL;
    }

    public boolean isHybridMixed() {
        return this.tier == ModTaxonomyTier.MIXED_VESSEL;
    }

    public boolean isLegacySingleLoader() {
        return this.tier == ModTaxonomyTier.SINGLE_LOADER;
    }
}
