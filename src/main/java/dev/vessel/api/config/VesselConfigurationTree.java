package dev.vessel.api.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance flat table hierarchical configuration representation providing O(1) lookup
 * across arbitrary path depths without deep nested object allocations.
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
public final class VesselConfigurationTree {

    public static final String PATH_SEPARATOR = ".";
    public static final String EMPTY_STRING = "";
    public static final int DEFAULT_CAPACITY = 128;

    private final Map<String, Object> values;
    private final Map<String, String> comments;

    /**
     * Constructs an empty configuration tree with default initial capacity.
     */
    public VesselConfigurationTree() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * Constructs an empty configuration tree with specified initial capacity.
     *
     * @param initialCapacity initial flat map capacity
     */
    public VesselConfigurationTree(int initialCapacity) {
        this.values = new ConcurrentHashMap<>(initialCapacity);
        this.comments = new ConcurrentHashMap<>(initialCapacity);
    }

    /**
     * Associates a value with a dot-separated canonical path.
     *
     * @param path  canonical dot-separated path (e.g., "server.port")
     * @param value value object to store (null removes the path)
     */
    public void set(String path, Object value) {
        Objects.requireNonNull(path, "path cannot be null");
        if (value == null) {
            values.remove(path);
            comments.remove(path);
        } else {
            values.put(path, value);
        }
    }

    /**
     * Associates a comment with a specific path.
     *
     * @param path    canonical path
     * @param comment comment string
     */
    public void setComment(String path, String comment) {
        Objects.requireNonNull(path, "path cannot be null");
        if (comment == null || comment.isBlank()) {
            comments.remove(path);
        } else {
            comments.put(path, comment.trim());
        }
    }

    /**
     * Retrieves the comment associated with a path if any.
     *
     * @param path canonical path
     * @return comment string or null
     */
    public String getComment(String path) {
        Objects.requireNonNull(path, "path cannot be null");
        return comments.get(path);
    }

    /**
     * Checks if a path is defined in this configuration tree.
     *
     * @param path canonical path
     * @return true if path exists
     */
    public boolean contains(String path) {
        Objects.requireNonNull(path, "path cannot be null");
        return values.containsKey(path);
    }

    /**
     * Resolves raw object value at path.
     *
     * @param path canonical path
     * @return object value or null
     */
    public Object get(String path) {
        Objects.requireNonNull(path, "path cannot be null");
        return values.get(path);
    }

    /**
     * Resolves raw object value at path with a default fallback.
     *
     * @param path canonical path
     * @param def  default fallback value
     * @return object value or def
     */
    public Object get(String path, Object def) {
        Object val = get(path);
        return val != null ? val : def;
    }

    /**
     * Resolves a string value at path.
     *
     * @param path canonical path
     * @return string value or null
     */
    public String getString(String path) {
        return getString(path, null);
    }

    /**
     * Resolves a string value at path with a default fallback.
     *
     * @param path canonical path
     * @param def  default fallback
     * @return string value or def
     */
    public String getString(String path, String def) {
        Object val = get(path);
        if (val == null) {
            return def;
        }
        return String.valueOf(val);
    }

    /**
     * Resolves an integer value at path with default fallback 0.
     *
     * @param path canonical path
     * @return int value or 0
     */
    public int getInt(String path) {
        return getInt(path, 0);
    }

    /**
     * Resolves an integer value at path with a specified default fallback.
     *
     * @param path canonical path
     * @param def  default fallback
     * @return int value or def
     */
    public int getInt(String path, int def) {
        Object val = get(path);
        if (val instanceof Number num) {
            return num.intValue();
        }
        if (val instanceof String str) {
            try {
                return Integer.parseInt(str.trim());
            } catch (NumberFormatException ignored) {
                return def;
            }
        }
        return def;
    }

    /**
     * Resolves a long value at path with default fallback 0L.
     *
     * @param path canonical path
     * @return long value or 0L
     */
    public long getLong(String path) {
        return getLong(path, 0L);
    }

    /**
     * Resolves a long value at path with a specified default fallback.
     *
     * @param path canonical path
     * @param def  default fallback
     * @return long value or def
     */
    public long getLong(String path, long def) {
        Object val = get(path);
        if (val instanceof Number num) {
            return num.longValue();
        }
        if (val instanceof String str) {
            try {
                return Long.parseLong(str.trim());
            } catch (NumberFormatException ignored) {
                return def;
            }
        }
        return def;
    }

