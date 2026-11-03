// ==============================================================================
// VESSEL-API — VesselInjectionPointDescriptor.java
// Version: 1.0.0
// Changelog:
// - 1.0.0: Initial creation of immutable VesselInjectionPointDescriptor contract
//          describing a Mixin @Inject/@Redirect injection point and its target.
// ==============================================================================

package dev.vessel.api.mixin;

import java.util.Objects;

/**
 * Immutable descriptor of a Mixin method injection point inspected or woven by
 * the VESSEL cross-loader Mixinweaver subsystem.
 *
 * @version 1.0.0
 */
public final class VesselInjectionPointDescriptor {

    private final String handlerMethodName;
    private final String targetMethodSignature;
    private final String atPointValue;
    private final boolean cancellable;

    /**
     * Constructs a new {@link VesselInjectionPointDescriptor}.
     *
     * @param handlerMethodName     name of the Mixin handler method (must not be {@code null} or blank)
     * @param targetMethodSignature target method name/descriptor signature (must not be {@code null} or blank)
     * @param atPointValue          the {@code @At} point value, such as {@code HEAD}, {@code RETURN}, {@code TAIL}, or {@code INVOKE} (must not be {@code null} or blank)
     * @param cancellable           {@code true} if the injection can cancel the target method execution
     * @throws NullPointerException     if any string parameter is {@code null}
     * @throws IllegalArgumentException if any string parameter is blank
     */
    public VesselInjectionPointDescriptor(
            String handlerMethodName,
            String targetMethodSignature,
            String atPointValue,
            boolean cancellable) {
        Objects.requireNonNull(handlerMethodName, "handlerMethodName must not be null");
        Objects.requireNonNull(targetMethodSignature, "targetMethodSignature must not be null");
        Objects.requireNonNull(atPointValue, "atPointValue must not be null");
        if (handlerMethodName.isBlank()) {
            throw new IllegalArgumentException("handlerMethodName must not be blank");
        }
        if (targetMethodSignature.isBlank()) {
            throw new IllegalArgumentException("targetMethodSignature must not be blank");
        }
        if (atPointValue.isBlank()) {
            throw new IllegalArgumentException("atPointValue must not be blank");
        }
        this.handlerMethodName = handlerMethodName;
        this.targetMethodSignature = targetMethodSignature;
        this.atPointValue = atPointValue;
        this.cancellable = cancellable;
    }

    /**
     * Returns the Mixin handler method name.
     *
     * @return handler method name
     */
    public String getHandlerMethodName() {
        return this.handlerMethodName;
    }

    /**
     * Returns the target method signature or selector.
     *
     * @return target method signature
     */
    public String getTargetMethodSignature() {
        return this.targetMethodSignature;
    }

    /**
     * Returns the {@code @At} injection point value (e.g., {@code HEAD}, {@code RETURN}, {@code TAIL}, {@code INVOKE}).
     *
     * @return the {@code @At} point value
     */
    public String getAtPointValue() {
        return this.atPointValue;
    }

    /**
     * Indicates whether this injection point is marked as cancellable.
     *
     * @return {@code true} if cancellable, {@code false} otherwise
     */
    public boolean isCancellable() {
        return this.cancellable;
    }

    /**
     * Checks whether this injection point targets the method head ({@code HEAD}).
     *
     * @return {@code true} if {@code "HEAD".equalsIgnoreCase(atPointValue)}
     */
    public boolean isHeadInjection() {
        return "HEAD".equalsIgnoreCase(this.atPointValue);
    }

    /**
     * Checks whether this injection point targets a method return or tail exit
     * ({@code RETURN} or {@code TAIL}).
     *
     * @return {@code true} if {@code atPointValue} matches {@code RETURN} or {@code TAIL} (case-insensitive)
     */
    public boolean isReturnInjection() {
        return "RETURN".equalsIgnoreCase(this.atPointValue) || "TAIL".equalsIgnoreCase(this.atPointValue);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof VesselInjectionPointDescriptor other)) {
            return false;
        }
        return this.cancellable == other.cancellable
                && this.handlerMethodName.equals(other.handlerMethodName)
                && this.targetMethodSignature.equals(other.targetMethodSignature)
                && this.atPointValue.equals(other.atPointValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                this.handlerMethodName,
                this.targetMethodSignature,
                this.atPointValue,
                this.cancellable);
    }

    @Override
    public String toString() {
        return "VesselInjectionPointDescriptor{"
                + "handlerMethodName='" + this.handlerMethodName + '\''
                + ", targetMethodSignature='" + this.targetMethodSignature + '\''
                + ", atPointValue='" + this.atPointValue + '\''
                + ", cancellable=" + this.cancellable
                + '}';
    }
}
