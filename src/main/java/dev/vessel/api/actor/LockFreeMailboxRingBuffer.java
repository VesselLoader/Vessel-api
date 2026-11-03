package dev.vessel.api.actor;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Consumer;

/**
 * High-performance lock-free Multi-Producer Single-Consumer (MPSC) Ring Buffer.
 * <p>
 * Enables cross-region actor messaging without lock contention or deadlocks. Multiple
 * producer threads can safely enqueue tasks concurrently using lock-free atomic CAS
 * operations, while the pinned regional strand consumer thread drains tasks in FIFO order.
 * </p>
 *
 * @param <E> element type
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class LockFreeMailboxRingBuffer<E> {

    /**
     * Default buffer capacity (must be a power of two).
     */
    public static final int DEFAULT_CAPACITY = 16384;

    /**
     * Minimum allowed buffer capacity.
     */
    public static final int MIN_CAPACITY = 16;

    private final AtomicReferenceArray<E> buffer;
    private final int capacity;
    private final int mask;
    private final AtomicLong tail;
    private final AtomicLong head;
    private final AtomicLong rejectedCount;
    private final AtomicLong totalEnqueuedCount;
    private final AtomicLong totalDequeuedCount;

    /**
     * Constructs a lock-free ring buffer with the default capacity (16,384 elements).
     */
    public LockFreeMailboxRingBuffer() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * Constructs a lock-free ring buffer with the specified capacity rounded up to the nearest power of two.
     *
     * @param requestedCapacity desired minimum capacity
     */
    public LockFreeMailboxRingBuffer(int requestedCapacity) {
        int cap = Math.max(MIN_CAPACITY, requestedCapacity);
        if ((cap & (cap - 1)) != 0) {
            cap = Integer.highestOneBit(cap) << 1;
        }
        this.capacity = cap;
        this.mask = cap - 1;
        this.buffer = new AtomicReferenceArray<>(cap);
        this.tail = new AtomicLong(0L);
        this.head = new AtomicLong(0L);
        this.rejectedCount = new AtomicLong(0L);
        this.totalEnqueuedCount = new AtomicLong(0L);
        this.totalDequeuedCount = new AtomicLong(0L);
    }

    /**
     * Asynchronously enqueues an element into the ring buffer.
     * <p>
     * Thread-safe for multiple concurrent producer threads.
     * </p>
     *
     * @param element the non-null element to enqueue
     * @return {@code true} if successfully enqueued; {@code false} if the buffer is saturated
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
     * Dequeues the next available element from the ring buffer.
     * <p>
     * Must be invoked exclusively by the single owning regional strand consumer thread.
     * </p>
     *
     * @return the next element, or {@code null} if the buffer is currently empty
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
            // Producer reserved slot but has not published element yet
            return null;
        }

        buffer.set(slot, null);
        head.lazySet(currentHead + 1);
        totalDequeuedCount.incrementAndGet();
        return element;
    }

    /**
     * Drains up to {@code maxBatch} elements and passes each to the provided consumer action.
     *
     * @param action callback to receive dequeued elements
     * @param maxBatch maximum number of elements to drain
     * @return the number of elements successfully drained
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
     * Returns the approximate number of pending elements in the buffer.
     *
     * @return approximate pending size
     */
    public int size() {
        long pending = tail.get() - head.get();
        if (pending < 0) {
            return 0;
        }
        return (int) Math.min(pending, capacity);
    }

    /**
     * Checks if the ring buffer is empty.
     *
     * @return {@code true} if empty
     */
    public boolean isEmpty() {
        return head.get() >= tail.get();
    }

    /**
     * Returns the allocated power-of-two capacity of this ring buffer.
     *
     * @return ring buffer capacity
     */
    public int capacity() {
        return capacity;
    }

    /**
     * Returns the cumulative number of rejected insertions due to buffer saturation.
     *
     * @return count of rejected elements
     */
    public long rejectedCount() {
        return rejectedCount.get();
    }

    /**
     * Returns the cumulative count of successfully enqueued elements.
     *
     * @return count of enqueued elements
     */
    public long totalEnqueuedCount() {
        return totalEnqueuedCount.get();
    }

    /**
     * Returns the cumulative count of successfully dequeued elements.
     *
     * @return count of dequeued elements
     */
    public long totalDequeuedCount() {
        return totalDequeuedCount.get();
    }

    /**
     * Clears all pending elements from the ring buffer.
     */
    public void clear() {
        while (poll() != null) {
            // Drain until empty
        }
    }
}
