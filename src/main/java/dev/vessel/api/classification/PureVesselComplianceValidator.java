package dev.vessel.api.classification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Validates compliance with Tier 1 (Pure Vessel Mod) standards under Chapter 2.1.
 * An artifact is certified as Pure Vessel if it contains zero external loader imports
 * and relies exclusively on Vessel Super-API or official Vanilla Minecraft mappings.
 */
public final class PureVesselComplianceValidator {

    private static final Set<String> EXTERNAL_LOADER_PREFIXES = Set.of(
        "net.minecraftforge.",
        "net.neoforged.",
        "net.fabricmc.",
        "org.quiltmc.",
        "org.bukkit.",
        "org.spigotmc.",
        "io.papermc.",
        "com.destroystokyo.paper.",
        "org.purpurmc."
    );

    public PureVesselComplianceValidator() {
    }

    /**
     * Checks if the given set of referenced classes contains zero references
     * to external loader packages.
     *
     * @param referencedClasses collection of class names (internal or fully-qualified)
     * @return true if 100% compliant with Pure Vessel Tier 1
     */
    public boolean isCompliant(Set<String> referencedClasses) {
        Objects.requireNonNull(referencedClasses, "referencedClasses cannot be null");
        return findExternalLoaderViolations(referencedClasses).isEmpty();
    }

    /**
     * Identifies any external loader class references violating Pure Vessel compliance.
     *
     * @param referencedClasses collection of class names
     * @return list of violating class references
     */
    public List<String> findExternalLoaderViolations(Set<String> referencedClasses) {
        Objects.requireNonNull(referencedClasses, "referencedClasses cannot be null");
        if (referencedClasses.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> violations = new ArrayList<>();
        for (String ref : referencedClasses) {
            if (ref == null) {
                continue;
            }
            String normalized = ref.replace('/', '.');
            for (String prefix : EXTERNAL_LOADER_PREFIXES) {
                if (normalized.startsWith(prefix)) {
                    violations.add(normalized);
                    break;
                }
            }
        }
        return Collections.unmodifiableList(violations);
    }
}
