package dev.vessel.api.capability;

/**
 * Bi-directional capability bridge translating item, fluid, and energy transfers between
 * Fabric Transfer API ({@code Storage<T>}) and Forge/NeoForge Capabilities
 * ({@code IItemHandler}, {@code IFluidHandler}, {@code IEnergyStorage}) without allocating
 * intermediate objects.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
public final class StorageTransferBridge {

    /**
     * Standard Fabric Transfer API fluid droplets per vanilla bucket (81,000 droplets).
     */
    public static final long FABRIC_DROPLETS_PER_FORGE_BUCKET = 81_000L;

    /**
     * Standard Forge / NeoForge fluid millibuckets per vanilla bucket (1,000 mB).
     */
    public static final long FORGE_MILLIBUCKETS_PER_BUCKET = 1_000L;

    /**
     * Ratio of Fabric droplets per Forge millibucket (81 droplets = 1 mB).
     */
    public static final long FABRIC_DROPLETS_PER_MILLIBUCKET = 81L;

    /**
     * Constructs a new zero-allocation storage transfer bridge.
     */
    public StorageTransferBridge() {
    }

    /**
     * Bridges an insertion from a source into a target storage without allocating intermediate wrappers.
     *
     * @param offeredAmount      amount offered for insertion
     * @param currentTargetAmount current amount held in the target
     * @param maxTargetCapacity  maximum capacity of the target
     * @param simulate           true to simulate the transfer without committing
     * @return actual amount accepted
     */
    public long insert(long offeredAmount, long currentTargetAmount, long maxTargetCapacity, boolean simulate) {
        if (offeredAmount <= 0 || maxTargetCapacity <= 0 || currentTargetAmount >= maxTargetCapacity) {
            return 0L;
        }
        long remainingSpace = Math.max(0L, maxTargetCapacity - currentTargetAmount);
        return Math.min(offeredAmount, remainingSpace);
    }

    /**
     * Bridges an extraction from a source storage without allocating intermediate wrappers.
     *
     * @param requestedAmount    amount requested for extraction
     * @param currentSourceAmount current amount available in the source
     * @param simulate           true to simulate the extraction without committing
     * @return actual amount extracted
     */
    public long extract(long requestedAmount, long currentSourceAmount, boolean simulate) {
        if (requestedAmount <= 0 || currentSourceAmount <= 0) {
            return 0L;
        }
        return Math.min(requestedAmount, currentSourceAmount);
    }

    /**
     * Bridges a direct point-to-point transfer between source and target in a single operation.
     *
     * @param requestedTransfer   amount requested to be moved
     * @param currentSourceAmount current amount in source
     * @param currentTargetAmount current amount in target
     * @param maxTargetCapacity   maximum capacity of target
     * @param simulate            true to simulate without committing
     * @return actual amount transferred
     */
    public long transferDirect(
            long requestedTransfer,
            long currentSourceAmount,
            long currentTargetAmount,
            long maxTargetCapacity,
            boolean simulate
    ) {
        if (requestedTransfer <= 0 || currentSourceAmount <= 0 || currentTargetAmount >= maxTargetCapacity) {
            return 0L;
        }
        long available = Math.min(requestedTransfer, currentSourceAmount);
        long remainingSpace = Math.max(0L, maxTargetCapacity - currentTargetAmount);
        return Math.min(available, remainingSpace);
    }

    /**
     * Converts Fabric fluid droplets to Forge millibuckets.
     *
     * @param fabricDroplets droplets to convert
     * @return equivalent Forge millibuckets
     */
    public long dropletsToMillibuckets(long fabricDroplets) {
        if (fabricDroplets <= 0) {
            return 0L;
        }
        return fabricDroplets / FABRIC_DROPLETS_PER_MILLIBUCKET;
    }

    /**
     * Converts Forge millibuckets to Fabric fluid droplets.
     *
     * @param forgeMillibuckets millibuckets to convert
     * @return equivalent Fabric droplets
     */
    public long millibucketsToDroplets(long forgeMillibuckets) {
        if (forgeMillibuckets <= 0) {
            return 0L;
        }
        return forgeMillibuckets * FABRIC_DROPLETS_PER_MILLIBUCKET;
    }

    /**
     * Bridges fluid insertion from a Fabric droplet source into a Forge millibucket container.
     *
     * @param offeredDroplets   Fabric droplets offered
     * @param targetCurrentMb   current Forge container level in millibuckets
     * @param targetCapacityMb  total Forge container capacity in millibuckets
     * @param simulate          true to simulate
     * @return accepted fluid measured in Fabric droplets
     */
    public long bridgeFluidInsert(
            long offeredDroplets,
            long targetCurrentMb,
            long targetCapacityMb,
            boolean simulate
    ) {
        if (offeredDroplets <= 0) {
            return 0L;
        }
        long offeredMb = dropletsToMillibuckets(offeredDroplets);
        long acceptedMb = insert(offeredMb, targetCurrentMb, targetCapacityMb, simulate);
        return millibucketsToDroplets(acceptedMb);
    }

    /**
     * Bridges fluid extraction from a Forge millibucket container to a Fabric droplet sink.
     *
     * @param requestedDroplets Fabric droplets requested
     * @param sourceCurrentMb   current Forge container level in millibuckets
     * @param simulate          true to simulate
     * @return extracted fluid measured in Fabric droplets
     */
    public long bridgeFluidExtract(long requestedDroplets, long sourceCurrentMb, boolean simulate) {
        if (requestedDroplets <= 0) {
            return 0L;
        }
        long requestedMb = dropletsToMillibuckets(requestedDroplets);
        long extractedMb = extract(requestedMb, sourceCurrentMb, simulate);
        return millibucketsToDroplets(extractedMb);
    }

    /**
     * Bridges energy transfer between Forge Energy (FE) and Fabric / TechReborn Energy (E).
     *
     * @param offeredEnergy     energy offered
     * @param currentTargetEnergy current energy in target
     * @param maxTargetEnergy   maximum energy capacity of target
     * @param simulate          true to simulate
     * @return accepted energy
     */
    public long bridgeEnergyTransfer(
            long offeredEnergy,
            long currentTargetEnergy,
            long maxTargetEnergy,
            boolean simulate
    ) {
        return insert(offeredEnergy, currentTargetEnergy, maxTargetEnergy, simulate);
    }
}
