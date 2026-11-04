package dev.vessel.api.block;

/**
 * Immutable specification record defining physical and gameplay characteristics of a custom block.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @param id             canonical namespaced identifier (e.g. {@code "mymod:quantum_generator"})
 * @param hardness       destruction time in seconds
 * @param resistance     blast explosion resistance
 * @param luminance      light emission level (0 to 15)
 * @param soundGroup     sound effect identifier group (e.g. {@code "metal"}, {@code "stone"})
 * @param requiresTool   whether a tool is strictly required to drop items
 * @param tickRate       scheduled tick interval
 * @param dropItemId     namespaced item id dropped when mined
 * @param dropMinCount   minimum items dropped
 * @param dropMaxCount   maximum items dropped
 * @param energyCapacity max energy capacity if block hosts energy capabilities
 * @version 1.1.0
 * @since 1.0.0
 */
public record VesselBlockProperties(
        String id,
        float hardness,
        float resistance,
        int luminance,
        String soundGroup,
        boolean requiresTool,
        int tickRate,
        String dropItemId,
        int dropMinCount,
        int dropMaxCount,
        long energyCapacity
) {
    public static VesselBlockProperties ofDefault(String id) {
        return new VesselBlockProperties(
                id,
                1.5f,
                6.0f,
                0,
                "stone",
                false,
                0,
                id,
                1,
                1,
                0L
        );
    }
}
