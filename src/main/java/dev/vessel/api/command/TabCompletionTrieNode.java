package dev.vessel.api.command;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Independent Trie node for high-speed prefix matching in the tab completion cache.
 * <p>
 * Strictly adheres to Rule 17 as an independent top-level class with zero inner,
 * nested, or anonymous classes.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class TabCompletionTrieNode {

    private final Map<Character, TabCompletionTrieNode> children;
    private volatile boolean endOfWord;
    private volatile String fullWord;

    /**
     * Constructs a new empty Trie node.
     */
    public TabCompletionTrieNode() {
        this.children = new ConcurrentHashMap<>();
        this.endOfWord = false;
        this.fullWord = null;
    }

    /**
     * Retrieves an existing child node for the given character.
     *
     * @param character target character
     * @return child node, or {@code null} if none exists
     */
    public TabCompletionTrieNode getChild(char character) {
        return children.get(character);
    }

    /**
     * Retrieves an existing child node or atomically creates a new one.
     *
     * @param character target character
     * @return existing or newly created child node
     */
    public TabCompletionTrieNode getOrCreateChild(char character) {
        return children.computeIfAbsent(character, k -> new TabCompletionTrieNode());
    }

    /**
     * Removes the child node corresponding to the given character.
     *
     * @param character target character
     * @return removed node, or {@code null}
     */
    public TabCompletionTrieNode removeChild(char character) {
        return children.remove(character);
    }

    /**
     * Returns an unmodifiable or direct view of the child nodes map.
     *
     * @return map of character to child node
     */
    public Map<Character, TabCompletionTrieNode> getChildren() {
        return children;
    }

    /**
     * Returns whether this node represents the terminus of a valid candidate word.
     *
     * @return {@code true} if end of word
     */
    public boolean isEndOfWord() {
        return endOfWord;
    }

    /**
     * Sets the terminus flag and stores the canonical word.
     *
     * @param isEnd whether this node is end of word
     * @param word  full string word
     */
    public void setEndOfWord(boolean isEnd, String word) {
        this.endOfWord = isEnd;
        this.fullWord = isEnd ? Objects.requireNonNull(word, "word cannot be null") : null;
    }

    /**
     * Returns the full candidate word stored at this terminus.
     *
     * @return full candidate word, or {@code null}
     */
    public String getFullWord() {
        return fullWord;
    }

    /**
     * Checks if this node has any children.
     *
     * @return {@code true} if children map is empty
     */
    public boolean hasNoChildren() {
        return children.isEmpty();
    }
}
