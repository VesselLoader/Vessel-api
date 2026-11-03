package dev.vessel.api.event;

/**
 * Foundational public contract implemented by events that support execution cancellation.
 * <p>
 * Unifies Bukkit {@code Cancellable}, Forge/NeoForge {@code ICancellableEvent}, and VESSEL
 * Kernel {@code dev.vessel.kernel.event.VesselCancellable} across the API/Core boundary.
 *
 * @version 1.0.0
 */
public interface VesselCancellable {

    /**
     * Checks if this event has been cancelled by an earlier listener.
     *
     * @return true if cancelled
     */
    boolean isCancelled();

    /**
     * Sets the cancellation status of this event.
     *
     * @param cancel true to cancel the event
     */
    void setCancelled(boolean cancel);
}
