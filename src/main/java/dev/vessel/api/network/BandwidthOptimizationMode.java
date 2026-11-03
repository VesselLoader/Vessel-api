package dev.vessel.api.network;

/**
 * Strategy mode for packet bandwidth optimization.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner or nested classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
public enum BandwidthOptimizationMode {

    /**
     * Payload sent without compression or coalescing.
     */
    PASSTHROUGH("PASSTHROUGH"),

    /**
     * Multiple packets batched into a contiguous buffer frame.
     */
    COALESCE_BATCH("COALESCE_BATCH"),

    /**
     * State delta diff compression against a reference baseline.
     */
    DELTA_COMPRESS("DELTA_COMPRESS"),

    /**
     * Full stream payload compression using standard Deflate algorithm.
     */
    FULL_COMPRESS("FULL_COMPRESS");

    private final String modeName;

    BandwidthOptimizationMode(String modeName) {
        this.modeName = modeName;
    }

    /**
     * Resolves the mode name.
     *
     * @return mode name string
     */
    public String modeName() {
        return modeName;
    }
}
