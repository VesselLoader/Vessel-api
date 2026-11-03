// ==============================================================================
// VESSEL-API — VesselMixinTargetDescriptor.java
// Version: 1.0.0
// Changelog:
// - 1.0.0: Initial creation of immutable VesselMixinTargetDescriptor contract
//          describing a Sponge/Fabric/NeoForge Mixin class and its targets.
// ==============================================================================

package dev.vessel.api.mixin;

import java.util.List;
import java.util.Objects;

/**
 * Immutable descriptor of a Mixin class registered within the VESSEL cross-loader
 * Mixinweaver and bytecode symmetry pipeline.
 *
 * <p>Captures the Mixin class name, owning mod identifier, immutable list of target
 * class names, application priority, and obfuscation remapping flag.</p>
 *
 * @version 1.0.0
 */
public final class VesselMixinTargetDescriptor {

    private final String mixinClassName;
    private final String ownerModId;
    private final List<String> targetClassNames;
    private final int priority;
    private final boolean remap;

    /**
     * Constructs a new {@link VesselMixinTargetDescriptor}.
     *
     * @param mixinClassName   fully qualified or binary name of the Mixin class (must not be {@code null} or blank)
     * @param ownerModId       identifier of the owning mod/plugin (must not be {@code null} or blank)
     * @param targetClassNames non-empty list of target class names (must not be {@code null}, empty, or contain blanks)
     * @param priority         Mixin application priority (e.g., {@code 1000})
     * @param remap            {@code true} if obfuscation remapping is enabled for this Mixin
     * @throws NullPointerException     if any reference argument or list element is {@code null}
     * @throws IllegalArgumentException if any string is blank or {@code targetClassNames} is empty
     */
    public VesselMixinTargetDescriptor(
            String mixinClassName,
            String ownerModId,
            List<String> targetClassNames,
            int priority,
            boolean remap) {
        Objects.requireNonNull(mixinClassName, "mixinClassName must not be null");
        Objects.requireNonNull(ownerModId, "ownerModId must not be null");
        Objects.requireNonNull(targetClassNames, "targetClassNames must not be null");
        if (mixinClassName.isBlank()) {
            throw new IllegalArgumentException("mixinClassName must not be blank");
        }
        if (ownerModId.isBlank()) {
            throw new IllegalArgumentException("ownerModId must not be blank");
        }
        List<String> copy = List.copyOf(targetClassNames);
        if (copy.isEmpty()) {
            throw new IllegalArgumentException("targetClassNames must not be empty");
        }
        for (String target : copy) {
            if (target.isBlank()) {
                throw new IllegalArgumentException("targetClassNames must not contain blank elements");
            }
        }
        this.mixinClassName = mixinClassName;
        this.ownerModId = ownerModId;
        this.targetClassNames = copy;
        this.priority = priority;
        this.remap = remap;
    }

    /**
     * Returns the Mixin class name.
     *
     * @return the Mixin class name
     */
    public String getMixinClassName() {
        return this.mixinClassName;
    }

    /**
     * Returns the owning mod or plugin identifier.
     *
     * @return the owner mod ID
     */
    public String getOwnerModId() {
        return this.ownerModId;
    }

    /**
     * Returns an unmodifiable list of target class names woven by this Mixin.
     *
     * @return immutable list of target class names
     */
    public List<String> getTargetClassNames() {
        return this.targetClassNames;
    }

    /**
     * Returns the Mixin application priority.
     *
     * @return priority value
     */
    public int getPriority() {
        return this.priority;
    }

    /**
     * Indicates whether symbol remapping is enabled for this Mixin.
     *
     * @return {@code true} if remapping is enabled, {@code false} otherwise
     */
    public boolean isRemap() {
        return this.remap;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof VesselMixinTargetDescriptor other)) {
            return false;
        }
        return this.priority == other.priority
                && this.remap == other.remap
                && this.mixinClassName.equals(other.mixinClassName)
                && this.ownerModId.equals(other.ownerModId)
                && this.targetClassNames.equals(other.targetClassNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                this.mixinClassName,
                this.ownerModId,
                this.targetClassNames,
                this.priority,
                this.remap);
    }

    @Override
    public String toString() {
        return "VesselMixinTargetDescriptor{"
                + "mixinClassName='" + this.mixinClassName + '\''
                + ", ownerModId='" + this.ownerModId + '\''
                + ", targetClassNames=" + this.targetClassNames
                + ", priority=" + this.priority
                + ", remap=" + this.remap
                + '}';
    }
}
