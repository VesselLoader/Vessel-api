package dev.vessel.api.capability;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages lifecycle and eviction of NeoForge DataAttachment instances on Entities and
 * BlockEntities, preventing RAM leaks when entities despawn or blocks are removed.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
public final class AttachmentMemoryEvictor {

    public static final int DEFAULT_INITIAL_ENTITIES = 256;
    public static final int DEFAULT_INITIAL_BLOCK_ENTITIES = 512;

    private final Map<Integer, Map<String, Object>> entityAttachments;
    private final Map<Long, Map<String, Object>> blockEntityAttachments;

    /**
     * Constructs a new attachment memory evictor with default concurrent tables.
     */
    public AttachmentMemoryEvictor() {
        this(DEFAULT_INITIAL_ENTITIES, DEFAULT_INITIAL_BLOCK_ENTITIES);
    }

    /**
     * Constructs a new attachment memory evictor with specified initial capacities.
     *
     * @param initialEntities     initial capacity for entity registry
     * @param initialBlockEntities initial capacity for block entity registry
     */
    public AttachmentMemoryEvictor(int initialEntities, int initialBlockEntities) {
        this.entityAttachments = new ConcurrentHashMap<>(initialEntities);
        this.blockEntityAttachments = new ConcurrentHashMap<>(initialBlockEntities);
    }

    /**
     * Packs 3D block coordinates into a canonical 64-bit coordinate key.
     *
     * @param x block X coordinate
     * @param y block Y coordinate
     * @param z block Z coordinate
     * @return 64-bit packed position key
     */
    public static long packBlockPos(int x, int y, int z) {
        return (((long) (x & 0x3FFFFFF)) << 38)
                | (((long) (y & 0xFFF)) << 26)
                | ((long) (z & 0x3FFFFFF));
    }

    /**
     * Associates a data attachment with a specific entity ID.
     *
     * @param entityId      runtime entity numeric ID
     * @param attachmentKey unique attachment identifier
     * @param value         attachment payload object
     */
    public void setEntityAttachment(int entityId, String attachmentKey, Object value) {
        Objects.requireNonNull(attachmentKey, "attachmentKey cannot be null");
        Objects.requireNonNull(value, "value cannot be null");
        entityAttachments.computeIfAbsent(entityId, k -> new ConcurrentHashMap<>())
                .put(attachmentKey, value);
    }

