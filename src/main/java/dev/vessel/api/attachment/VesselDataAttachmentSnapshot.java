// ==============================================================================
// VESSEL-API — VesselDataAttachmentSnapshot.java
// Version: 1.0.0
// Changelog:
// - 1.0.0: Initial creation of immutable VesselDataAttachmentSnapshot record
//          bridging NeoForge DataAttachments, Fabric Data Attachment API, and
//          Bukkit PersistentDataContainer entries in VUSA.
// ==============================================================================

package dev.vessel.api.attachment;

import java.util.Objects;

/**
 * Immutable snapshot of a custom data attachment bound to an entity, chunk, level,
 * or item holder across NeoForge DataAttachments, Fabric Attachment API, and
 * Bukkit {@code PersistentDataContainer}.
 *
 * @param attachmentKey      namespaced attachment identifier (must not be {@code null} or blank)
 * @param valueTypeClassName binary or canonical class name of the attachment value (must not be {@code null} or blank)
 * @param serializedValue    string-serialized representation of the attachment value (must not be {@code null})
 * @param persistent         {@code true} if the attachment is persisted across server restarts
 * @version 1.0.0
 */
public record VesselDataAttachmentSnapshot(
        String attachmentKey,
        String valueTypeClassName,
        String serializedValue,
        boolean persistent) {

    /**
     * Validates all components of {@link VesselDataAttachmentSnapshot}.
     *
     * @throws NullPointerException     if any reference component is {@code null}
     * @throws IllegalArgumentException if {@code attachmentKey} or {@code valueTypeClassName} is blank
     */
    public VesselDataAttachmentSnapshot {
        Objects.requireNonNull(attachmentKey, "attachmentKey must not be null");
        Objects.requireNonNull(valueTypeClassName, "valueTypeClassName must not be null");
        Objects.requireNonNull(serializedValue, "serializedValue must not be null");
        if (attachmentKey.isBlank()) {
            throw new IllegalArgumentException("attachmentKey must not be blank");
        }
        if (valueTypeClassName.isBlank()) {
            throw new IllegalArgumentException("valueTypeClassName must not be blank");
        }
    }
}
