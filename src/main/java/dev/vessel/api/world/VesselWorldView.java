package dev.vessel.api.world;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance primitive read and state accessor for Minecraft worlds,
 * providing direct O(1) access to block types, world time, weather conditions,
 * and chunk residency status under VUSA Chapter 3.6.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Pass K-33)
 */
public final class VesselWorldView {

    /**
     * Default block type identifier representing air (ID = 0).
     */
    public static final int DEFAULT_AIR_TYPE_ID = 0;

    /**
     * Default initial world time (1000 ticks = morning).
     */
    public static final long DEFAULT_INITIAL_WORLD_TIME = 1000L;

    private static final VesselWorldView DEFAULT_INSTANCE = new VesselWorldView();

    private final Map<Object, Map<Long, Integer>> worldBlockTypes;
    private final Map<Object, Long> worldTimes;
    private final Map<Object, Boolean> worldStorms;
    private final Map<Object, Set<Long>> worldLoadedChunks;

    /**
     * Returns the global default world view instance.
     *
     * @return singleton world view
     */
    public static VesselWorldView getInstance() {
        return DEFAULT_INSTANCE;
    }

    /**
     * Constructs a new world view storage engine with concurrent tables.
     */
    public VesselWorldView() {
        this.worldBlockTypes = new ConcurrentHashMap<>();
        this.worldTimes = new ConcurrentHashMap<>();
        this.worldStorms = new ConcurrentHashMap<>();
        this.worldLoadedChunks = new ConcurrentHashMap<>();
    }

    /**
     * Returns the numeric block type ID at the specified 3D block coordinates.
     *
     * @param world world handle
     * @param x     block X coordinate
     * @param y     block Y coordinate
     * @param z     block Z coordinate
     * @return numeric block type identifier (0 if unset/air)
     */
    public int getBlockTypeId(Object world, int x, int y, int z) {
        if (world == null) {
            return DEFAULT_AIR_TYPE_ID;
        }
        Map<Long, Integer> blocks = worldBlockTypes.get(world);
        if (blocks == null) {
            return DEFAULT_AIR_TYPE_ID;
        }
        long key = CoordinateFastPacker.pack(x, y, z);
        return blocks.getOrDefault(key, DEFAULT_AIR_TYPE_ID);
    }

    /**
     * Sets the numeric block type ID at the specified 3D block coordinates.
     *
     * @param world  world handle
     * @param x      block X coordinate
     * @param y      block Y coordinate
     * @param z      block Z coordinate
     * @param typeId numeric block type ID
     */
    public void setBlockTypeId(Object world, int x, int y, int z, int typeId) {
        Objects.requireNonNull(world, "world cannot be null");
        long key = CoordinateFastPacker.pack(x, y, z);
        worldBlockTypes.computeIfAbsent(world, k -> new ConcurrentHashMap<>()).put(key, typeId);
    }

    /**
     * Retrieves the current game time in ticks for the specified world.
     *
     * @param world world handle
     * @return world time in ticks
     */
    public long getTime(Object world) {
        if (world == null) {
            return DEFAULT_INITIAL_WORLD_TIME;
        }
        return worldTimes.getOrDefault(world, DEFAULT_INITIAL_WORLD_TIME);
    }

    /**
     * Sets the game time in ticks for the specified world.
     *
     * @param world world handle
     * @param time  new world time in ticks
     */
    public void setTime(Object world, long time) {
        Objects.requireNonNull(world, "world cannot be null");
        worldTimes.put(world, time);
    }

    /**
     * Checks if the specified world currently has an active storm.
     *
     * @param world world handle
     * @return true if storming
     */
    public boolean hasStorm(Object world) {
        if (world == null) {
            return false;
        }
        return worldStorms.getOrDefault(world, Boolean.FALSE);
    }

    /**
     * Sets the storm weather state for the specified world.
     *
     * @param world world handle
     * @param storm true to start storm, false to clear
     */
    public void setStorm(Object world, boolean storm) {
        Objects.requireNonNull(world, "world cannot be null");
        worldStorms.put(world, storm);
    }

    /**
     * Checks if the chunk at the specified coordinates is loaded in memory.
     *
     * @param world  world handle
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @return true if chunk is currently loaded
     */
    public boolean isChunkLoaded(Object world, int chunkX, int chunkZ) {
        if (world == null) {
            return false;
        }
        Set<Long> loaded = worldLoadedChunks.get(world);
        if (loaded == null) {
            return false;
        }
        long key = CoordinateFastPacker.packChunk(chunkX, chunkZ);
        return loaded.contains(key);
    }

    /**
     * Sets the loaded status of a chunk in memory.
     *
     * @param world  world handle
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @param loaded true if loaded, false if unloaded
     */
    public void setChunkLoaded(Object world, int chunkX, int chunkZ, boolean loaded) {
        Objects.requireNonNull(world, "world cannot be null");
        long key = CoordinateFastPacker.packChunk(chunkX, chunkZ);
        if (loaded) {
            worldLoadedChunks.computeIfAbsent(world, k -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(key);
        } else {
            Set<Long> set = worldLoadedChunks.get(world);
            if (set != null) {
                set.remove(key);
            }
        }
    }

    /**
     * Clears all world state data from cache.
     */
    public void clearAll() {
        worldBlockTypes.clear();
        worldTimes.clear();
        worldStorms.clear();
        worldLoadedChunks.clear();
    }
}
