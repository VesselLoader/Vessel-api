package dev.vessel.api.event;

import java.util.Arrays;
import java.util.Objects;

/**
 * Flat O(1) open-addressing hash table mapping event class tokens to dense primitive integer IDs.
 * <p>
 * Eliminates heap allocation during event lookups by utilizing primitive arrays, identity hash
 * distribution, and linear probing with power-of-two masking. Concurrent reads are lock-free
 * via volatile array references.
 *
 * @version 1.0.1
 */
public final class DenseEventIndexTable {

    public static final int NOT_FOUND = -1;
    private static final int MINIMUM_CAPACITY = 16;
    private static final int DEFAULT_INITIAL_CAPACITY = 256;
    private static final float LOAD_FACTOR = 0.60f;

    private volatile Class<?>[] tableKeys;
    private volatile int[] tableValues;
    private volatile Class<?>[] idToClassMapping;
    private int elementCount;
    private int nextDenseId;
    private int threshold;

    public DenseEventIndexTable() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    public DenseEventIndexTable(int initialCapacity) {
        int capacity = computePowerOfTwo(Math.max(initialCapacity, MINIMUM_CAPACITY));
        this.tableKeys = new Class<?>[capacity];
        this.tableValues = new int[capacity];
        Arrays.fill(this.tableValues, NOT_FOUND);
        this.idToClassMapping = new Class<?>[capacity];
        this.elementCount = 0;
        this.nextDenseId = 0;
        this.threshold = (int) (capacity * LOAD_FACTOR);
    }

    public int getIndex(Class<?> eventType) {
        if (eventType == null) {
            return NOT_FOUND;
        }
        Class<?>[] keys = this.tableKeys;
        int[] values = this.tableValues;
        int mask = keys.length - 1;
        int index = spreadHash(System.identityHashCode(eventType)) & mask;

        while (true) {
            Class<?> current = keys[index];
            if (current == null) {
                return NOT_FOUND;
            }
            if (current == eventType) {
                return values[index];
            }
            index = (index + 1) & mask;
        }
    }

    public synchronized int getOrRegister(Class<?> eventType) {
        Objects.requireNonNull(eventType, "eventType cannot be null");
        int existing = getIndex(eventType);
        if (existing != NOT_FOUND) {
            return existing;
        }

        if (elementCount >= threshold) {
            resize(tableKeys.length << 1);
        }

        int denseId = nextDenseId++;
        insertEntry(eventType, denseId);
        ensureReverseCapacity(denseId);
        idToClassMapping[denseId] = eventType;
        elementCount++;
        return denseId;
    }

    public Class<?> getEventType(int eventId) {
        Class<?>[] mapping = this.idToClassMapping;
        if (eventId < 0 || eventId >= mapping.length) {
            return null;
        }
        return mapping[eventId];
    }

    public boolean contains(Class<?> eventType) {
        return getIndex(eventType) != NOT_FOUND;
    }

    public int size() {
        return elementCount;
    }

    public int capacity() {
        return tableKeys.length;
    }

    public synchronized void clear() {
        Arrays.fill(this.tableKeys, null);
        Arrays.fill(this.tableValues, NOT_FOUND);
        Arrays.fill(this.idToClassMapping, null);
        this.elementCount = 0;
        this.nextDenseId = 0;
    }

    private void insertEntry(Class<?> eventType, int denseId) {
        Class<?>[] keys = this.tableKeys;
        int[] values = this.tableValues;
        int mask = keys.length - 1;
        int index = spreadHash(System.identityHashCode(eventType)) & mask;

        while (keys[index] != null && keys[index] != eventType) {
            index = (index + 1) & mask;
        }
        keys[index] = eventType;
        values[index] = denseId;
    }

    private void resize(int newCapacity) {
        Class<?>[] oldKeys = this.tableKeys;
        int[] oldValues = this.tableValues;
        Class<?>[] newKeys = new Class<?>[newCapacity];
        int[] newValues = new int[newCapacity];
        Arrays.fill(newValues, NOT_FOUND);

        int mask = newCapacity - 1;
        for (int i = 0; i < oldKeys.length; i++) {
            Class<?> key = oldKeys[i];
            if (key != null) {
                int index = spreadHash(System.identityHashCode(key)) & mask;
                while (newKeys[index] != null) {
                    index = (index + 1) & mask;
                }
                newKeys[index] = key;
                newValues[index] = oldValues[i];
            }
        }

        this.tableKeys = newKeys;
        this.tableValues = newValues;
        this.threshold = (int) (newCapacity * LOAD_FACTOR);
    }

    private void ensureReverseCapacity(int requiredId) {
        if (requiredId >= idToClassMapping.length) {
            int newCap = Math.max(idToClassMapping.length << 1, requiredId + 1);
            idToClassMapping = Arrays.copyOf(idToClassMapping, newCap);
        }
    }

    private static int computePowerOfTwo(int value) {
        int highestOneBit = Integer.highestOneBit(value);
        return value == highestOneBit ? value : highestOneBit << 1;
    }

    private static int spreadHash(int h) {
        return h ^ (h >>> 16);
    }
}
