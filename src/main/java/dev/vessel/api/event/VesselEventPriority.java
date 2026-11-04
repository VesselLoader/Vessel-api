package dev.vessel.api.event;

/**
 * Execution priority enumeration for event subscribers in the VESSEL Super-API ecosystem.
 *
 * <p>Modeled after Bukkit/Paper {@code EventPriority} and Forge {@code EventPriority}.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.1.0
 * @since 1.0.0
 */
public enum VesselEventPriority {

    /**
     * First listener to receive the event, used for setup.
     */
    LOWEST(0),

    /**
     * Early event processing.
     */
    LOW(1),

    /**
     * Default event processing priority.
     */
    NORMAL(2),

    /**
     * Late event processing.
     */
    HIGH(3),

    /**
     * Last event processing before outcome is fixed.
     */
    HIGHEST(4),

    /**
     * Read-only monitor pass, should never modify or cancel the event.
     */
    MONITOR(5);

    private final int slotIndex;

    VesselEventPriority(int slotIndex) {
        this.slotIndex = slotIndex;
    }

    public int getSlotIndex() {
        return slotIndex;
    }
}
