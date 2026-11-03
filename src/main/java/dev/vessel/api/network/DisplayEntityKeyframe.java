package dev.vessel.api.network;

/**
 * 3D transform keyframe representing an animation update or motion state.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner or nested classes.</p>
 *
 * @param entityId           target entity identifier
 * @param posX               X coordinate position
 * @param posY               Y coordinate position
 * @param posZ               Z coordinate position
 * @param yaw                horizontal rotation in degrees
 * @param pitch              vertical pitch rotation in degrees
 * @param roll               roll rotation in degrees
 * @param scaleX             scaling factor on X axis
 * @param scaleY             scaling factor on Y axis
 * @param scaleZ             scaling factor on Z axis
 * @param interpolationTicks duration of interpolation transition in game ticks
 * @param timestamp          system timestamp in milliseconds
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
public record DisplayEntityKeyframe(
        int entityId,
        double posX,
        double posY,
        double posZ,
        float yaw,
        float pitch,
        float roll,
        float scaleX,
        float scaleY,
        float scaleZ,
        int interpolationTicks,
        long timestamp
) {

    public static final float DEFAULT_SCALE = 1.0f;
    public static final int DEFAULT_INTERPOLATION_TICKS = 1;

    /**
     * Convenience factory for position-only keyframe.
     *
     * @param entityId target entity ID
     * @param x        X coordinate
     * @param y        Y coordinate
     * @param z        Z coordinate
     * @return constructed keyframe
     */
    public static DisplayEntityKeyframe ofPosition(int entityId, double x, double y, double z) {
        return new DisplayEntityKeyframe(
                entityId,
                x, y, z,
                0.0f, 0.0f, 0.0f,
                DEFAULT_SCALE, DEFAULT_SCALE, DEFAULT_SCALE,
                DEFAULT_INTERPOLATION_TICKS,
                System.currentTimeMillis()
        );
    }
}
