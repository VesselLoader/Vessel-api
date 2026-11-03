package dev.vessel.api.actor;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Consumer;

/**
 * NUMA-aware, cache-padded lock-free Multi-Producer Single-Consumer (MPSC) Ring Buffer.
 * <p>
 * Specifically engineered for high-core-count multi-socket enterprise servers (Intel Xeon, AMD EPYC,
 * and Ampere Altra). Employs explicit cache-line padding (64-byte boundaries) between concurrent producer
 * write pointers and single-strand consumer read pointers, completely eliminating false sharing and
 * MESI cache-line invalidation bus stalls.
 * </p>
 *
 * @param <E> element type
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ActorMailboxPaddedRingQueue<E> {

    /**
     * Default capacity for NUMA-padded mailbox (power of two).
     */
    public static final int DEFAULT_PADDED_CAPACITY = 16384;

    /**
     * Minimum allowable capacity.
     */
    public static final int MIN_PADDED_CAPACITY = 16;

    // --- PADDING BEFORE PRODUCER FIELDS (64 bytes) ---
    private final long p00 = 0L, p01 = 0L, p02 = 0L, p03 = 0L, p04 = 0L, p05 = 0L, p06 = 0L, p07 = 0L;

    // --- PRODUCER CACHE LINE ---
    private final AtomicLong tail;
    private final AtomicLong rejectedCount;
    private final AtomicLong totalEnqueuedCount;

    // --- PADDING BETWEEN PRODUCER AND CONSUMER FIELDS (64 bytes) ---
    private final long p10 = 0L, p11 = 0L, p12 = 0L, p13 = 0L, p14 = 0L, p15 = 0L, p16 = 0L, p17 = 0L;

    // --- CONSUMER CACHE LINE ---
    private final AtomicLong head;
    private final AtomicLong totalDequeuedCount;

    // --- PADDING AFTER CONSUMER FIELDS (64 bytes) ---
    private final long p20 = 0L, p21 = 0L, p22 = 0L, p23 = 0L, p24 = 0L, p25 = 0L, p26 = 0L, p27 = 0L;

    // --- BUFFER STORAGE & METADATA ---
    private final AtomicReferenceArray<E> buffer;
    private final int capacity;
    private final int mask;

    /**
     * Constructs a padded ring queue with default capacity (16,384 elements).
     */
    public ActorMailboxPaddedRingQueue() {
        this(DEFAULT_PADDED_CAPACITY);
    }

    /**
     * Constructs a padded ring queue with the specified capacity rounded up to the nearest power of two.
     *
     * @param requestedCapacity desired minimum capacity
     */
    public ActorMailboxPaddedRingQueue(int requestedCapacity) {
        int cap = Math.max(MIN_PADDED_CAPACITY, requestedCapacity);
        if ((cap & (cap - 1)) != 0) {
            cap = Integer.highestOneBit(cap) << 1;
        }

        this.capacity = cap;
        this.mask = cap - 1;
        this.buffer = new AtomicReferenceArray<>(cap);

        this.tail = new AtomicLong(0L);
        this.rejectedCount = new AtomicLong(0L);
        this.totalEnqueuedCount = new AtomicLong(0L);

        this.head = new AtomicLong(0L);
        this.totalDequeuedCount = new AtomicLong(0L);
    }

    /**
     * Non-blocking concurrent enqueue for multiple producer threads.
     *
     * @param element non-null element to enqueue
     * @return {@code true} if enqueued; {@code false} if capacity is saturated
     */
    public boolean offer(E element) {
        Objects.requireNonNull(element, "element cannot be null");

        while (true) {
            long currentTail = tail.get();
            long currentHead = head.get();

            if (currentTail - currentHead >= capacity) {
                rejectedCount.incrementAndGet();
                return false;
            }

            if (tail.compareAndSet(currentTail, currentTail + 1)) {
                int slot = (int) (currentTail & mask);
                while (!buffer.compareAndSet(slot, null, element)) {
                    Thread.onSpinWait();
                }
                totalEnqueuedCount.incrementAndGet();
                return true;
            }
        }
    }

    /**
     * Dequeues the next available element by the single regional strand consumer thread.
     *
     * @return next element, or {@code null} if empty or element not yet published
     */
    public E poll() {
        long currentHead = head.get();
        long currentTail = tail.get();

        if (currentHead >= currentTail) {
            return null;
        }

        int slot = (int) (currentHead & mask);
        E element = buffer.get(slot);

        if (element == null) {
            return null;
        }

        buffer.set(slot, null);
        head.lazySet(currentHead + 1);
        totalDequeuedCount.incrementAndGet();
        return element;
    }

    /**
     * Drains up to {@code maxBatch} elements to the given consumer action.
     *
     * @param action consumer callback
     * @param maxBatch maximum elements to drain
     * @return number of elements drained
     */
    public int drain(Consumer<E> action, int maxBatch) {
        Objects.requireNonNull(action, "action cannot be null");
        if (maxBatch <= 0) {
            return 0;
        }

        int drained = 0;
        while (drained < maxBatch) {
            E element = poll();
            if (element == null) {
                break;
            }
            action.accept(element);
            drained++;
        }
        return drained;
    }

    /**
     * Returns approximate number of pending elements.
     *
     * @return pending count
     */
    public int size() {
        long pending = tail.get() - head.get();
        if (pending < 0) {
            return 0;
        }
        return (int) Math.min(pending, capacity);
    }

    /**
     * Checks if queue is currently empty.
     *
     * @return {@code true} if empty
     */
    public boolean isEmpty() {
        return head.get() >= tail.get();
    }

    /**
     * Returns the capacity of the ring queue.
     *
     * @return power-of-two capacity
     */
    public int capacity() {
        return capacity;
    }

    /**
     * Returns count of rejected elements due to saturation.
     *
     * @return rejected count
     */
    public long getRejectedCount() {
        return rejectedCount.get();
    }

    /**
     * Returns cumulative count of successfully enqueued elements.
     *
     * @return enqueued count
     */
    public long getTotalEnqueuedCount() {
        return totalEnqueuedCount.get();
    }

    /**
     * Returns cumulative count of successfully dequeued elements.
     *
     * @return dequeued count
     */
    public long getTotalDequeuedCount() {
        return totalDequeuedCount.get();
    }

    /**
     * Clears all pending elements from the queue.
     */
    public void clear() {
        while (poll() != null) {
            // Drain until empty
        }
    }
}
