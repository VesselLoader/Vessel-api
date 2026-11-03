package dev.vessel.api.block;

import java.util.Map;
import java.util.Objects;

/**
 * Immutable VUSA snapshot representing a block state with key-value property taxonomy
 * ({@code dev.vessel.api.block.VesselBlockStatePropertySnapshot}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param blockId    namespaced block identifier
 * @param properties map of block state property names and string values
 * @param solid      whether the block is solid
 * @param air        whether the block is air
 * @version 1.0.0
 */
public record VesselBlockStatePropertySnapshot(
        String blockId,
        Map<String, String> properties,
        boolean solid,
        boolean air) {

    public VesselBlockStatePropertySnapshot {
        Objects.requireNonNull(blockId, "blockId must not be null");
        if (blockId.isBlank()) {
            throw new IllegalArgumentException("blockId must not be blank");
        }
        properties = properties == null ? Map.of() : Map.copyOf(properties);
    }

    public boolean hasProperty(String name) {
        return name != null && properties.containsKey(name);
    }

    public String getProperty(String name) {
        return name != null ? properties.get(name) : null;
    }
}
