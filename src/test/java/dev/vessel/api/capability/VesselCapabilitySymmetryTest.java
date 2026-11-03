package dev.vessel.api.capability;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verification test suite for the Universal Capability & Attachment Bridge under VUSA Chapter 3.2.
 * Validates cross-loader symmetry, nested transaction rollbacks, zero-allocation storage transfers,
 * and memory eviction sentinels.
 *
 * <p>Strictly adheres to Rule 17: Zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
public final class VesselCapabilitySymmetryTest {

    private CapabilityTransactionSimulator transactionSimulator;
    private StorageTransferBridge transferBridge;
    private WeakCapabilitySentinel capabilitySentinel;
    private AttachmentMemoryEvictor memoryEvictor;

    @BeforeEach
    public void setUp() {
        this.transactionSimulator = new CapabilityTransactionSimulator();
        this.transferBridge = new StorageTransferBridge();
        this.capabilitySentinel = new WeakCapabilitySentinel();
        this.memoryEvictor = new AttachmentMemoryEvictor();
    }

    @Test
    @DisplayName("VesselUnifiedCapability validates correctly and revokes on invalidate()")
    public void testUnifiedCapabilityLifecycle() {
        String testProvider = "forge_item_handler_provider_mock";
        VesselUnifiedCapability<String> capability = new VesselUnifiedCapability<>(
                "vessel:item_storage",
                String.class,
                testProvider
        );

        assertThat(capability.getCapabilityId()).isEqualTo("vessel:item_storage");
        assertThat(capability.getCapabilityType()).isEqualTo(String.class);
        assertThat(capability.isValid()).isTrue();
        assertThat(capability.getProvider()).isPresent().contains("forge_item_handler_provider_mock");

        capability.invalidate();

        assertThat(capability.isValid()).isFalse();
        assertThat(capability.getProvider()).isEmpty();
    }

    @Test
    @DisplayName("CapabilityTransactionSimulator executes nested transaction rollbacks with zero duplication")
    public void testNestedTransactionRollbackSymmetry() {
        // Initial setup: Slot 0 has 100 items, Slot 1 has 50 items
        transactionSimulator.setSlotAmount(0, 100L);
        transactionSimulator.setSlotAmount(1, 50L);

        // Tier 1 Transaction
        int depth1 = transactionSimulator.beginNestedTransaction();
        assertThat(depth1).isEqualTo(1);
        assertThat(transactionSimulator.getCurrentDepth()).isEqualTo(1);

        transactionSimulator.recordMutation(0, 120L); // Slot 0: 100 -> 120
        assertThat(transactionSimulator.getSlotAmount(0)).isEqualTo(120L);

        // Tier 2 Nested Transaction
        int depth2 = transactionSimulator.beginNestedTransaction();
        assertThat(depth2).isEqualTo(2);

        transactionSimulator.recordMutation(0, 150L); // Slot 0: 120 -> 150
        transactionSimulator.recordMutation(1, 80L);  // Slot 1: 50 -> 80
        assertThat(transactionSimulator.getSlotAmount(0)).isEqualTo(150L);
        assertThat(transactionSimulator.getSlotAmount(1)).isEqualTo(80L);

        // Abort Tier 2 Transaction: Should revert slot 0 to 120 and slot 1 to 50
        int restoredCount = transactionSimulator.abortAndRollbackTransaction();
        assertThat(restoredCount).isEqualTo(2);
        assertThat(transactionSimulator.getCurrentDepth()).isEqualTo(1);
        assertThat(transactionSimulator.getSlotAmount(0)).isEqualTo(120L);
        assertThat(transactionSimulator.getSlotAmount(1)).isEqualTo(50L);

        // Commit Tier 1 Transaction: Finalizes slot 0 at 120L and slot 1 at 50L
        int committedDepth = transactionSimulator.commitTransaction();
        assertThat(committedDepth).isEqualTo(1);
        assertThat(transactionSimulator.getCurrentDepth()).isEqualTo(0);
        assertThat(transactionSimulator.getSlotAmount(0)).isEqualTo(120L);
        assertThat(transactionSimulator.getSlotAmount(1)).isEqualTo(50L);
    }

    @Test
    @DisplayName("StorageTransferBridge executes zero-allocation item insertion and extraction")
    public void testStorageTransferBridgeItemOperations() {
        long currentTarget = 40L;
        long targetCapacity = 64L;
        long offered = 30L;

        // Simulate insertion
        long simulatedInsert = transferBridge.insert(offered, currentTarget, targetCapacity, true);
        assertThat(simulatedInsert).isEqualTo(24L); // Space remaining = 64 - 40 = 24

        // Actual insertion calculation
        long actualInsert = transferBridge.insert(offered, currentTarget, targetCapacity, false);
        assertThat(actualInsert).isEqualTo(24L);

        // Extraction test
        long currentSource = 50L;
        long requested = 20L;
        long extracted = transferBridge.extract(requested, currentSource, false);
        assertThat(extracted).isEqualTo(20L);

        // Direct transfer test
        long directTransferred = transferBridge.transferDirect(35L, 30L, 50L, 64L, false);
        // source has 30, target can take 14 (64 - 50 = 14) -> transferred = 14
        assertThat(directTransferred).isEqualTo(14L);
    }

