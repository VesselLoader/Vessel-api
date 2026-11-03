package dev.vessel.api.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * High-speed parser and serializer for Forge and NeoForge TOML configurations (NightConfig ModConfigSpec dialect).
 *
 * <p>Flattens TOML tables and section headers into canonical dot-separated keys within a {@link VesselConfigurationTree}.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Pass K-32)
 */
public final class NightConfigToVesselParser {

    public static final String TOML_SECTION_START = "[";
    public static final String TOML_SECTION_END = "]";
    public static final String TOML_COMMENT = "#";
    public static final char EQUALS = '=';

    /**
     * Constructs a TOML parser instance.
     */
    public NightConfigToVesselParser() {
    }

    /**
     * Parses raw TOML text into a new flat configuration tree.
     *
     * @param tomlContent raw TOML string
     * @return populated configuration tree
     */
    public VesselConfigurationTree parse(String tomlContent) {
        VesselConfigurationTree tree = new VesselConfigurationTree();
        parseInto(tomlContent, tree);
        return tree;
    }

    /**
     * Parses raw TOML text into a target flat configuration tree.
     *
     * @param tomlContent raw TOML string
     * @param targetTree  destination configuration tree
     */
    public void parseInto(String tomlContent, VesselConfigurationTree targetTree) {
        Objects.requireNonNull(targetTree, "targetTree cannot be null");
        if (tomlContent == null || tomlContent.isBlank()) {
            return;
        }

        String[] lines = tomlContent.split("\r?\n");
        StringBuilder pendingComment = new StringBuilder();
        String currentSection = VesselConfigurationTree.EMPTY_STRING;

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.isEmpty()) {
                continue;
            }

            // Capture comments
            if (trimmed.startsWith(TOML_COMMENT)) {
                if (pendingComment.length() > 0) {
                    pendingComment.append("\n");
                }
                pendingComment.append(trimmed.substring(1).trim());
                continue;
            }

            // Section header: [section.subsection]
            if (trimmed.startsWith(TOML_SECTION_START) && trimmed.endsWith(TOML_SECTION_END)) {
                currentSection = trimmed.substring(1, trimmed.length() - 1).trim();
                continue;
            }

            // Key-value pair: key = value
            int equalsIdx = trimmed.indexOf(EQUALS);
            if (equalsIdx != -1) {
                String key = trimmed.substring(0, equalsIdx).trim();
                String rawVal = trimmed.substring(equalsIdx + 1).trim();

                String fullPath = currentSection.isEmpty()
                        ? key
                        : currentSection + VesselConfigurationTree.PATH_SEPARATOR + key;

                if (pendingComment.length() > 0) {
                    targetTree.setComment(fullPath, pendingComment.toString());
                    pendingComment.setLength(0);
                }

                targetTree.set(fullPath, parseTomlValue(rawVal));
            }
        }
    }

    /**
     * Resolves a raw TOML value string into typed primitives, lists, or unquoted strings.
     *
     * @param raw raw text value
     * @return typed primitive, list, or string
     */
    public Object parseTomlValue(String raw) {
        if (raw == null || raw.isEmpty()) {
            return VesselConfigurationTree.EMPTY_STRING;
        }

        // Inline array: ["item1", "item2"]
        if (raw.startsWith("[") && raw.endsWith("]")) {
            String inside = raw.substring(1, raw.length() - 1).trim();
            if (inside.isEmpty()) {
                return Collections.emptyList();
            }
            String[] tokens = inside.split(",");
            List<Object> list = new ArrayList<>(tokens.length);
            for (String tok : tokens) {
                list.add(parseTomlValue(tok.trim()));
            }
            return Collections.unmodifiableList(list);
        }

        // Quoted string
        if ((raw.startsWith("\"") && raw.endsWith("\"")) || (raw.startsWith("'") && raw.endsWith("'"))) {
            if (raw.length() >= 2) {
                return raw.substring(1, raw.length() - 1);
            }
        }

        // Boolean
        if ("true".equalsIgnoreCase(raw)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(raw)) {
            return Boolean.FALSE;
        }

        // Integer / Long
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            try {
                return Long.parseLong(raw);
            } catch (NumberFormatException ignored2) {
                // Double
                try {
                    return Double.parseDouble(raw);
                } catch (NumberFormatException ignored3) {
                    return raw;
                }
            }
        }
    }

    /**
     * Serializes a flat configuration tree into formatted TOML text grouped by section tables.
     *
     * @param tree flat configuration tree
     * @return formatted TOML text string
     */
    public String toToml(VesselConfigurationTree tree) {
        Objects.requireNonNull(tree, "tree cannot be null");

        // Group keys by section
        Map<String, List<String>> sectionKeys = new LinkedHashMap<>();
        List<String> rootKeys = new ArrayList<>();

        for (String fullPath : new TreeSet<>(tree.getKeys(true))) {
            int lastDot = fullPath.lastIndexOf(VesselConfigurationTree.PATH_SEPARATOR);
            if (lastDot == -1) {
                rootKeys.add(fullPath);
            } else {
                String section = fullPath.substring(0, lastDot);
                sectionKeys.computeIfAbsent(section, k -> new ArrayList<>()).add(fullPath);
            }
        }

        StringBuilder sb = new StringBuilder();

        // Print root-level keys first
        for (String key : rootKeys) {
            appendTomlEntry(sb, tree, key, key);
        }

        // Print sections
        for (Map.Entry<String, List<String>> entry : sectionKeys.entrySet()) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(TOML_SECTION_START).append(entry.getKey()).append(TOML_SECTION_END).append("\n");
            for (String fullPath : entry.getValue()) {
                int lastDot = fullPath.lastIndexOf(VesselConfigurationTree.PATH_SEPARATOR);
                String leafKey = fullPath.substring(lastDot + 1);
                appendTomlEntry(sb, tree, fullPath, leafKey);
            }
        }

        return sb.toString();
    }

    private void appendTomlEntry(StringBuilder sb, VesselConfigurationTree tree, String fullPath, String leafKey) {
        String comment = tree.getComment(fullPath);
        if (comment != null && !comment.isBlank()) {
            for (String cLine : comment.split("\n")) {
                sb.append(TOML_COMMENT).append(" ").append(cLine).append("\n");
            }
        }

        Object val = tree.get(fullPath);
        sb.append(leafKey).append(" = ").append(formatTomlValue(val)).append("\n");
    }

    private String formatTomlValue(Object val) {
        if (val == null) {
            return "\"\"";
        }
        if (val instanceof String str) {
            return "\"" + str.replace("\"", "\\\"") + "\"";
        }
        if (val instanceof List<?> list) {
            StringBuilder arr = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    arr.append(", ");
                }
                arr.append(formatTomlValue(list.get(i)));
            }
            arr.append("]");
            return arr.toString();
        }
        return String.valueOf(val);
    }
}
