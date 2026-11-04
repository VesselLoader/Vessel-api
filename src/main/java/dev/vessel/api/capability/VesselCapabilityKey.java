package dev.vessel.api.capability;

import java.util.Objects;

/**
 * Type-safe capability token identifying a discrete contract in the VESSEL Super-API ecosystem.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @param <T> capability contract interface
 * @version 1.1.0
 * @since 1.0.0
 */
public final class VesselCapabilityKey<T> {

    public static final VesselCapabilityKey<VesselEnergy> ENERGY =
            new VesselCapabilityKey<>("vessel:energy", VesselEnergy.class);

    public static final VesselCapabilityKey<VesselFluidHandler> FLUID =
            new VesselCapabilityKey<>("vessel:fluid", VesselFluidHandler.class);

    public static final VesselCapabilityKey<VesselItemStorage> ITEM_STORAGE =
            new VesselCapabilityKey<>("vessel:item_storage", VesselItemStorage.class);

    private final String identifier;
    private final Class<T> contractClass;

    public VesselCapabilityKey(String identifier, Class<T> contractClass) {
        this.identifier = Objects.requireNonNull(identifier, "identifier cannot be null");
        this.contractClass = Objects.requireNonNull(contractClass, "contractClass cannot be null");
    }

    public static <E> VesselCapabilityKey<E> of(String identifier, Class<E> contractClass) {
        return new VesselCapabilityKey<>(identifier, contractClass);
    }

    public String getIdentifier() {
        return identifier;
    }

    public Class<T> getContractClass() {
        return contractClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VesselCapabilityKey<?> that = (VesselCapabilityKey<?>) o;
        return Objects.equals(identifier, that.identifier);
    }

    @Override
    public int hashCode() {
        return identifier.hashCode();
    }

    @Override
    public String toString() {
        return "VesselCapabilityKey[" + identifier + "]";
    }
}
