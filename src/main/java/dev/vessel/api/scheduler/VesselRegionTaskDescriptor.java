// ==============================================================================
// VESSEL-API — VesselRegionTaskDescriptor.java
// Version: 1.0.0
// Changelog:
// - 1.0.0: Initial creation of immutable VesselRegionTaskDescriptor contract
//          describing a scheduled regionized, global, or async task in VUSA.
// ==============================================================================

package dev.vessel.api.scheduler;

import java.util.Objects;

/**
 * Immutable descriptor of a task scheduled on the VESSEL regionized/Folia-compatible
 * scheduler or asynchronous worker pool.
 *
 * @version 1.0.0
 */
public final class VesselRegionTaskDescriptor {

    private final long taskId;
    private final String ownerNamespace;
    private final int chunkX;
    private final int chunkZ;
    private final boolean globalRegion;
    private final boolean async;
    private final boolean cancelled;

    /**
     * Constructs a new {@link VesselRegionTaskDescriptor}.
     *
     * @param taskId         monotonic non-negative task identifier (must be {@code >= 0})
     * @param ownerNamespace namespace of the owning mod or plugin (must not be {@code null} or blank)
     * @param chunkX         target chunk X coordinate (used when {@code isChunkRegionTask()} is {@code true})
     * @param chunkZ         target chunk Z coordinate (used when {@code isChunkRegionTask()} is {@code true})
     * @param globalRegion   {@code true} if scheduled on the global region thread
     * @param async          {@code true} if scheduled on the asynchronous worker pool
     * @param cancelled      {@code true} if the task has been cancelled
     * @throws NullPointerException     if {@code ownerNamespace} is {@code null}
     * @throws IllegalArgumentException if {@code taskId < 0} or {@code ownerNamespace} is blank
     */
    public VesselRegionTaskDescriptor(
            long taskId,
            String ownerNamespace,
            int chunkX,
            int chunkZ,
            boolean globalRegion,
            boolean async,
            boolean cancelled) {
        if (taskId < 0L) {
            throw new IllegalArgumentException("taskId must not be negative");
        }
        Objects.requireNonNull(ownerNamespace, "ownerNamespace must not be null");
        if (ownerNamespace.isBlank()) {
            throw new IllegalArgumentException("ownerNamespace must not be blank");
        }
        this.taskId = taskId;
        this.ownerNamespace = ownerNamespace;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.globalRegion = globalRegion;
        this.async = async;
        this.cancelled = cancelled;
    }

    /**
     * Returns the monotonic task identifier.
     *
     * @return non-negative task ID
     */
    public long getTaskId() {
        return this.taskId;
    }

    /**
     * Returns the namespace of the owning mod or plugin.
     *
     * @return owner namespace string
     */
    public String getOwnerNamespace() {
        return this.ownerNamespace;
    }

    /**
     * Returns the target chunk X coordinate associated with this task.
     *
     * @return chunk X coordinate
     */
    public int getChunkX() {
        return this.chunkX;
    }

    /**
     * Returns the target chunk Z coordinate associated with this task.
     *
     * @return chunk Z coordinate
     */
    public int getChunkZ() {
        return this.chunkZ;
    }

    /**
     * Indicates whether this task executes on the global region thread.
     *
     * @return {@code true} if global region task, {@code false} otherwise
     */
    public boolean isGlobalRegion() {
        return this.globalRegion;
    }

    /**
     * Indicates whether this task executes on an asynchronous worker thread.
     *
     * @return {@code true} if async task, {@code false} otherwise
     */
    public boolean isAsync() {
        return this.async;
    }

    /**
     * Indicates whether this task has been marked as cancelled.
     *
     * @return {@code true} if cancelled, {@code false} otherwise
     */
    public boolean isCancelled() {
        return this.cancelled;
    }

    /**
     * Determines whether this task is bound to a specific chunk region partition
     * (neither a global region task nor an asynchronous task).
     *
     * @return {@code true} if {@code !globalRegion && !async}
     */
    public boolean isChunkRegionTask() {
        return !this.globalRegion && !this.async;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof VesselRegionTaskDescriptor other)) {
            return false;
        }
        return this.taskId == other.taskId
                && this.chunkX == other.chunkX
                && this.chunkZ == other.chunkZ
                && this.globalRegion == other.globalRegion
                && this.async == other.async
                && this.cancelled == other.cancelled
                && this.ownerNamespace.equals(other.ownerNamespace);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                this.taskId,
                this.ownerNamespace,
                this.chunkX,
                this.chunkZ,
                this.globalRegion,
                this.async,
                this.cancelled);
    }

    @Override
    public String toString() {
        return "VesselRegionTaskDescriptor{"
                + "taskId=" + this.taskId
                + ", ownerNamespace='" + this.ownerNamespace + '\''
                + ", chunkX=" + this.chunkX
                + ", chunkZ=" + this.chunkZ
                + ", globalRegion=" + this.globalRegion
                + ", async=" + this.async
                + ", cancelled=" + this.cancelled
                + '}';
    }
}
