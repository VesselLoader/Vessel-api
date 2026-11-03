package dev.vessel.api.datapack;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Universal registry hub for datapack resources, dynamic tags, and recipe conditions under
 * VUSA Chapter 3 and Wave 3 &amp; 4 specification.
 *
 * <p>Coordinates virtual in-memory datapacks, conventional tag resolutions, and conditional
 * recipe loading across heterogeneous mod ecosystems (Fabric, Forge, NeoForge, Quilt).</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0
 */
public final class VesselDataPackHub {

    private final Map<String, Object> dataPacks = new ConcurrentHashMap<>();
    private final Set<String> activeDataPacks = ConcurrentHashMap.newKeySet();
    private final Map<String, Map<String, Object>> registryTags = new ConcurrentHashMap<>();
    private final Map<String, Predicate<Object>> recipeConditions = new ConcurrentHashMap<>();

    /**
     * Constructs a new universal datapack hub instance.
     */
    public VesselDataPackHub() {
    }

    /**
     * Registers a datapack artifact under the specified unique identifier.
     *
     * @param id   unique namespaced datapack identifier (e.g. "modid:virtual_datapack")
     * @param pack datapack representation instance
     */
    public void registerDataPack(String id, Object pack) {
        Objects.requireNonNull(id, "datapack id cannot be null");
        Objects.requireNonNull(pack, "datapack instance cannot be null");
        this.dataPacks.put(id, pack);
        this.activeDataPacks.add(id);
    }

    /**
     * Checks if the datapack with the given identifier is registered and currently active.
     *
     * @param id datapack identifier
     * @return true if active, false otherwise
     */
    public boolean isDataPackActive(String id) {
        if (id == null) {
            return false;
        }
        return this.activeDataPacks.contains(id);
    }

    /**
     * Activates a previously registered datapack.
     *
     * @param id datapack identifier
     */
    public void activateDataPack(String id) {
        Objects.requireNonNull(id, "datapack id cannot be null");
        if (this.dataPacks.containsKey(id)) {
            this.activeDataPacks.add(id);
        }
    }

    /**
     * Deactivates a datapack without unregistering its resource definitions.
     *
     * @param id datapack identifier
     */
    public void deactivateDataPack(String id) {
        if (id != null) {
            this.activeDataPacks.remove(id);
        }
    }

    /**
     * Retrieves the registered datapack instance by its identifier.
     *
     * @param id datapack identifier
     * @return datapack instance, or null if absent
     */
    public Object getDataPack(String id) {
        if (id == null) {
            return null;
        }
        return this.dataPacks.get(id);
    }

    /**
     * Registers a conventional or modded tag definition under a specific registry type.
     *
     * @param registryType  namespaced registry type (e.g. "minecraft:item", "minecraft:block")
     * @param tagId         namespaced tag identifier (e.g. "c:iron_ingots", "forge:ores/copper")
     * @param tagDefinition tag definition container or collection
     */
    public void registerTag(String registryType, String tagId, Object tagDefinition) {
        Objects.requireNonNull(registryType, "registryType cannot be null");
        Objects.requireNonNull(tagId, "tagId cannot be null");
        Objects.requireNonNull(tagDefinition, "tagDefinition cannot be null");

        Map<String, Object> tags = this.registryTags.computeIfAbsent(
                registryType,
                k -> new ConcurrentHashMap<>()
        );
        tags.put(tagId, tagDefinition);
    }

    /**
     * Retrieves a registered tag definition.
     *
     * @param registryType namespaced registry type
     * @param tagId        namespaced tag identifier
     * @return tag definition, or null if not registered
     */
    public Object getTag(String registryType, String tagId) {
        if (registryType == null || tagId == null) {
            return null;
        }
        Map<String, Object> tags = this.registryTags.get(registryType);
        return tags != null ? tags.get(tagId) : null;
    }

    /**
     * Checks if a tag definition exists for the specified registry.
     *
     * @param registryType namespaced registry type
     * @param tagId        namespaced tag identifier
     * @return true if tag is present
     */
    public boolean hasTag(String registryType, String tagId) {
        return getTag(registryType, tagId) != null;
    }

    /**
     * Registers a recipe condition predicate evaluator.
     *
     * @param conditionId        namespaced condition type (e.g. "forge:mod_loaded", "fabric:all_mods_loaded")
     * @param conditionPredicate predicate evaluating condition context
     */
    public void registerRecipeCondition(String conditionId, Predicate<Object> conditionPredicate) {
        Objects.requireNonNull(conditionId, "conditionId cannot be null");
        Objects.requireNonNull(conditionPredicate, "conditionPredicate cannot be null");
        this.recipeConditions.put(conditionId, conditionPredicate);
    }

    /**
     * Evaluates a recipe condition predicate against a contextual payload.
     *
     * @param conditionId namespaced condition identifier
     * @param context     contextual evaluation parameter
     * @return evaluation result, or false if condition is unregistered
     */
    public boolean testRecipeCondition(String conditionId, Object context) {
        if (conditionId == null) {
            return false;
        }
        Predicate<Object> predicate = this.recipeConditions.get(conditionId);
        return predicate != null && predicate.test(context);
    }

    /**
     * Returns total number of registered datapacks.
     *
     * @return total datapack count
     */
    public int getTotalDataPackCount() {
        return this.dataPacks.size();
    }

    /**
     * Returns count of currently active datapacks.
     *
     * @return active datapack count
     */
    public int getActiveDataPackCount() {
        return this.activeDataPacks.size();
    }

    /**
     * Clears all registered datapacks, tags, and recipe conditions.
     */
    public void clearAll() {
        this.dataPacks.clear();
        this.activeDataPacks.clear();
        this.registryTags.clear();
        this.recipeConditions.clear();
    }
}
