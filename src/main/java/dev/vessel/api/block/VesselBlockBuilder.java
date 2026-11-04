package dev.vessel.api.block;

import java.util.Objects;

/**
 * Fluent builder for creating high-performance {@link VesselBlock} instances.
 *
 * <p>Inspired by Fabric's {@code FabricBlockSettings} and NeoForge's {@code BlockBehaviour.Properties},
 * offering a clean, lightweight, fluent developer experience.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.1.0
 * @since 1.0.0
 */
public final class VesselBlockBuilder {

    private final String id;
    private float hardness;
    private float resistance;
    private int luminance;
    private String soundGroup;
    private boolean requiresTool;
    private int tickRate;
    private String dropItemId;
    private int dropMinCount;
    private int dropMaxCount;
    private long energyCapacity;

    private VesselBlockBuilder(String id) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.hardness = 1.5f;
        this.resistance = 6.0f;
        this.luminance = 0;
        this.soundGroup = "stone";
        this.requiresTool = false;
        this.tickRate = 0;
        this.dropItemId = id;
        this.dropMinCount = 1;
        this.dropMaxCount = 1;
        this.energyCapacity = 0L;
    }

    /**
     * Initializes a builder for a given namespaced block id.
     *
     * @param id namespaced id (e.g. {@code "mymod:quantum_generator"})
     * @return builder
     */
    public static VesselBlockBuilder create(String id) {
        return new VesselBlockBuilder(id);
    }

    public VesselBlockBuilder hardness(float hardness) {
        this.hardness = hardness;
        return this;
    }

    public VesselBlockBuilder resistance(float resistance) {
        this.resistance = resistance;
        return this;
    }

    public VesselBlockBuilder luminance(int luminance) {
        this.luminance = Math.clamp(luminance, 0, 15);
        return this;
    }

    public VesselBlockBuilder sound(String soundGroup) {
        this.soundGroup = Objects.requireNonNull(soundGroup, "soundGroup cannot be null");
        return this;
    }

    public VesselBlockBuilder requiresTool(boolean requiresTool) {
        this.requiresTool = requiresTool;
        return this;
    }

    public VesselBlockBuilder tickRate(int tickRate) {
        this.tickRate = tickRate;
        return this;
    }

    public VesselBlockBuilder drops(String dropItemId, int min, int max) {
        this.dropItemId = Objects.requireNonNull(dropItemId, "dropItemId cannot be null");
        this.dropMinCount = min;
        this.dropMaxCount = max;
        return this;
    }

    public VesselBlockBuilder energyStorage(long capacity) {
        this.energyCapacity = Math.max(0L, capacity);
        return this;
    }

    /**
     * Builds and returns the configured {@link VesselBlock}.
     *
     * @return constructed block
     */
    public VesselBlock build() {
        VesselBlockProperties props = new VesselBlockProperties(
                id,
                hardness,
                resistance,
                luminance,
                soundGroup,
                requiresTool,
                tickRate,
                dropItemId,
                dropMinCount,
                dropMaxCount,
                energyCapacity
        );
        return new VesselBlock(props);
    }
}
