package dev.vessel.api.config;

import java.util.Objects;

/**
 * Immutable token entry representing a parsed path-value pair within a flat configuration table.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner or nested classes.</p>
 *
 * @param path        canonical dot-separated path (e.g., "database.connection.timeout")
 * @param rawValue    raw string token before type resolution
 * @param typedValue  resolved typed Java object (Integer, Boolean, Double, String, or List)
 * @param comment     associated header or inline comment
 * @param lineNumber  source file line number
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
public record ConfigTokenEntry(
        String path,
        String rawValue,
        Object typedValue,
        String comment,
        int lineNumber
) {
    public ConfigTokenEntry {
        Objects.requireNonNull(path, "path cannot be null");
    }

    /**
     * Checks if this token possesses an associated comment.
     *
     * @return true if comment is present and not blank
     */
    public boolean hasComment() {
        return comment != null && !comment.isBlank();
    }
}
