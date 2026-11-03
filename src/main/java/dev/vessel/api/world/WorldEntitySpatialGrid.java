package dev.vessel.api.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * High-performance spatial grid partitioning entities into 16x16x16 block cells for
 * O(1) expected-time radius queries, avoiding linear full-world entity iterations.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Pass K-33)
 */
public final class WorldEntitySpatialGrid {

    /**
     * Width, height, and depth of a spatial grid cell in blocks (16 blocks = 1 chunk section).
     */
    public static final int CELL_SIZE = 16;

    private static final WorldEntitySpatialGrid DEFAULT_INSTANCE = new WorldEntitySpatialGrid();

    private final Map<Object, Map<Long, List<Object>>> worldCellEntities;
    private final Map<Object, Map<Object, double[]>> worldEntityPositions;

    /**
     * Returns the global default spatial grid instance.
     *
     * @return singleton spatial grid
     */
    public static WorldEntitySpatialGrid getInstance() {
        return DEFAULT_INSTANCE;
    }

    /**
     * Constructs a new spatial grid with concurrent partitioned storage.
     */
    public WorldEntitySpatialGrid() {
        this.worldCellEntities = new ConcurrentHashMap<>();
        this.worldEntityPositions = new ConcurrentHashMap<>();
    }

    /**
     * Registers an entity into the spatial grid at the given world coordinates.
     *
     * @param world  world handle
     * @param entity entity instance
     * @param x      X coordinate
     * @param y      Y coordinate
     * @param z      Z coordinate
     */
    public void registerEntity(Object world, Object entity, double x, double y, double z) {
        Objects.requireNonNull(world, "world cannot be null");
        Objects.requireNonNull(entity, "entity cannot be null");

        removeEntity(world, entity);

        double[] coords = new double[]{x, y, z};
        worldEntityPositions.computeIfAbsent(world, k -> new ConcurrentHashMap<>()).put(entity, coords);

        long cellKey = computeCellKey(x, y, z);
        worldCellEntities.computeIfAbsent(world, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(cellKey, k -> new CopyOnWriteArrayList<>())
                .add(entity);
    }

    /**
     * Updates an entity's coordinates within the spatial grid.
     *
     * @param world  world handle
     * @param entity entity instance
     * @param x      new X coordinate
     * @param y      new Y coordinate
     * @param z      new Z coordinate
     */
    public void updateEntityPosition(Object world, Object entity, double x, double y, double z) {
        registerEntity(world, entity, x, y, z);
    }

    /**
     * Removes an entity from the spatial grid.
     *
     * @param world  world handle
     * @param entity entity instance to remove
     */
    public void removeEntity(Object world, Object entity) {
        if (world == null || entity == null) {
            return;
        }

        Map<Object, double[]> positions = worldEntityPositions.get(world);
        if (positions != null) {
            double[] oldCoords = positions.remove(entity);
            if (oldCoords != null) {
                long oldCellKey = computeCellKey(oldCoords[0], oldCoords[1], oldCoords[2]);
                Map<Long, List<Object>> cells = worldCellEntities.get(world);
                if (cells != null) {
                    List<Object> cellList = cells.get(oldCellKey);
                    if (cellList != null) {
                        cellList.remove(entity);
                        if (cellList.isEmpty()) {
                            cells.remove(oldCellKey, cellList);
                        }
                    }
                }
            }
        }
    }

    /**
     * Queries all entities within the specified spherical radius around a center point.
     *
     * @param world  world handle
     * @param cx     center X coordinate
     * @param cy     center Y coordinate
     * @param cz     center Z coordinate
     * @param radius search radius in blocks
     * @return list of entities strictly within the radius
     */
    public List<Object> getEntitiesInRadius(Object world, double cx, double cy, double cz, double radius) {
        if (world == null || radius <= 0.0) {
            return Collections.emptyList();
        }

        Map<Long, List<Object>> cells = worldCellEntities.get(world);
        Map<Object, double[]> positions = worldEntityPositions.get(world);
        if (cells == null || positions == null) {
            return Collections.emptyList();
        }

        int minCellX = (int) Math.floor((cx - radius) / CELL_SIZE);
        int maxCellX = (int) Math.floor((cx + radius) / CELL_SIZE);
        int minCellY = (int) Math.floor((cy - radius) / CELL_SIZE);
        int maxCellY = (int) Math.floor((cy + radius) / CELL_SIZE);
        int minCellZ = (int) Math.floor((cz - radius) / CELL_SIZE);
        int maxCellZ = (int) Math.floor((cz + radius) / CELL_SIZE);

        double radiusSq = radius * radius;
        List<Object> results = new ArrayList<>();

        for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
            for (int cellY = minCellY; cellY <= maxCellY; cellY++) {
                for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                    long cellKey = CoordinateFastPacker.pack(cellX, cellY, cellZ);
                    List<Object> inCell = cells.get(cellKey);
                    if (inCell != null) {
                        for (Object entity : inCell) {
                            double[] pos = positions.get(entity);
                            if (pos != null) {
                                double dx = pos[0] - cx;
                                double dy = pos[1] - cy;
                                double dz = pos[2] - cz;
                                double distSq = dx * dx + dy * dy + dz * dz;
                                if (distSq <= radiusSq) {
                                    results.add(entity);
                                }
                            }
                        }
                    }
                }
            }
        }

        return results;
    }

    /**
     * Returns all entities located in the specified 16x16 chunk column.
     *
     * @param world  world handle
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @return list of entities in chunk
     */
    public List<Object> getEntitiesInChunk(Object world, int chunkX, int chunkZ) {
        if (world == null) {
            return Collections.emptyList();
        }

        Map<Object, double[]> positions = worldEntityPositions.get(world);
        if (positions == null) {
            return Collections.emptyList();
        }

        int minBlockX = chunkX << 4;
        int maxBlockX = minBlockX + 15;
        int minBlockZ = chunkZ << 4;
        int maxBlockZ = minBlockZ + 15;

        List<Object> results = new ArrayList<>();
        for (Map.Entry<Object, double[]> entry : positions.entrySet()) {
            double[] pos = entry.getValue();
            if (pos[0] >= minBlockX && pos[0] <= maxBlockX && pos[2] >= minBlockZ && pos[2] <= maxBlockZ) {
                results.add(entry.getKey());
            }
        }
        return results;
    }

    /**
     * Returns the total number of tracked entities in a world.
     *
     * @param world world handle
     * @return active entity count
     */
    public int getEntityCount(Object world) {
        if (world == null) {
            return 0;
        }
        Map<Object, double[]> positions = worldEntityPositions.get(world);
        return positions != null ? positions.size() : 0;
    }

    /**
     * Clears all entities associated with a specific world.
     *
     * @param world world handle
     */
    public void clearWorld(Object world) {
        if (world != null) {
            worldCellEntities.remove(world);
            worldEntityPositions.remove(world);
        }
    }

    private static long computeCellKey(double x, double y, double z) {
        int cellX = (int) Math.floor(x / CELL_SIZE);
        int cellY = (int) Math.floor(y / CELL_SIZE);
        int cellZ = (int) Math.floor(z / CELL_SIZE);
        return CoordinateFastPacker.pack(cellX, cellY, cellZ);
    }
}
