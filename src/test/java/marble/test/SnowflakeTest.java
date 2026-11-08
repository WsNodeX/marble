package marble.test;

import marble.core.Clock;
import marble.id.Snowflake;
import marble.id.SnowflakeIdGenerator;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.LongSupplier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnowflakeTest {

    /** Supplies queued timestamps, then keeps returning the last one. */
    private static LongSupplier queueClock(long... values) {
        Deque<Long> q = new ArrayDeque<>();
        for (long v : values) {
            q.addLast(v);
        }
        return () -> q.isEmpty() ? (q.peekLast() == null ? 0L : q.peekLast()) : q.pollFirst();
    }

    @Test
    void configRejectsBadLayouts() {
        assertThrows(IllegalArgumentException.class, () -> new Snowflake(1, 23, 0));
        assertThrows(IllegalArgumentException.class, () -> new Snowflake(1, -1, 4));
        assertThrows(IllegalArgumentException.class, () -> new Snowflake(0, 4, 4));
    }

    @Test
    void maxNodeAndSequence() {
        Snowflake s = new Snowflake(1, 10, 12);
        assertEquals(1023, s.maxNodeId());
        assertEquals(4095, s.maxSequence());
    }

    @Test
    void nodeIdOutOfRangeRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new SnowflakeIdGenerator(new Snowflake(1, 2, 2), 4));
    }

    @Test
    void idsAreStrictlyIncreasing() {
        SnowflakeIdGenerator gen = new SnowflakeIdGenerator(Snowflake.classic(), 1);
        long prev = -1;
        for (int i = 0; i < 200; i++) {
            long id = gen.nextId();
            assertTrue(id > prev, "ids must increase");
            prev = id;
        }
    }

    @Test
    void decodeRecoversNodeAndSequence() {
        // epoch=1, nodeBits=0, stepBits=2 -> maxSequence=3
        Snowflake config = new Snowflake(1, 0, 2);
        SnowflakeIdGenerator gen = new SnowflakeIdGenerator(config, 0, queueClock(1000, 1000, 1000, 1000, 1000, 1001));
        SnowflakeIdGenerator.Components[] parts = new SnowflakeIdGenerator.Components[5];
        for (int i = 0; i < 5; i++) {
            parts[i] = gen.decode(gen.nextId());
        }
        assertEquals(0, parts[0].nodeId());
        assertEquals(0, parts[0].sequence());
        assertEquals(1, parts[1].sequence());
        assertEquals(2, parts[2].sequence());
        assertEquals(3, parts[3].sequence());
        // 5th call overflowed the millisecond and waited for the next one
        assertEquals(1000 + 1, parts[4].timestamp());
        assertEquals(0, parts[4].sequence());
    }

    @Test
    void clockBackwardsThrows() {
        long[] ticks = {2000, 1999};
        int[] i = {0};
        LongSupplier clock = () -> ticks[i[0]++];
        SnowflakeIdGenerator gen = new SnowflakeIdGenerator(Snowflake.classic(), 0, clock);
        assertDoesNotThrow(gen::nextId);
        assertThrows(SnowflakeIdGenerator.ClockMovedBackwardsException.class, gen::nextId);
    }

    @Test
    void differentNodesNeverCollide() {
        SnowflakeIdGenerator a = new SnowflakeIdGenerator(Snowflake.classic(), 1);
        SnowflakeIdGenerator b = new SnowflakeIdGenerator(Snowflake.classic(), 2);
        long idA = a.nextId();
        long idB = b.nextId();
        assertNotEquals(a.decode(idA).nodeId(), b.decode(idB).nodeId());
    }
}
