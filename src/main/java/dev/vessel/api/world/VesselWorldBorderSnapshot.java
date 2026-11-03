// ==============================================================================
// VESSEL-API — VesselWorldBorderSnapshot.java
// Version: 1.0.0
// Changelog:
// - 1.0.0: Initial creation of immutable VesselWorldBorderSnapshot record
//          capturing world border geometry, damage, and warning parameters.
// ==============================================================================

package dev.vessel.api.world;

import java.util.Objects;

/**
 * Immutable point-in-time snapshot of a dimension's world border state bridged
 * across Vanilla {@code WorldBorder} and Bukkit/Paper {@code org.bukkit.WorldBorder}
 * within the VESSEL Universal Super-API (VUSA).
 *
 * @param worldName       canonical world or dimension identifier (must not be {@code null} or blank)
 * @param centerX         world border center X coordinate
 * @param centerZ         world border center Z coordinate
 * @param size            side length of the square world border in blocks (must be {@code >= 1.0D})
 * @param damageAmount    damage dealt per block per second outside the border buffer (must be {@code >= 0.0D})
 * @param warningDistance warning overlay distance in blocks (must be {@code >= 0})
 * @version 1.0.0
 */
public record VesselWorldBorderSnapshot(
        String worldName,
        double centerX,
        double centerZ,
        double size,
        double damageAmount,
        int warningDistance) {

    /**
     * Validates all components of {@link VesselWorldBorderSnapshot}.
     *
     * @throws NullPointerException     if {@code worldName} is {@code null}
     * @throws IllegalArgumentException if {@code worldName} is blank, {@code size < 1.0D},
     *                                  {@code damageAmount < 0.0D}, or {@code warningDistance < 0}
     */
    public VesselWorldBorderSnapshot {
        Objects.requireNonNull(worldName, "worldName must not be null");
        if (worldName.isBlank()) {
            throw new IllegalArgumentException("worldName must not be blank");
        }
        if (Double.isNaN(size) || size < 1.0D) {
            throw new IllegalArgumentException("size must be at least 1.0");
        }
        if (Double.isNaN(damageAmount) || damageAmount < 0.0D) {
            throw new IllegalArgumentException("damageAmount must not be negative");
        }
        if (warningDistance < 0) {
            throw new IllegalArgumentException("warningDistance must not be negative");
        }
    }

    /**
     * Determines whether the specified {@code (x, z)} block coordinate lies within
     * or on the boundary of this world border.
     *
     * @param x world X coordinate
     * @param z world Z coordinate
     * @return {@code true} if {@code Math.abs(x - centerX) <= size / 2.0D && Math.abs(z - centerZ) <= size / 2.0D}
     */
    public boolean isInside(double x, double z) {
        return Math.abs(x - this.centerX) <= this.size / 2.0D
                && Math.abs(z - this.centerZ) <= this.size / 2.0D;
    }
}