    /**
     * Resolves a boolean value at path with default fallback false.
     *
     * @param path canonical path
     * @return boolean value or false
     */
    public boolean getBoolean(String path) {
        return getBoolean(path, false);
    }

    /**
     * Resolves a boolean value at path with a specified default fallback.
     *
     * @param path canonical path
     * @param def  default fallback
     * @return boolean value or def
     */
    public boolean getBoolean(String path, boolean def) {
        Object val = get(path);
        if (val instanceof Boolean b) {
            return b;
        }
        if (val instanceof String str) {
            return Boolean.parseBoolean(str.trim());
        }
        return def;
    }

    /**
     * Resolves a double precision float at path with default fallback 0.0.
     *
     * @param path canonical path
     * @return double value or 0.0
     */
    public double getDouble(String path) {
        return getDouble(path, 0.0);
    }

    /**
     * Resolves a double precision float at path with a specified default fallback.
     *
     * @param path canonical path
     * @param def  default fallback
     * @return double value or def
     */
    public double getDouble(String path, double def) {
        Object val = get(path);
        if (val instanceof Number num) {
            return num.doubleValue();
        }
        if (val instanceof String str) {
            try {
                return Double.parseDouble(str.trim());
            } catch (NumberFormatException ignored) {
                return def;
            }
        }
        return def;
    }

    /**
     * Resolves a generic list value at path.
     *
     * @param path canonical path
     * @return List instance or empty list
     */
    @SuppressWarnings("unchecked")
    public List<?> getList(String path) {
        Object val = get(path);
        if (val instanceof List<?> list) {
            return list;
        }
        return Collections.emptyList();
    }

    /**
     * Resolves a string list value at path.
     *
     * @param path canonical path
     * @return List of strings or empty list
     */
    public List<String> getStringList(String path) {
        List<?> rawList = getList(path);
        if (rawList.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>(rawList.size());
        for (Object item : rawList) {
            result.add(String.valueOf(item));
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Resolves all configuration keys.
     *
     * @param deep if true, returns full dot-separated paths; if false, returns top-level root segments only
     * @return unmodifiable set of keys
     */
    public Set<String> getKeys(boolean deep) {
        if (deep) {
            return Collections.unmodifiableSet(values.keySet());
        }

        Set<String> topLevel = new LinkedHashSet<>();
        for (String key : values.keySet()) {
            int dotIdx = key.indexOf(PATH_SEPARATOR);
            if (dotIdx != -1) {
                topLevel.add(key.substring(0, dotIdx));
            } else {
                topLevel.add(key);
            }
        }
        return Collections.unmodifiableSet(topLevel);
    }

    /**
     * Resolves direct child keys under a specific section prefix.
     *
     * @param prefix section path (e.g., "server")
     * @return set of immediate child keys (e.g., "port", "ip")
     */
    public Set<String> getSectionChildKeys(String prefix) {
        Objects.requireNonNull(prefix, "prefix cannot be null");
        String searchPrefix = prefix.isEmpty() ? EMPTY_STRING : prefix + PATH_SEPARATOR;
        int prefixLen = searchPrefix.length();

        Set<String> children = new LinkedHashSet<>();
        for (String key : values.keySet()) {
            if (key.startsWith(searchPrefix)) {
                String sub = key.substring(prefixLen);
                int dotIdx = sub.indexOf(PATH_SEPARATOR);
                if (dotIdx != -1) {
                    children.add(sub.substring(0, dotIdx));
                } else {
                    children.add(sub);
                }
            }
        }
        return Collections.unmodifiableSet(children);
    }

    /**
     * Resolves an unmodifiable view of all flattened path-value pairs.
     *
     * @return map of path -> value
     */
    public Map<String, Object> getValues() {
        return Collections.unmodifiableMap(values);
    }

    /**
     * Total number of flat keys registered in this tree.
     *
     * @return key count
     */
    public int size() {
        return values.size();
    }

    /**
     * Clears all values and comments from this tree.
     */
    public void clear() {
        values.clear();
        comments.clear();
    }
}
