package dev.vessel.api.world;

import java.util.Objects;

/**
 * Direct opcode receptor and symmetry adapter for Bukkit's {@code org.bukkit.World} API,
 * routing block queries, entity spawns, item drops, and explosion effects directly into VESSEL's
 * sovereign world substrate without proxy allocations.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Pass K-33)
 */
public final class BukkitWorldSymmetryAdapter {

    private static final BukkitWorldSymmetryAdapter DEFAULT_INSTANCE = new BukkitWorldSymmetryAdapter();

    private final VesselWorldView worldView;
    private final WorldEntitySpatialGrid spatialGrid;
    private final WorldEnvironmentalGovernor environmentalGovernor;

    /**
     * Returns the global default adapter instance.
     *
     * @return singleton adapter
     */
    public static BukkitWorldSymmetryAdapter getInstance() {
        return DEFAULT_INSTANCE;
    }

    /**
     * Constructs a symmetry adapter using default subsystems.
     */
    public BukkitWorldSymmetryAdapter() {
        this(
                VesselWorldView.getInstance(),
                WorldEntitySpatialGrid.getInstance(),
                WorldEnvironmentalGovernor.getInstance()
        );
    }

    /**
     * Constructs a symmetry adapter with custom subsystems.
     *
     * @param worldView             world view engine
     * @param spatialGrid           entity spatial grid
     * @param environmentalGovernor environmental governor
     */
    public BukkitWorldSymmetryAdapter(
            VesselWorldView worldView,
            WorldEntitySpatialGrid spatialGrid,
            WorldEnvironmentalGovernor environmentalGovernor
    ) {
        this.worldView = Objects.requireNonNull(worldView, "worldView cannot be null");
        this.spatialGrid = Objects.requireNonNull(spatialGrid, "spatialGrid cannot be null");
        this.environmentalGovernor = Objects.requireNonNull(environmentalGovernor, "environmentalGovernor cannot be null");
    }

    /**
     * Resolves the block state / type at the specified coordinates (Bukkit World.getBlockAt).
     *
     * @param world world handle
     * @param x     X coordinate
     * @param y     Y coordinate
     * @param z     Z coordinate
     * @return numeric block type ID or handle
     */
    public Object getBlockAt(Object world, int x, int y, int z) {
        return worldView.getBlockTypeId(world, x, y, z);
    }

    /**
     * Spawns an entity in the world at the specified coordinates (Bukkit World.spawnEntity).
     *
     * @param world      world handle
     * @param x          X coordinate
     * @param y          Y coordinate
     * @param z          Z coordinate
     * @param entityType entity descriptor or type
     * @return spawned entity handle
     */
    public Object spawnEntity(Object world, double x, double y, double z, Object entityType) {
        Objects.requireNonNull(world, "world cannot be null");
        Objects.requireNonNull(entityType, "entityType cannot be null");

        // Canonical mock representation of spawned entity handle
        String entityInstance = "vessel_entity[" + entityType + "@" + (long) x + "," + (long) y + "," + (long) z + "]";
        spatialGrid.registerEntity(world, entityInstance, x, y, z);
        return entityInstance;
    }

    /**
     * Drops an item stack in the world as an item entity (Bukkit World.dropItem).
     *
     * @param world     world handle
     * @param x         X coordinate
     * @param y         Y coordinate
     * @param z         Z coordinate
     * @param itemStack item stack to drop
     * @return dropped item entity handle
     */
    public Object dropItem(Object world, double x, double y, double z, Object itemStack) {
        Objects.requireNonNull(world, "world cannot be null");
        Objects.requireNonNull(itemStack, "itemStack cannot be null");

        String itemEntityInstance = "vessel_item_drop[" + itemStack + "@" + (long) x + "," + (long) y + "," + (long) z + "]";
        spatialGrid.registerEntity(world, itemEntityInstance, x, y, z);
        return itemEntityInstance;
    }

    /**
     * Sets the world's solar clock time in ticks (Bukkit World.setTime).
     *
     * @param world world handle
     * @param time  time in ticks
     */
    public void setTime(Object world, long time) {
        environmentalGovernor.setTime(world, time);
    }

    /**
     * Simulates an explosion in the world (Bukkit World.createExplosion).
     *
     * @param world world handle
     * @param x     X coordinate
     * @param y     Y coordinate
     * @param z     Z coordinate
     * @param power blast power
     * @return true if explosion succeeded
     */
    public boolean createExplosion(Object world, double x, double y, double z, float power) {
        return environmentalGovernor.createExplosion(world, x, y, z, power, false);
    }

    /**
     * Simulates an explosion with fire generation (Bukkit World.createExplosion).
     *
     * @param world   world handle
     * @param x       X coordinate
     * @param y       Y coordinate
     * @param z       Z coordinate
     * @param power   blast power
     * @param setFire whether fire is ignited
     * @return true if explosion succeeded
     */
    public boolean createExplosion(Object world, double x, double y, double z, float power, boolean setFire) {
        return environmentalGovernor.createExplosion(world, x, y, z, power, setFire);
    }

    // =========================================================================
    // STATIC ASM OP-CODE RECEPTORS (INVOKEINTERFACE -> INVOKESTATIC)
    // =========================================================================

    /**
     * Opcode receptor for {@code World.getBlockAt}.
     */
    public static Object getBlock(Object world, int x, int y, int z) {
        return getInstance().getBlockAt(world, x, y, z);
    }

    /**
     * Opcode receptor for {@code World.spawnEntity}.
     */
    public static Object spawn(Object world, double x, double y, double z, Object entityType) {
        return getInstance().spawnEntity(world, x, y, z, entityType);
    }

    /**
     * Opcode receptor for {@code World.dropItem}.
     */
    public static Object drop(Object world, double x, double y, double z, Object itemStack) {
        return getInstance().dropItem(world, x, y, z, itemStack);
    }

    /**
     * Opcode receptor for {@code World.setTime}.
     */
    public static void setWorldTime(Object world, long time) {
        getInstance().setTime(world, time);
    }

    /**
     * Opcode receptor for {@code World.createExplosion}.
     */
    public static boolean explode(Object world, double x, double y, double z, float power, boolean setFire) {
        return getInstance().createExplosion(world, x, y, z, power, setFire);
    }
}
