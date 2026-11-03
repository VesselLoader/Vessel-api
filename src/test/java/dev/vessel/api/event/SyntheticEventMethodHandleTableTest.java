package dev.vessel.api.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verification test suite for SyntheticEventMethodHandleTable.
 * Strictly adheres to Rule 17 with zero inner/nested classes.
 *
 * @version 1.0.1
 */
public final class SyntheticEventMethodHandleTableTest {

    private SyntheticEventMethodHandleTable table;
    private TestSpeedListener listenerA;
    private TestSpeedListener listenerB;

    @BeforeEach
    public void setUp() {
        this.table = new SyntheticEventMethodHandleTable(16);
        this.listenerA = new TestSpeedListener();
        this.listenerB = new TestSpeedListener();
    }

    @Test
    @DisplayName("Monomorphic registration and direct O(1) hardware dispatch")
    public void testMonomorphicDispatch() throws Throwable {
        MethodHandle handle = MethodHandles.lookup().findVirtual(
            TestSpeedListener.class,
            "onSpeedEvent",
            MethodType.methodType(void.class, TestSpeedEvent.class)
        ).bindTo(listenerA);

        table.setHandle(0, handle);

        assertThat(table.size()).isEqualTo(1);
        assertThat(table.getHandle(0)).isNotNull();

        TestSpeedEvent event = new TestSpeedEvent(100L, 0L);
        table.invoke(0, event);

        assertThat(listenerA.getReceivedEvents()).isEqualTo(1L);
        assertThat(event.getSequence()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Direct MethodHandle replacement and invocation")
    public void testMethodHandleReplacement() throws Throwable {
        MethodHandle handleA = MethodHandles.lookup().findVirtual(
            TestSpeedListener.class,
            "onSpeedEvent",
            MethodType.methodType(void.class, TestSpeedEvent.class)
        ).bindTo(listenerA);

        MethodHandle handleB = MethodHandles.lookup().findVirtual(
            TestSpeedListener.class,
            "onSpeedEvent",
            MethodType.methodType(void.class, TestSpeedEvent.class)
        ).bindTo(listenerB);

        table.setHandle(5, handleA);
        assertThat(table.size()).isEqualTo(1);
        assertThat(table.getHandle(5)).isEqualTo(handleA);

        table.setHandle(5, handleB);
        assertThat(table.size()).isEqualTo(1);
        assertThat(table.getHandle(5)).isEqualTo(handleB);

        TestSpeedEvent event = new TestSpeedEvent(200L, 0L);
        table.invoke(5, event);

        assertThat(listenerA.getReceivedEvents()).isEqualTo(0L);
        assertThat(listenerB.getReceivedEvents()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Clearing handle table resets elements and returns null on lookup")
    public void testClearTable() throws Throwable {
        MethodHandle handle = MethodHandles.lookup().findVirtual(
            TestSpeedListener.class,
            "onSpeedEvent",
            MethodType.methodType(void.class, TestSpeedEvent.class)
        ).bindTo(listenerA);

        table.setHandle(3, handle);
        assertThat(table.size()).isEqualTo(1);
        assertThat(table.getHandle(3)).isNotNull();

        table.clear();
        assertThat(table.size()).isEqualTo(0);
        assertThat(table.getHandle(3)).isNull();

        TestSpeedEvent event = new TestSpeedEvent(300L, 0L);
        table.invoke(3, event);
        assertThat(listenerA.getReceivedEvents()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Automatic capacity growth handles large dense IDs")
    public void testCapacityExpansion() throws Throwable {
        MethodHandle handle = MethodHandles.lookup().findVirtual(
            TestSpeedListener.class,
            "onSpeedEvent",
            MethodType.methodType(void.class, TestSpeedEvent.class)
        ).bindTo(listenerA);

        int largeId = 500;
        table.setHandle(largeId, handle);

        assertThat(table.capacity()).isGreaterThanOrEqualTo(501);
        assertThat(table.size()).isEqualTo(1);
        assertThat(table.getHandle(largeId)).isNotNull();

        TestSpeedEvent event = new TestSpeedEvent(400L, 0L);
        table.invoke(largeId, event);

        assertThat(listenerA.getReceivedEvents()).isEqualTo(1L);
    }
}
