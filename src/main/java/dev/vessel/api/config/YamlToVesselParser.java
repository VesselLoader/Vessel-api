package dev.vessel.api.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Ultra-lightweight, zero-dependency streaming YAML parser and serializer.
 *
 * <p>Parses YAML files directly into a flat {@link VesselConfigurationTree} without constructing
 * excessive intermediate {@code LinkedHashMap} node graphs, minimizing memory churn.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 * @since 1.0.0 (Pass K-32)
 */
public final class YamlToVesselParser {

    public static final String INDENT_STEP = "  ";
    public static final String COMMENT_PREFIX = "#";
    public static final String LIST_PREFIX = "-";
    public static final char COLON = ':';
    public static final char EQUALS = '=';
    public static final char SINGLE_QUOTE = '\'';
    public static final char DOUBLE_QUOTE = '"';

    /**
     * Constructs a YAML parser instance.
     */
    public YamlToVesselParser() {
    }

    /**
     * Parses raw YAML text into a newly created flat configuration tree.
     *
     * @param content raw YAML string
     * @return populated configuration tree
     */
    public VesselConfigurationTree parse(String content) {
        VesselConfigurationTree tree = new VesselConfigurationTree();
        parseInto(content, tree);
        return tree;
    }

    /**
     * Parses raw YAML text into a destination configuration tree.
     *
     * @param content    raw YAML string
     * @param targetTree destination tree
     */
    public void parseInto(String content, VesselConfigurationTree targetTree) {
        Objects.requireNonNull(targetTree, "targetTree cannot be null");
        if (content == null || content.isBlank()) {
            return;
        }

        String[] lines = content.split("\r?\n");
        StringBuilder pendingComment = new StringBuilder();

        List<String> keyStack = new ArrayList<>();
        List<Integer> indentStack = new ArrayList<>();

        String currentListPath = null;
        List<Object> currentList = null;

        for (int lineNum = 0; lineNum < lines.length; lineNum++) {
            String line = lines[lineNum];
            String trimmed = line.trim();

            if (trimmed.isEmpty()) {
                continue;
            }

            // Capture comments
            if (trimmed.startsWith(COMMENT_PREFIX)) {
                if (pendingComment.length() > 0) {
                    pendingComment.append("\n");
                }
                pendingComment.append(trimmed.substring(1).trim());
                continue;
            }

            // Indentation
            int indent = 0;
            while (indent < line.length() && (line.charAt(indent) == ' ' || line.charAt(indent) == '\t')) {
                indent++;
            }

            // List element handling
            if (trimmed.startsWith(LIST_PREFIX + " ") || trimmed.equals(LIST_PREFIX)) {
                String itemVal = trimmed.substring(1).trim();
                if (currentList != null && currentListPath != null) {
                    currentList.add(parseScalar(itemVal));
                }
                continue;
            }

            // If we were parsing a list, commit it if indentation popped
            if (currentList != null && currentListPath != null) {
                targetTree.set(currentListPath, new ArrayList<>(currentList));
                currentList = null;
                currentListPath = null;
            }

            // Unwind stack for shallower indentation
            while (!indentStack.isEmpty() && indent <= indentStack.get(indentStack.size() - 1)) {
                keyStack.remove(keyStack.size() - 1);
                indentStack.remove(indentStack.size() - 1);
            }

            int colonIdx = trimmed.indexOf(COLON);
            int equalsIdx = trimmed.indexOf(EQUALS);
            int delimiterIdx = (colonIdx != -1) ? colonIdx : equalsIdx;

            if (delimiterIdx != -1) {
                String rawKey = trimmed.substring(0, delimiterIdx).trim();
                String rawVal = trimmed.substring(delimiterIdx + 1).trim();

                keyStack.add(rawKey);
                indentStack.add(indent);

                String currentPath = String.join(VesselConfigurationTree.PATH_SEPARATOR, keyStack);

                if (pendingComment.length() > 0) {
                    targetTree.setComment(currentPath, pendingComment.toString());
                    pendingComment.setLength(0);
                }

                if (rawVal.isEmpty()) {
                    // Check if subsequent line begins a list
                    if (lineNum + 1 < lines.length && lines[lineNum + 1].trim().startsWith(LIST_PREFIX)) {
                        currentListPath = currentPath;
                        currentList = new ArrayList<>();
                    }
                    // Otherwise this is a parent section header
                } else {
                    targetTree.set(currentPath, parseScalar(rawVal));
                }
            }
        }

        // Final list commit if file ends on a list
        if (currentList != null && currentListPath != null) {
            targetTree.set(currentListPath, new ArrayList<>(currentList));
        }
    }

    /**
     * Resolves a scalar string token into typed Java primitives.
     *
     * @param raw raw text token
     * @return typed primitive or string
     */
    public Object parseScalar(String raw) {
        if (raw == null || raw.isEmpty()) {
            return VesselConfigurationTree.EMPTY_STRING;
        }

        // Quoted strings
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
                    // Raw string
                    return raw;
                }
            }
        }
    }

    /**
     * Serializes a flat configuration tree back into formatted YAML text with preserved comments.
     *
     * @param tree source flat configuration tree
     * @return formatted YAML text string
     */
    public String toYaml(VesselConfigurationTree tree) {
        Objects.requireNonNull(tree, "tree cannot be null");

        // Convert flat paths to sorted keys
        Set<String> sortedKeys = new TreeSet<>(tree.getKeys(true));
        StringBuilder sb = new StringBuilder();

        List<String> activePathSegments = new ArrayList<>();

        for (String fullPath : sortedKeys) {
            Object val = tree.get(fullPath);
            String comment = tree.getComment(fullPath);

            String[] segments = fullPath.split("\\.");
            int commonPrefix = 0;
            while (commonPrefix < segments.length - 1 && commonPrefix < activePathSegments.size()
                    && segments[commonPrefix].equals(activePathSegments.get(commonPrefix))) {
                commonPrefix++;
            }

            // Print missing intermediate sections
            for (int s = commonPrefix; s < segments.length - 1; s++) {
                String indent = INDENT_STEP.repeat(s);
                sb.append(indent).append(segments[s]).append(":\n");
            }

            activePathSegments = new ArrayList<>(Arrays.asList(segments).subList(0, segments.length - 1));

            // Print comment if present
            int leafIndentLevel = segments.length - 1;
            String leafIndent = INDENT_STEP.repeat(leafIndentLevel);
            if (comment != null && !comment.isBlank()) {
                for (String cLine : comment.split("\n")) {
                    sb.append(leafIndent).append(COMMENT_PREFIX).append(" ").append(cLine).append("\n");
                }
            }

            String leafKey = segments[segments.length - 1];

            if (val instanceof List<?> list) {
                sb.append(leafIndent).append(leafKey).append(":\n");
                String itemIndent = INDENT_STEP.repeat(leafIndentLevel + 1);
                for (Object item : list) {
                    sb.append(itemIndent).append(LIST_PREFIX).append(" ").append(formatScalar(item)).append("\n");
                }
            } else {
                sb.append(leafIndent).append(leafKey).append(": ").append(formatScalar(val)).append("\n");
            }
        }

        return sb.toString();
    }

    private String formatScalar(Object val) {
        if (val == null) {
            return "null";
        }
        if (val instanceof String str) {
            if (str.contains(":") || str.contains("#") || str.contains(" ") || str.startsWith("-") || str.isEmpty()) {
                return "\"" + str.replace("\"", "\\\"") + "\"";
            }
            return str;
        }
        return String.valueOf(val);
    }
}
