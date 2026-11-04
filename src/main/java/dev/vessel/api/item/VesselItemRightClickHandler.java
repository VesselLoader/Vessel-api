package dev.vessel.api.item;

/**
 * Functional callback interface for right-click interactions on custom {@link VesselItem}s.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.1.0
 * @since 1.0.0
 */
@FunctionalInterface
public interface VesselItemRightClickHandler {

    /**
     * Invoked when a player uses or right-clicks with this item in hand.
     *
     * @param playerContext player or entity representation
     * @param worldContext  world reference
     * @param hand          main hand or off hand identifier
     * @return true if action was consumed successfully
     */
    boolean onRightClick(Object playerContext, Object worldContext, String hand);
}
