package dev.vessel.api.world;

/**
 * Zero-allocation packing utility condensing 3D block coordinates (x, y, z) and 2D chunk coordinates
 * into primitive 64-bit {@code long} values for ultra-fast spatial hashing and cache indexing.
 *
 * <p>Bit allocation: 26 bits for X, 12 bits for Y, 26 bits for Z (64 bits total),
 * preserving signed coordinate space from -33,554,432 to +33,554,431 in X/Z, and -2,048 to +2,047 in Y.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Pass K-33)
 */
public final class CoordinateFastPacker {

    /**
     * Bit allocation for X axis (26 bits).
     */
    public static final int X_BITS = 26;

    /**
     * Bit allocation for Z axis (26 bits).
     */
    public static final int Z_BITS = 26;

    /**
     * Bit allocation for Y axis (12 bits).
     */
    public static final int Y_BITS = 12;

    /**
     * 26-bit bitmask for X coordinates.
     */
    public static final long X_MASK = (1L << X_BITS) - 1L;

    /**
     * 12-bit bitmask for Y coordinates.
     */
    public static final long Y_MASK = (1L << Y_BITS) - 1L;

    /**
     * 26-bit bitmask for Z coordinates.
     */
    public static final long Z_MASK = (1L << Z_BITS) - 1L;

    /**
     * Bit shift offset for Y coordinate.
     */
    public static final int Y_SHIFT = 0;

    /**
     * Bit shift offset for Z coordinate.
     */
    public static final int Z_SHIFT = Y_BITS;

    /**
     * Bit shift offset for X coordinate.
     */
    public static final int X_SHIFT = Y_BITS + Z_BITS;

    /**
     * Unused private constructor for static utility class.
     */
    public CoordinateFastPacker() {
    }

    /**
     * Packs 3D block coordinates into a single 64-bit primitive long.
     *
     * @param x block X coordinate
     * @param y block Y coordinate
     * @param z block Z coordinate
     * @return 64-bit packed position key
     */
    public static long pack(int x, int y, int z) {
        return (((long) x & X_MASK) << X_SHIFT)
                | (((long) z & Z_MASK) << Z_SHIFT)
                | (((long) y & Y_MASK) << Y_SHIFT);
    }

    /**
     * Extracts the signed 32-bit X block coordinate from a packed key.
     *
     * @param packed 64-bit packed key
     * @return signed X coordinate
     */
    public static int unpackX(long packed) {
        return (int) (packed >> X_SHIFT);
    }

    /**
     * Extracts the signed 32-bit Y block coordinate from a packed key.
     *
     * @param packed 64-bit packed key
     * @return signed Y coordinate
     */
    public static int unpackY(long packed) {
        return (int) ((packed << (64 - Y_BITS)) >> (64 - Y_BITS));
    }

    /**
     * Extracts the signed 32-bit Z block coordinate from a packed key.
     *
     * @param packed 64-bit packed key
     * @return signed Z coordinate
     */
    public static int unpackZ(long packed) {
        return (int) ((packed << (64 - X_SHIFT)) >> (64 - Z_BITS));
    }

    /**
     * Packs 2D chunk coordinates (chunkX, chunkZ) into a 64-bit key.
     *
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @return 64-bit packed chunk key
     */
    public static long packChunk(int chunkX, int chunkZ) {
        return (((long) chunkX) << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    /**
     * Extracts signed 32-bit chunk X from a packed chunk key.
     *
     * @param packedChunk packed chunk key
     * @return chunk X
     */
    public static int unpackChunkX(long packedChunk) {
        return (int) (packedChunk >> 32);
    }

    /**
     * Extracts signed 32-bit chunk Z from a packed chunk key.
     *
     * @param packedChunk packed chunk key
     * @return chunk Z
     */
    public static int unpackChunkZ(long packedChunk) {
        return (int) packedChunk;
    }
}
