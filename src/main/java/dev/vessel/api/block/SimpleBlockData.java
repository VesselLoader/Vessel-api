package dev.vessel.api.block;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Clean-room immutable block data representation mirroring Bukkit {@code BlockData}
 * and Mojang {@code BlockState} properties without third-party dependencies.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner or nested classes.</p>
 *
 * @param materialId canonical material identifier (e.g. "minecraft:stone", "minecraft:oak_log")
 * @param properties state properties (e.g. "axis" -> "y", "waterlogged" -> "false")
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record SimpleBlockData(
        String materialId,
        Map<String, String> properties
) {

    public static final String AIR_ID = "minecraft:air";
    public static final SimpleBlockData AIR = new SimpleBlockData(AIR_ID, Collections.emptyMap());

    public SimpleBlockData {
        Objects.requireNonNull(materialId, "materialId cannot be null");
        Objects.requireNonNull(properties, "properties cannot be null");
    }

    /**
     * Creates a simple block data state with default properties.
     *
     * @param materialId material identifier
     * @return block data snapshot
     */
    public static SimpleBlockData of(String materialId) {
        if (materialId == null || materialId.isEmpty() || AIR_ID.equals(materialId)) {
            return AIR;
        }
        return new SimpleBlockData(materialId, Collections.emptyMap());
    }

    /**
     * Creates a block data state with custom properties.
     *
     * @param materialId material identifier
     * @param properties property map
     * @return block data snapshot
     */
    public static SimpleBlockData of(String materialId, Map<String, String> properties) {
        if (materialId == null || materialId.isEmpty() || AIR_ID.equals(materialId)) {
            return AIR;
        }
        return new SimpleBlockData(materialId, Map.copyOf(properties));
    }
}
