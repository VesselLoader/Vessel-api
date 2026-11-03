package dev.vessel.api.config;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Concrete holder representing a Forge/NeoForge ConfigValue instance.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @param <T> value data type
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
public final class ForgeConfigValueHolder<T> implements Supplier<T> {

    private final List<String> path;
    private final String dotPath;
    private final T defaultValue;
    private final String comment;
    private final VesselConfigurationTree tree;
    private T cachedValue;

    /**
     * Constructs a ForgeConfigValueHolder bound to a backing configuration tree.
     *
     * @param path         path hierarchy segment list
     * @param defaultValue default value
     * @param comment      associated description comment
     * @param tree         backing configuration tree
     */
    public ForgeConfigValueHolder(List<String> path, T defaultValue, String comment, VesselConfigurationTree tree) {
        this.path = Collections.unmodifiableList(Objects.requireNonNull(path, "path cannot be null"));
        this.dotPath = String.join(VesselConfigurationTree.PATH_SEPARATOR, path);
        this.defaultValue = defaultValue;
        this.comment = comment;
        this.tree = Objects.requireNonNull(tree, "tree cannot be null");
        this.cachedValue = defaultValue;

        if (comment != null && !comment.isBlank()) {
            tree.setComment(dotPath, comment);
        }
        if (defaultValue != null && !tree.contains(dotPath)) {
            tree.set(dotPath, defaultValue);
        }
    }

    /**
     * Retrieves the current resolved config value.
     *
     * @return current value or default
     */
    @Override
    @SuppressWarnings("unchecked")
    public T get() {
        Object val = tree.get(dotPath);
        if (val == null) {
            return defaultValue;
        }
        if (defaultValue != null && defaultValue.getClass().isInstance(val)) {
            return (T) val;
        }
        // Fallback or string conversion
        return (T) val;
    }

    /**
     * Updates the config value and reflects it in the backing tree.
     *
     * @param value new value
     */
    public void set(T value) {
        this.cachedValue = value;
        tree.set(dotPath, value);
    }

    /**
     * Resolves the hierarchical path segments list.
     *
     * @return unmodifiable path list
     */
    public List<String> getPath() {
        return path;
    }

    /**
     * Resolves the canonical dot-separated path string.
     *
     * @return dot path
     */
    public String getDotPath() {
        return dotPath;
    }

    /**
     * Resolves the default value.
     *
     * @return default value
     */
    public T getDefault() {
        return defaultValue;
    }

    /**
     * Resolves the comment description.
     *
     * @return comment string
     */
    public String getComment() {
        return comment;
    }
}
