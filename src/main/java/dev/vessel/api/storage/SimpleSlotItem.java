package dev.vessel.api.storage;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Clean-room immutable item stack representation for cross-loader storage operations.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner or nested classes.</p>
 *
 * @param itemId     canonical identifier of the item (e.g. "minecraft:diamond")
 * @param count      stack count (must be non-negative)
 * @param components metadata components or custom attributes
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record SimpleSlotItem(
        String itemId,
        int count,
        Map<String, Object> components
) {

    public static final String EMPTY_ITEM_ID = "minecraft:air";
    public static final SimpleSlotItem EMPTY = new SimpleSlotItem(EMPTY_ITEM_ID, 0, Collections.emptyMap());

    public SimpleSlotItem {
        Objects.requireNonNull(itemId, "itemId cannot be null");
        Objects.requireNonNull(components, "components cannot be null");
        if (count < 0) {
            throw new IllegalArgumentException("count cannot be negative: " + count);
        }
    }

    /**
     * Creates a simple item stack without extra components.
     *
     * @param itemId canonical item identifier
     * @param count  quantity
     * @return immutable item stack
     */
    public static SimpleSlotItem of(String itemId, int count) {
        if (count <= 0 || EMPTY_ITEM_ID.equals(itemId)) {
            return EMPTY;
        }
        return new SimpleSlotItem(itemId, count, Collections.emptyMap());
    }

    /**
     * Checks if this item stack is empty.
     *
     * @return true if count is 0 or item id is air
     */
    public boolean isEmpty() {
        return count <= 0 || EMPTY_ITEM_ID.equals(itemId);
    }

    /**
     * Creates a copy of this stack with updated count.
     *
     * @param newCount new quantity
     * @return updated stack
     */
    public SimpleSlotItem withCount(int newCount) {
        if (newCount <= 0) {
            return EMPTY;
        }
        return new SimpleSlotItem(itemId, newCount, components);
    }
}