    @Test
    @DisplayName("StorageTransferBridge converts fluid droplets and millibuckets symmetrically")
    public void testFluidConversionAndTransferSymmetry() {
        // 81,000 droplets = 1,000 millibuckets (1 vanilla bucket)
        assertThat(transferBridge.dropletsToMillibuckets(StorageTransferBridge.FABRIC_DROPLETS_PER_FORGE_BUCKET))
                .isEqualTo(StorageTransferBridge.FORGE_MILLIBUCKETS_PER_BUCKET);

        assertThat(transferBridge.millibucketsToDroplets(StorageTransferBridge.FORGE_MILLIBUCKETS_PER_BUCKET))
                .isEqualTo(StorageTransferBridge.FABRIC_DROPLETS_PER_FORGE_BUCKET);

        // Fabric offers 162,000 droplets (2,000 mB) into Forge tank with 500 mB current, 1,500 mB max
        long offeredDroplets = 162_000L;
        long targetCurrentMb = 500L;
        long targetCapacityMb = 1_500L;

        long acceptedDroplets = transferBridge.bridgeFluidInsert(
                offeredDroplets,
                targetCurrentMb,
                targetCapacityMb,
                false
        );
        // Tank can take 1,000 mB = 81,000 droplets
        assertThat(acceptedDroplets).isEqualTo(81_000L);

        // Fabric extracts 81,000 droplets from Forge tank with 2,000 mB
        long extractedDroplets = transferBridge.bridgeFluidExtract(81_000L, 2_000L, false);
        assertThat(extractedDroplets).isEqualTo(81_000L);
    }

    @Test
    @DisplayName("WeakCapabilitySentinel automatically invalidates capabilities on chunk unload")
    public void testWeakCapabilitySentinelChunkEviction() {
        int chunkX = 12;
        int chunkZ = -34;

        VesselUnifiedCapability<String> cap1 = new VesselUnifiedCapability<>("vessel:energy", String.class, "provider1");
        VesselUnifiedCapability<String> cap2 = new VesselUnifiedCapability<>("vessel:fluids", String.class, "provider2");

        capabilitySentinel.registerCapability(chunkX, chunkZ, cap1);
        capabilitySentinel.registerCapability(chunkX, chunkZ, cap2);

        assertThat(capabilitySentinel.getTrackedChunkCount()).isEqualTo(1);
        assertThat(capabilitySentinel.getActiveCapabilityCount()).isEqualTo(2);
        assertThat(cap1.isValid()).isTrue();
        assertThat(cap2.isValid()).isTrue();

        // Trigger chunk unload eviction
        int evicted = capabilitySentinel.evictChunkCapabilities(chunkX, chunkZ);

        assertThat(evicted).isEqualTo(2);
        assertThat(cap1.isValid()).isFalse();
        assertThat(cap2.isValid()).isFalse();
        assertThat(cap1.getProvider()).isEmpty();
        assertThat(cap2.getProvider()).isEmpty();
        assertThat(capabilitySentinel.getTrackedChunkCount()).isEqualTo(0);
        assertThat(capabilitySentinel.getActiveCapabilityCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("AttachmentMemoryEvictor registers, resolves, and evicts entity/block attachments")
    public void testAttachmentMemoryEvictorLifecycle() {
        int entityId = 1042;
        String attachKey = "neoforge:mana_level";
        Double manaValue = 99.5;

        memoryEvictor.setEntityAttachment(entityId, attachKey, manaValue);

        assertThat(memoryEvictor.hasEntityAttachment(entityId, attachKey)).isTrue();
        Optional<Object> resolved = memoryEvictor.getEntityAttachment(entityId, attachKey);
        assertThat(resolved).isPresent().contains(99.5);
        assertThat(memoryEvictor.getActiveEntityCount()).isEqualTo(1);
        assertThat(memoryEvictor.getTotalEntityAttachmentCount()).isEqualTo(1);

        // Evict entity on despawn
        int evicted = memoryEvictor.evictEntity(entityId);
        assertThat(evicted).isEqualTo(1);
        assertThat(memoryEvictor.hasEntityAttachment(entityId, attachKey)).isFalse();
        assertThat(memoryEvictor.getActiveEntityCount()).isEqualTo(0);

        // Block Entity attachments
        int blockX = 100;
        int blockY = 64;
        int blockZ = -200;
        String blockKey = "neoforge:energy_buffer";
        Long energyBuffer = 50_000L;

        memoryEvictor.setBlockEntityAttachment(blockX, blockY, blockZ, blockKey, energyBuffer);
        assertThat(memoryEvictor.hasBlockEntityAttachment(AttachmentMemoryEvictor.packBlockPos(blockX, blockY, blockZ), blockKey)).isTrue();
        assertThat(memoryEvictor.getBlockEntityAttachment(blockX, blockY, blockZ, blockKey)).isPresent().contains(50_000L);
        assertThat(memoryEvictor.getActiveBlockEntityCount()).isEqualTo(1);

        // Evict block entity on block break
        int evictedBlocks = memoryEvictor.evictBlockEntity(blockX, blockY, blockZ);
        assertThat(evictedBlocks).isEqualTo(1);
        assertThat(memoryEvictor.getActiveBlockEntityCount()).isEqualTo(0);
    }
}
