package dev.vessel.api.classification;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Detects Tier 2 (Mixed-Vessel Mod) occurrences where artifact bytecode references
 * both native Vessel Super-API and one or more third-party loader APIs.
 */
public final class MixedVesselReferenceDetector {

    private static final String VESSEL_API_PREFIX = "dev.vessel.api.";

    private static final Map<String, String> LOADER_PREFIX_MAP = Map.of(
        "net.minecraftforge.", "FORGE",
        "net.neoforged.", "NEOFORGE",
        "net.fabricmc.", "FABRIC",
        "org.quiltmc.", "QUILT",
        "org.purpurmc.", "PURPUR",
        "io.papermc.", "PAPER",
        "com.destroystokyo.paper.", "PAPER",
        "org.bukkit.", "BUKKIT",
        "org.spigotmc.", "BUKKIT"
    );

    public MixedVesselReferenceDetector() {
    }

    /**
     * Determines whether the given references constitute a Mixed-Vessel mod (Tier 2).
     *
     * @param referencedClasses collection of referenced class names
     * @return true if artifact references both Vessel Super-API and external loader APIs
     */
    public boolean isMixedVessel(Set<String> referencedClasses) {
        Objects.requireNonNull(referencedClasses, "referencedClasses cannot be null");
        boolean hasVessel = hasVesselApiReference(referencedClasses);
        boolean hasExternal = !detectExternalLoaders(referencedClasses).isEmpty();
        return hasVessel && hasExternal;
    }

    /**
     * Checks if the artifact bytecode references native Vessel Super-API.
     *
     * @param referencedClasses collection of referenced class names
     * @return true if Vessel Super-API classes are referenced
     */
    public boolean hasVesselApiReference(Set<String> referencedClasses) {
        Objects.requireNonNull(referencedClasses, "referencedClasses cannot be null");
        for (String ref : referencedClasses) {
            if (ref == null) {
                continue;
            }
            String normalized = ref.replace('/', '.');
            if (normalized.startsWith(VESSEL_API_PREFIX)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Detects external loader ecosystems present in the referenced classes.
     *
     * @param referencedClasses collection of referenced class names
     * @return set of loader identifiers detected (e.g. FORGE, FABRIC, BUKKIT)
     */
    public Set<String> detectExternalLoaders(Set<String> referencedClasses) {
        Objects.requireNonNull(referencedClasses, "referencedClasses cannot be null");
        if (referencedClasses.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> detected = new LinkedHashSet<>();
        for (String ref : referencedClasses) {
            if (ref == null) {
                continue;
            }
            String normalized = ref.replace('/', '.');
            for (Map.Entry<String, String> entry : LOADER_PREFIX_MAP.entrySet()) {
                if (normalized.startsWith(entry.getKey())) {
                    detected.add(entry.getValue());
                }
            }
        }
        return Collections.unmodifiableSet(detected);
    }
}
