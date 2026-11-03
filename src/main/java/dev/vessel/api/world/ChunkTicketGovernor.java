package dev.vessel.api.world;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages chunk ticket persistence and force-loading leases across loader boundaries,
 * preventing world chunks from unloading when pinned by mods, plugins, or portals.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Pass K-33)
 */
public final class ChunkTicketGovernor {

    private static final ChunkTicketGovernor DEFAULT_INSTANCE = new ChunkTicketGovernor();

    private final VesselWorldView worldView;
    private final Map<Object, Map<Long, Set<Object>>> worldTickets;

    /**
     * Returns the global default chunk ticket governor.
     *
     * @return singleton governor
     */
    public static ChunkTicketGovernor getInstance() {
        return DEFAULT_INSTANCE;
    }

    /**
     * Constructs a chunk ticket governor connected to the default world view.
     */
    public ChunkTicketGovernor() {
        this(VesselWorldView.getInstance());
    }

    /**
     * Constructs a chunk ticket governor connected to a custom world view.
     *
     * @param worldView world view engine
     */
    public ChunkTicketGovernor(VesselWorldView worldView) {
        this.worldView = Objects.requireNonNull(worldView, "worldView cannot be null");
        this.worldTickets = new ConcurrentHashMap<>();
    }

    /**
     * Adds a chunk loading ticket pinned by a plugin or subsystem.
     *
     * @param world  world handle
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @param plugin owner or subsystem holding the ticket
     * @return true if this was the first ticket pinning the chunk
     */
    public boolean addTicket(Object world, int chunkX, int chunkZ, Object plugin) {
        Objects.requireNonNull(world, "world cannot be null");
        Objects.requireNonNull(plugin, "plugin cannot be null");

        long key = CoordinateFastPacker.packChunk(chunkX, chunkZ);
        Map<Long, Set<Object>> tickets = worldTickets.computeIfAbsent(world, k -> new ConcurrentHashMap<>());
        Set<Object> holders = tickets.computeIfAbsent(key, k -> Collections.newSetFromMap(new ConcurrentHashMap<>()));

        boolean first = holders.isEmpty();
        holders.add(plugin);

        if (first) {
            worldView.setChunkLoaded(world, chunkX, chunkZ, true);
        }
        return first;
    }

    /**
     * Removes a chunk loading ticket held by a plugin or subsystem.
     *
     * @param world  world handle
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @param plugin owner releasing the ticket
     * @return true if the chunk has no remaining tickets
     */
    public boolean removeTicket(Object world, int chunkX, int chunkZ, Object plugin) {
        if (world == null || plugin == null) {
            return false;
        }

        long key = CoordinateFastPacker.packChunk(chunkX, chunkZ);
        Map<Long, Set<Object>> tickets = worldTickets.get(world);
        if (tickets == null) {
            return false;
        }

        Set<Object> holders = tickets.get(key);
        if (holders == null) {
            return false;
        }

        holders.remove(plugin);
        if (holders.isEmpty()) {
            tickets.remove(key, holders);
            worldView.setChunkLoaded(world, chunkX, chunkZ, false);
            return true;
        }
        return false;
    }

    /**
     * Checks if a chunk currently has at least one active loading ticket.
     *
     * @param world  world handle
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @return true if actively ticketed
     */
    public boolean isTicketActive(Object world, int chunkX, int chunkZ) {
        if (world == null) {
            return false;
        }
        Map<Long, Set<Object>> tickets = worldTickets.get(world);
        if (tickets == null) {
            return false;
        }
        long key = CoordinateFastPacker.packChunk(chunkX, chunkZ);
        Set<Object> holders = tickets.get(key);
        return holders != null && !holders.isEmpty();
    }

    /**
     * Returns the number of distinct ticket holders pinning the specified chunk.
     *
     * @param world  world handle
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @return ticket count
     */
    public int getTicketCount(Object world, int chunkX, int chunkZ) {
        if (world == null) {
            return 0;
        }
        Map<Long, Set<Object>> tickets = worldTickets.get(world);
        if (tickets == null) {
            return 0;
        }
        long key = CoordinateFastPacker.packChunk(chunkX, chunkZ);
        Set<Object> holders = tickets.get(key);
        return holders != null ? holders.size() : 0;
    }

    /**
     * Revokes all chunk tickets associated with a specific plugin across an entire world.
     *
     * @param world  world handle
     * @param plugin plugin handle
     * @return count of released chunk tickets
     */
    public int removeAllTicketsForPlugin(Object world, Object plugin) {
        if (world == null || plugin == null) {
            return 0;
        }

        Map<Long, Set<Object>> tickets = worldTickets.get(world);
        if (tickets == null) {
            return 0;
        }

        int removed = 0;
        for (Map.Entry<Long, Set<Object>> entry : tickets.entrySet()) {
            Set<Object> holders = entry.getValue();
            if (holders.remove(plugin)) {
                removed++;
                if (holders.isEmpty()) {
                    long key = entry.getKey();
                    tickets.remove(key, holders);
                    worldView.setChunkLoaded(world, CoordinateFastPacker.unpackChunkX(key), CoordinateFastPacker.unpackChunkZ(key), false);
                }
            }
        }
        return removed;
    }

    /**
     * Returns the total count of actively ticketed chunks in a world.
     *
     * @param world world handle
     * @return active ticketed chunk count
     */
    public int getActiveChunkCount(Object world) {
        if (world == null) {
            return 0;
        }
        Map<Long, Set<Object>> tickets = worldTickets.get(world);
        return tickets != null ? tickets.size() : 0;
    }

    /**
     * Clears all chunk tickets across all worlds.
     */
    public void clearAll() {
        worldTickets.clear();
    }
}
