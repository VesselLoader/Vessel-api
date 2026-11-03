package dev.vessel.api.storage;

import java.util.Objects;

/**
 * Direct opcode-level symmetry receptor and adapter for Fabric Transfer API v1
 * ({@code net.fabricmc.fabric.api.transfer.v1.storage.Storage<T>}).
 *
 * <p>Receives rewritten static method calls from transmutated Fabric bytecode, routing
 * transaction-based resource streams into the VESSEL native substrate without intermediate
 * heap allocations.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class FabricStorageSymmetryAdapter {

    public static final String FABRIC_STORAGE_INTERNAL = "net/fabricmc/fabric/api/transfer/v1/storage/Storage";
    public static final String FABRIC_STORAGE_VIEW_INTERNAL = "net/fabricmc/fabric/api/transfer/v1/storage/StorageView";
    public static final String FABRIC_TRANSACTION_INTERNAL = "net/fabricmc/fabric/api/transfer/v1/transaction/TransactionContext";
    public static final String RECEPTOR_CLASS_INTERNAL = "dev/vessel/api/storage/FabricStorageSymmetryAdapter";

    public static final long FABRIC_DROPLETS_PER_FORGE_BUCKET = 81_000L;
    public static final long FORGE_MILLIBUCKETS_PER_BUCKET = 1_000L;
    public static final long FABRIC_DROPLETS_PER_MILLIBUCKET = 81L;

    /**
     * Constructs a new Fabric storage symmetry adapter instance.
     */
    public FabricStorageSymmetryAdapter() {
    }

    /**
     * Direct opcode receptor for {@code Storage.insert(T resource, long maxAmount, TransactionContext transaction)}.
     *
     * @param storage            target storage object
     * @param resource           resource identifier or object
     * @param maxAmount          maximum quantity to insert
     * @param transactionContext active transaction context
     * @return accepted quantity
     */
    public static long insertFabric(Object storage, Object resource, long maxAmount, Object transactionContext) {
        if (storage == null || resource == null || maxAmount <= 0) {
            return 0L;
        }

        FabricTransactionContext tx = (transactionContext instanceof FabricTransactionContext ftc) ? ftc : null;
        String resKey = resource.toString();

        if (storage instanceof SimpleFabricStorageContainer container) {
            return container.insert(resKey, maxAmount, tx);
        }

        return 0L;
    }

    /**
     * Direct opcode receptor for {@code Storage.extract(T resource, long maxAmount, TransactionContext transaction)}.
     *
     * @param storage            target storage object
     * @param resource           resource identifier or object
     * @param maxAmount          maximum quantity to extract
     * @param transactionContext active transaction context
     * @return extracted quantity
     */
    public static long extractFabric(Object storage, Object resource, long maxAmount, Object transactionContext) {
        if (storage == null || resource == null || maxAmount <= 0) {
            return 0L;
        }

        FabricTransactionContext tx = (transactionContext instanceof FabricTransactionContext ftc) ? ftc : null;
        String resKey = resource.toString();

        if (storage instanceof SimpleFabricStorageContainer container) {
            return container.extract(resKey, maxAmount, tx);
        }

        return 0L;
    }

    /**
     * Direct opcode receptor for {@code Storage.supportsInsertion()}.
     *
     * @param storage target storage object
     * @return true if insertion is supported
     */
    public static boolean supportsInsertion(Object storage) {
        if (storage instanceof SimpleFabricStorageContainer container) {
            return container.supportsInsertion();
        }
        return false;
    }

    /**
     * Direct opcode receptor for {@code Storage.supportsExtraction()}.
     *
     * @param storage target storage object
     * @return true if extraction is supported
     */
    public static boolean supportsExtraction(Object storage) {
        if (storage instanceof SimpleFabricStorageContainer container) {
            return container.supportsExtraction();
        }
        return false;
    }

    /**
     * Direct opcode receptor for {@code Storage.getVersion()}.
     *
     * @param storage target storage object
     * @return version counter
     */
    public static long getVersion(Object storage) {
        if (storage instanceof SimpleFabricStorageContainer container) {
            return container.getVersion();
        }
        return 0L;
    }

    /**
     * Converts Fabric fluid droplets to standard Forge/NeoForge millibuckets.
     *
     * @param droplets droplets quantity
     * @return equivalent millibuckets
     */
    public static long dropletsToMillibuckets(long droplets) {
        if (droplets <= 0) {
            return 0L;
        }
        return droplets / FABRIC_DROPLETS_PER_MILLIBUCKET;
    }

    /**
     * Converts Forge/NeoForge millibuckets to Fabric fluid droplets.
     *
     * @param millibuckets millibuckets quantity
     * @return equivalent droplets
     */
    public static long millibucketsToDroplets(long millibuckets) {
        if (millibuckets <= 0) {
            return 0L;
        }
        return millibuckets * FABRIC_DROPLETS_PER_MILLIBUCKET;
    }
}
