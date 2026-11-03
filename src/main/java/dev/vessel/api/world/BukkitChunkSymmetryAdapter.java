package dev.vessel.api.world;

import java.util.List;
import java.util.Objects;

/**
 * Direct opcode receptor and symmetry adapter for Bukkit's {@code org.bukkit.Chunk} API,
 * translating chunk residency checks, manual load/unload requests, and chunk entity queries
 * into VESSEL's high-performance spatial world substrate.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Pass K-33)
 */
public final class BukkitChunkSymmetryAdapter {

    private static final BukkitChunkSymmetryAdapter DEFAULT_INSTANCE = new BukkitChunkSymmetryAdapter();

    private final VesselWorldView worldView;
    private final ChunkTicketGovernor ticketGovernor;
    private final WorldEntitySpatialGrid spatialGrid;

    /**
     * Returns the global default chunk adapter instance.
     *
     * @return singleton adapter
     */
    public static BukkitChunkSymmetryAdapter getInstance() {
        return DEFAULT_INSTANCE;
    }

    /**
     * Constructs a chunk symmetry adapter using default subsystems.
     */
    public BukkitChunkSymmetryAdapter() {
        this(
                VesselWorldView.getInstance(),
                ChunkTicketGovernor.getInstance(),
                WorldEntitySpatialGrid.getInstance()
        );
    }

    /**
     * Constructs a chunk symmetry adapter with custom subsystems.
     *
     * @param worldView      world view engine
     * @param ticketGovernor chunk ticket governor
     * @param spatialGrid    entity spatial grid
     */
    public BukkitChunkSymmetryAdapter(
            VesselWorldView worldView,
            ChunkTicketGovernor ticketGovernor,
            WorldEntitySpatialGrid spatialGrid
    ) {
        this.worldView = Objects.requireNonNull(worldView, "worldView cannot be null");
        this.ticketGovernor = Objects.requireNonNull(ticketGovernor, "ticketGovernor cannot be null");
        this.spatialGrid = Objects.requireNonNull(spatialGrid, "spatialGrid cannot be null");
    }

    /**
     * Loads a chunk into memory (Bukkit Chunk.load).
     *
     * @param world    world handle
     * @param chunkX   chunk X coordinate
     * @param chunkZ   chunk Z coordinate
     * @param generate whether missing chunk terrain should be generated
     * @return true if chunk is loaded
     */
    public boolean load(Object world, int chunkX, int chunkZ, boolean generate) {
        Objects.requireNonNull(world, "world cannot be null");
        worldView.setChunkLoaded(world, chunkX, chunkZ, true);
        return true;
    }

    /**
     * Unloads a chunk from memory if no tickets pin it (Bukkit Chunk.unload).
     *
     * @param world  world handle
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @param save   whether to save dirty chunk data
     * @return true if successfully unloaded; false if tickets pin the chunk
     */
    public boolean unload(Object world, int chunkX, int chunkZ, boolean save) {
        if (world == null) {
            return false;
        }
        if (ticketGovernor.isTicketActive(world, chunkX, chunkZ)) {
            // Cannot unload chunk pinned by active ticket
            return false;
        }
        worldView.setChunkLoaded(world, chunkX, chunkZ, false);
        return true;
    }

    /**
     * Checks if a chunk is loaded in memory (Bukkit Chunk.isLoaded).
     *
     * @param world  world handle
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @return true if loaded
     */
    public boolean isLoaded(Object world, int chunkX, int chunkZ) {
        return worldView.isChunkLoaded(world, chunkX, chunkZ);
    }

    /**
     * Retrieves an array of all entities residing in the specified chunk (Bukkit Chunk.getEntities).
     *
     * @param world  world handle
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @return array of entity objects in chunk
     */
    public Object[] getEntities(Object world, int chunkX, int chunkZ) {
        List<Object> entities = spatialGrid.getEntitiesInChunk(world, chunkX, chunkZ);
        return entities.toArray();
    }

    // =========================================================================
    // STATIC ASM OP-CODE RECEPTORS (INVOKEINTERFACE -> INVOKESTATIC)
    // =========================================================================

    /**
     * Opcode receptor for {@code Chunk.load}.
     */
    public static boolean loadChunk(Object world, int chunkX, int chunkZ, boolean generate) {
        return getInstance().load(world, chunkX, chunkZ, generate);
    }

    /**
     * Opcode receptor for {@code Chunk.unload}.
     */
    public static boolean unloadChunk(Object world, int chunkX, int chunkZ, boolean save) {
        return getInstance().unload(world, chunkX, chunkZ, save);
    }

    /**
     * Opcode receptor for {@code Chunk.isLoaded}.
     */
    public static boolean isChunkLoaded(Object world, int chunkX, int chunkZ) {
        return getInstance().isLoaded(world, chunkX, chunkZ);
    }

    /**
     * Opcode receptor for {@code Chunk.getEntities}.
     */
    public static Object[] getChunkEntities(Object world, int chunkX, int chunkZ) {
        return getInstance().getEntities(world, chunkX, chunkZ);
    }
}
