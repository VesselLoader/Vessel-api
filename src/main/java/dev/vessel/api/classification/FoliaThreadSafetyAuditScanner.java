package dev.vessel.api.classification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Audits bytecode invocations against the Folia regional concurrency thread-safety gate.
 * Plugins declaring 'folia-supported: true' must exhibit zero calls to legacy synchronous
 * Bukkit methods; any violation triggers automatic demotion to Paper/Bukkit and routing
 * through GlobalRegionSyncBarrier.
 */
public final class FoliaThreadSafetyAuditScanner {

    private static final Set<String> UNSAFE_METHOD_SIGNATURES = Set.of(
        "org.bukkit.Bukkit.getWorld",
        "org.bukkit.Bukkit.getWorlds",
        "org.bukkit.Bukkit.getOnlinePlayers",
        "org.bukkit.Server.broadcastMessage",
        "org.bukkit.entity.Entity.teleport",
        "org.bukkit.block.Block.setType",
        "org.bukkit.World.getBlockAt",
        "org.bukkit.Bukkit.dispatchCommand"
    );

    public FoliaThreadSafetyAuditScanner() {
    }

    /**
     * Determines whether the plugin passes the Folia thread safety audit gate.
     *
     * @param foliaSupportedInDescriptor whether descriptor declares 'folia-supported: true'
     * @param invokedMethods collection of invoked method identifiers (e.g. owner.name)
     * @return true if plugin declared folia support and has zero unsafe invocations
     */
    public boolean isThreadSafeForFolia(boolean foliaSupportedInDescriptor, Collection<String> invokedMethods) {
        if (!foliaSupportedInDescriptor) {
            return false;
        }
        return !hasUnsafeCalls(invokedMethods);
    }

    /**
     * Checks if any invoked method matches known non-thread-safe legacy Bukkit operations.
     *
     * @param invokedMethods collection of invoked method identifiers
     * @return true if at least one unsafe call is present
     */
    public boolean hasUnsafeCalls(Collection<String> invokedMethods) {
        Objects.requireNonNull(invokedMethods, "invokedMethods cannot be null");
        return !findUnsafeInvocations(invokedMethods).isEmpty();
    }

    /**
     * Finds and collects all unsafe method invocations found in the bytecode.
     *
     * @param invokedMethods collection of invoked method identifiers
     * @return unmodifiable list of unsafe invocations
     */
    public List<String> findUnsafeInvocations(Collection<String> invokedMethods) {
        Objects.requireNonNull(invokedMethods, "invokedMethods cannot be null");
        if (invokedMethods.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> violations = new ArrayList<>();
        for (String methodRef : invokedMethods) {
            if (methodRef == null) {
                continue;
            }
            String normalized = methodRef.replace('/', '.');
            for (String unsafeSignature : UNSAFE_METHOD_SIGNATURES) {
                if (normalized.startsWith(unsafeSignature)) {
                    violations.add(normalized);
                    break;
                }
            }
        }
        return Collections.unmodifiableList(violations);
    }

    /**
     * Resolves the fallback target loader when an artifact is demoted from Folia.
     *
     * @param hasPaperReferences whether the artifact references Paper-specific APIs
     * @return "PAPER" if paper symbols are present, otherwise "BUKKIT"
     */
    public String resolveDemotedLoader(boolean hasPaperReferences) {
        return hasPaperReferences ? "PAPER" : "BUKKIT";
    }
}
