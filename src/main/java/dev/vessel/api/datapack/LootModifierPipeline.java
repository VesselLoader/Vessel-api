package dev.vessel.api.datapack;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Fast-path loot table interceptor pipeline applying Forge GlobalLootModifier and NeoForge
 * loot modifiers directly without ephemeral heap list reallocations under Wave 3 &amp; 4.
 *
 * <p>Operates in-place on existing collection instances to eliminate GC churn during rapid
 * entity mob farms, explosion drops, and mining cascades.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0
 */
public final class LootModifierPipeline {

    private final Map<String, List<Consumer<Object>>> tableModifiers = new ConcurrentHashMap<>();
    private final Map<String, List<BiConsumer<Object, Object>>> contextualModifiers = new ConcurrentHashMap<>();
    private final List<Consumer<Object>> globalModifiers = new CopyOnWriteArrayList<>();
    private final AtomicLong totalModificationsApplied = new AtomicLong(0);

    /**
     * Constructs a new loot modifier pipeline.
     */
    public LootModifierPipeline() {
    }

    /**
     * Registers a table-specific loot modifier callback.
     *
     * @param tableId  namespaced loot table identifier (e.g. "minecraft:chests/simple_dungeon")
     * @param modifier in-place collection consumer
     */
    public void registerModifier(String tableId, Consumer<Object> modifier) {
        Objects.requireNonNull(tableId, "tableId cannot be null");
        Objects.requireNonNull(modifier, "modifier cannot be null");

        List<Consumer<Object>> list = this.tableModifiers.computeIfAbsent(
                tableId,
                k -> new CopyOnWriteArrayList<>()
        );
        list.add(modifier);
    }

    /**
     * Registers a contextual table-specific loot modifier callback.
     *
     * @param tableId  namespaced loot table identifier
     * @param modifier bi-consumer receiving the loot collection and loot context
     */
    public void registerContextualModifier(String tableId, BiConsumer<Object, Object> modifier) {
        Objects.requireNonNull(tableId, "tableId cannot be null");
        Objects.requireNonNull(modifier, "modifier cannot be null");

        List<BiConsumer<Object, Object>> list = this.contextualModifiers.computeIfAbsent(
                tableId,
                k -> new CopyOnWriteArrayList<>()
        );
        list.add(modifier);
    }

    /**
     * Registers a global loot modifier invoked across all loot table rolls.
     *
     * @param globalModifier global in-place modifier consumer
     */
    public void registerGlobalModifier(Consumer<Object> globalModifier) {
        Objects.requireNonNull(globalModifier, "globalModifier cannot be null");
        this.globalModifiers.add(globalModifier);
    }

    /**
     * Fast-path interception applying all matching table-specific and global loot modifiers
     * directly in-place without list reallocations.
     *
     * @param tableId  namespaced loot table identifier
     * @param lootList loot collection to be modified
     * @param context  loot generation context (e.g. LootContext parameter map or entity holder)
     * @return the mutated loot list instance
     */
    public Object applyModifiers(String tableId, Object lootList, Object context) {
        if (lootList == null) {
            return null;
        }

        // Apply table-specific direct modifiers
        if (tableId != null) {
            List<Consumer<Object>> modifiers = this.tableModifiers.get(tableId);
            if (modifiers != null) {
                for (Consumer<Object> modifier : modifiers) {
                    modifier.accept(lootList);
                    this.totalModificationsApplied.incrementAndGet();
                }
            }

            List<BiConsumer<Object, Object>> ctxModifiers = this.contextualModifiers.get(tableId);
            if (ctxModifiers != null) {
                for (BiConsumer<Object, Object> ctxModifier : ctxModifiers) {
                    ctxModifier.accept(lootList, context);
                    this.totalModificationsApplied.incrementAndGet();
                }
            }
        }

        // Apply global modifiers
        for (Consumer<Object> globalModifier : this.globalModifiers) {
            globalModifier.accept(lootList);
            this.totalModificationsApplied.incrementAndGet();
        }

        return lootList;
    }

    /**
     * Returns count of registered modifiers for the specified loot table.
     *
     * @param tableId namespaced loot table identifier
     * @return count of active modifiers
     */
    public int getModifierCount(String tableId) {
        if (tableId == null) {
            return 0;
        }
        int count = 0;
        List<Consumer<Object>> direct = this.tableModifiers.get(tableId);
        if (direct != null) {
            count += direct.size();
        }
        List<BiConsumer<Object, Object>> ctx = this.contextualModifiers.get(tableId);
        if (ctx != null) {
            count += ctx.size();
        }
        return count;
    }

    /**
     * Returns count of registered global loot modifiers.
     *
     * @return global modifier count
     */
    public int getGlobalModifierCount() {
        return this.globalModifiers.size();
    }

    /**
     * Returns cumulative total of loot modifications executed by this pipeline.
     *
     * @return total modifications applied
     */
    public long getTotalModificationsApplied() {
        return this.totalModificationsApplied.get();
    }

    /**
     * Removes all registered modifiers for the specified loot table.
     *
     * @param tableId namespaced loot table identifier
     */
    public void removeModifiersForTable(String tableId) {
        if (tableId != null) {
            this.tableModifiers.remove(tableId);
            this.contextualModifiers.remove(tableId);
        }
    }

    /**
     * Clears all registered table and global loot modifiers.
     */
    public void clear() {
        this.tableModifiers.clear();
        this.contextualModifiers.clear();
        this.globalModifiers.clear();
    }
}
