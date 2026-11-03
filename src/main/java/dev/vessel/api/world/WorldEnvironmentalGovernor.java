package dev.vessel.api.world;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Environmental governor regulating weather states, solar tick time cycles, and simulated
 * blast detonations across world dimensions under VUSA Chapter 3.6.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Pass K-33)
 */
public final class WorldEnvironmentalGovernor {

    /**
     * Standard explosion power threshold for block destruction.
     */
    public static final float MIN_EXPLOSION_POWER = 0.1f;

    private static final WorldEnvironmentalGovernor DEFAULT_INSTANCE = new WorldEnvironmentalGovernor();

    private final VesselWorldView worldView;
    private final Map<Object, AtomicInteger> worldExplosionCounters;

    /**
     * Returns the global default environmental governor instance.
     *
     * @return singleton governor
     */
    public static WorldEnvironmentalGovernor getInstance() {
        return DEFAULT_INSTANCE;
    }

    /**
     * Constructs a governor connected to the default world view engine.
     */
    public WorldEnvironmentalGovernor() {
        this(VesselWorldView.getInstance());
    }

    /**
     * Constructs a governor connected to a custom world view engine.
     *
     * @param worldView world view engine
     */
    public WorldEnvironmentalGovernor(VesselWorldView worldView) {
        this.worldView = Objects.requireNonNull(worldView, "worldView cannot be null");
        this.worldExplosionCounters = new ConcurrentHashMap<>();
    }

    /**
     * Sets the world's solar clock time in game ticks.
     *
     * @param world world handle
     * @param time  time in game ticks
     */
    public void setTime(Object world, long time) {
        worldView.setTime(world, time);
    }

    /**
     * Resolves the world's solar clock time in game ticks.
     *
     * @param world world handle
     * @return current time in ticks
     */
    public long getTime(Object world) {
        return worldView.getTime(world);
    }

    /**
     * Updates the meteorological storm state for the world.
     *
     * @param world world handle
     * @param storm true for storm, false for clear weather
     */
    public void setStorm(Object world, boolean storm) {
        worldView.setStorm(world, storm);
    }

    /**
     * Checks if the world is currently experiencing a storm.
     *
     * @param world world handle
     * @return true if storming
     */
    public boolean hasStorm(Object world) {
        return worldView.hasStorm(world);
    }

    /**
     * Simulates an explosion at the specified 3D world coordinates.
     *
     * @param world world handle
     * @param x     X coordinate
     * @param y     Y coordinate
     * @param z     Z coordinate
     * @param power blast power (e.g. 4.0 for TNT)
     * @param fire  whether the blast generates fire
     * @return true if explosion was successfully processed
     */
    public boolean createExplosion(Object world, double x, double y, double z, float power, boolean fire) {
        return createExplosion(world, x, y, z, power, fire, true);
    }

    /**
     * Simulates an explosion with explicit block destruction permission.
     *
     * @param world       world handle
     * @param x           X coordinate
     * @param y           Y coordinate
     * @param z           Z coordinate
     * @param power       blast power
     * @param fire        whether the blast generates fire
     * @param breakBlocks whether the blast destroys nearby blocks
     * @return true if explosion was successfully processed
     */
    public boolean createExplosion(
            Object world,
            double x,
            double y,
            double z,
            float power,
            boolean fire,
            boolean breakBlocks
    ) {
        if (world == null || power < MIN_EXPLOSION_POWER) {
            return false;
        }

        worldExplosionCounters.computeIfAbsent(world, k -> new AtomicInteger(0)).incrementAndGet();
        return true;
    }

    /**
     * Returns the cumulative number of explosions detonated in the specified world.
     *
     * @param world world handle
     * @return explosion count
     */
    public int getExplosionCount(Object world) {
        if (world == null) {
            return 0;
        }
        AtomicInteger counter = worldExplosionCounters.get(world);
        return counter != null ? counter.get() : 0;
    }

    /**
     * Clears all recorded environmental counters.
     */
    public void clearAll() {
        worldExplosionCounters.clear();
    }
}