    /**
     * Resolves a data attachment from an entity.
     *
     * @param entityId      runtime entity numeric ID
     * @param attachmentKey unique attachment identifier
     * @return Optional containing the attachment value if present
     */
    public Optional<Object> getEntityAttachment(int entityId, String attachmentKey) {
        Objects.requireNonNull(attachmentKey, "attachmentKey cannot be null");
        Map<String, Object> map = entityAttachments.get(entityId);
        if (map == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(map.get(attachmentKey));
    }

    /**
     * Checks if an entity possesses a specific data attachment.
     *
     * @param entityId      runtime entity numeric ID
     * @param attachmentKey unique attachment identifier
     * @return true if attachment exists
     */
    public boolean hasEntityAttachment(int entityId, String attachmentKey) {
        Map<String, Object> map = entityAttachments.get(entityId);
        return map != null && map.containsKey(attachmentKey);
    }

    /**
     * Evicts all data attachments associated with an entity, freeing memory upon entity death or despawn.
     *
     * @param entityId runtime entity numeric ID
     * @return count of evicted attachment entries
     */
    public int evictEntity(int entityId) {
        Map<String, Object> map = entityAttachments.remove(entityId);
        return map != null ? map.size() : 0;
    }

    /**
     * Associates a data attachment with a specific block position key.
     *
     * @param blockPosKey   packed 64-bit block position key
     * @param attachmentKey unique attachment identifier
     * @param value         attachment payload object
     */
    public void setBlockEntityAttachment(long blockPosKey, String attachmentKey, Object value) {
        Objects.requireNonNull(attachmentKey, "attachmentKey cannot be null");
        Objects.requireNonNull(value, "value cannot be null");
        blockEntityAttachments.computeIfAbsent(blockPosKey, k -> new ConcurrentHashMap<>())
                .put(attachmentKey, value);
    }

    /**
     * Associates a data attachment with 3D block coordinates.
     *
     * @param x             block X coordinate
     * @param y             block Y coordinate
     * @param z             block Z coordinate
     * @param attachmentKey unique attachment identifier
     * @param value         attachment payload object
     */
    public void setBlockEntityAttachment(int x, int y, int z, String attachmentKey, Object value) {
        setBlockEntityAttachment(packBlockPos(x, y, z), attachmentKey, value);
    }

    /**
     * Resolves a data attachment from a packed block position key.
     *
     * @param blockPosKey   packed 64-bit block position key
     * @param attachmentKey unique attachment identifier
     * @return Optional containing the attachment value if present
     */
    public Optional<Object> getBlockEntityAttachment(long blockPosKey, String attachmentKey) {
        Objects.requireNonNull(attachmentKey, "attachmentKey cannot be null");
        Map<String, Object> map = blockEntityAttachments.get(blockPosKey);
        if (map == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(map.get(attachmentKey));
    }

    /**
     * Resolves a data attachment from 3D block coordinates.
     *
     * @param x             block X coordinate
     * @param y             block Y coordinate
     * @param z             block Z coordinate
     * @param attachmentKey unique attachment identifier
     * @return Optional containing the attachment value if present
     */
    public Optional<Object> getBlockEntityAttachment(int x, int y, int z, String attachmentKey) {
        return getBlockEntityAttachment(packBlockPos(x, y, z), attachmentKey);
    }

    /**
     * Checks if a block position possesses a specific data attachment.
     *
     * @param blockPosKey   packed 64-bit block position key
     * @param attachmentKey unique attachment identifier
     * @return true if attachment exists
     */
    public boolean hasBlockEntityAttachment(long blockPosKey, String attachmentKey) {
        Map<String, Object> map = blockEntityAttachments.get(blockPosKey);
        return map != null && map.containsKey(attachmentKey);
    }

    /**
     * Evicts all data attachments associated with a block position key, freeing memory upon block break.
     *
     * @param blockPosKey packed 64-bit block position key
     * @return count of evicted attachment entries
     */
    public int evictBlockEntity(long blockPosKey) {
        Map<String, Object> map = blockEntityAttachments.remove(blockPosKey);
        return map != null ? map.size() : 0;
    }

    /**
     * Evicts all data attachments associated with 3D block coordinates.
     *
     * @param x block X coordinate
     * @param y block Y coordinate
     * @param z block Z coordinate
     * @return count of evicted attachment entries
     */
    public int evictBlockEntity(int x, int y, int z) {
        return evictBlockEntity(packBlockPos(x, y, z));
    }

    /**
     * Returns the count of entities with registered attachments.
     *
     * @return active entity count
     */
    public int getActiveEntityCount() {
        return entityAttachments.size();
    }

    /**
     * Returns the count of block entities with registered attachments.
     *
     * @return active block entity count
     */
    public int getActiveBlockEntityCount() {
        return blockEntityAttachments.size();
    }

    /**
     * Returns the total count of all individual entity attachment instances.
     *
     * @return total entity attachment count
     */
    public int getTotalEntityAttachmentCount() {
        int count = 0;
        for (Map<String, Object> map : entityAttachments.values()) {
            count += map.size();
        }
        return count;
    }

    /**
     * Returns the total count of all individual block entity attachment instances.
     *
     * @return total block entity attachment count
     */
    public int getTotalBlockEntityAttachmentCount() {
        int count = 0;
        for (Map<String, Object> map : blockEntityAttachments.values()) {
            count += map.size();
        }
        return count;
    }

    /**
     * Clears all entity and block entity attachments from memory.
     */
    public void clearAll() {
        entityAttachments.clear();
        blockEntityAttachments.clear();
    }
}
