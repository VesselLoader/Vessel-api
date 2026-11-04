package dev.vessel.api.block;

import java.util.Objects;

/**
 * Universal custom block representation for the VESSEL Super-API ecosystem.
 *
 * <p>Provides cross-loader block behavior abstraction, enabling modders to deploy custom
 * functional blocks with energy capabilities, custom drops, and dynamic light levels
 * across Fabric, Forge, Paper, and Bedrock clients without NMS dependencies.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.1.0
 * @since 1.0.0
 */
public class VesselBlock {

    private final VesselBlockProperties properties;

    /**
     * Constructs a custom block with specified properties.
     *
     * @param properties block properties specification
     */
    public VesselBlock(VesselBlockProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties cannot be null");
    }

    /**
     * Resolves the canonical namespaced identifier of this block.
     *
     * @return namespaced id
     */
    public String getId() {
        return properties.id();
    }

    /**
     * Resolves block physical and gameplay properties.
     *
     * @return properties record
     */
    public VesselBlockProperties getProperties() {
        return properties;
    }

    /**
     * Resolves block hardness.
     *
     * @return hardness
     */
    public float getHardness() {
        return properties.hardness();
    }

    /**
     * Resolves blast resistance.
     *
     * @return resistance
     */
    public float getResistance() {
        return properties.resistance();
    }

    /**
     * Resolves emitted light level (0 to 15).
     *
     * @return luminance
     */
    public int getLuminance() {
        return properties.luminance();
    }

    /**
     * Checks whether this block is capable of holding energy.
     *
     * @return true if energy capacity > 0
     */
    public boolean hasEnergyStorage() {
        return properties.energyCapacity() > 0L;
    }

    @Override
    public String toString() {
        return "VesselBlock[" + properties.id() + "]";
    }
}
