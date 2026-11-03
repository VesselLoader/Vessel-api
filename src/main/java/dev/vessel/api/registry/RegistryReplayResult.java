package dev.vessel.api.registry;

/**
 * Result metrics from executing a deferred registry replay phase.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner or nested classes.</p>
 *
 * @param totalEntriesReplayed total deferred entries evaluated
 * @param successCount         number of entries successfully mounted
 * @param failureCount         number of entries that failed resolution
 * @param durationNanos        execution duration in nanoseconds
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
public record RegistryReplayResult(
        int totalEntriesReplayed,
        int successCount,
        int failureCount,
        long durationNanos
) {
    /**
     * Checks if all deferred entries resolved cleanly with zero failures.
     *
     * @return true if 100% successful
     */
    public boolean isFullySuccessful() {
        return failureCount == 0 && successCount == totalEntriesReplayed;
    }
}
