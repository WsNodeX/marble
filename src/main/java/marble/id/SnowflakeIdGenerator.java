package marble.id;

import marble.core.Bits;
import marble.core.Clock;
import marble.core.Preconditions;

import java.util.function.LongSupplier;

/**
 * A thread-safe, zero-dependency implementation of Twitter's Snowflake id scheme.
 *
 * <p>Each call to {@link #nextId()} returns a 64-bit id that is roughly
 * time-ordered, embeds the node id (so ids from different nodes never collide),
 * and carries a per-millisecond sequence (so a single node can emit up to
 * {@code 2^stepBits} ids per millisecond).</p>
 *
 * <p>The generator is safe to share across threads. When a single millisecond
 * is exhausted it blocks until the next millisecond. If the system clock moves
 * backwards it refuses to issue ids until time catches up, surfacing
 * {@link ClockMovedBackwardsException} &mdash; silently generating duplicate or
 * out-of-order ids would be far worse.</p>
 */
public final class SnowflakeIdGenerator implements IdGenerator {

    private final long epoch;
    private final int nodeId;
    private final int nodeBits;
    private final int stepBits;
    private final long nodeShift;
    private final long timestampShift;
    private final long stepMask;
    private final long nodeMask;
    private final LongSupplier clock;

    private final Object lock = new Object();
    private long lastMillis = -1L;
    private long sequence = 0L;

    public SnowflakeIdGenerator(Snowflake config, int nodeId) {
        this(config, nodeId, Clock.SYSTEM::millis);
    }

    public SnowflakeIdGenerator(Snowflake config, int nodeId, LongSupplier clock) {
        Preconditions.requireNonNull(config, "config");
        Preconditions.requireNonNull(clock, "clock");
        this.epoch = config.epoch();
        this.nodeBits = config.nodeBits();
        this.stepBits = config.stepBits();
        this.nodeShift = stepBits;
        this.timestampShift = stepBits + nodeBits;
        this.stepMask = Bits.mask(stepBits);
        this.nodeMask = Bits.mask(nodeBits);
        Preconditions.checkArgument(nodeId >= 0 && nodeId <= this.nodeMask,
                "nodeId out of range [0, " + this.nodeMask + "]");
        this.nodeId = nodeId;
        this.clock = clock;
    }

    @Override
    public long nextId() {
        synchronized (lock) {
            long now = clock.getAsLong();
            if (now < lastMillis) {
                throw new ClockMovedBackwardsException(lastMillis, now);
            }
            if (now == lastMillis) {
                sequence = (sequence + 1) & stepMask;
                if (sequence == 0) {
                    now = waitNextMillis(lastMillis);
                }
            } else {
                sequence = 0L;
            }
            lastMillis = now;
            long time = now - epoch;
            return (time << timestampShift)
                    | ((long) nodeId << nodeShift)
                    | sequence;
        }
    }

    private long waitNextMillis(long last) {
        long now = clock.getAsLong();
        while (now <= last) {
            now = clock.getAsLong();
        }
        return now;
    }

    /** Decodes an id back into its constituent fields. */
    public Components decode(long id) {
        long seq = Bits.extract(id, stepBits, 0);
        long node = Bits.extract(id, nodeBits, nodeShift);
        long time = Bits.extract(id, (int) (63 - timestampShift), timestampShift);
        return new Components(epoch + time, (int) node, (int) seq);
    }

    /** The structural components of a generated id. */
    public record Components(long timestamp, int nodeId, int sequence) {
    }

    /** Thrown when the clock moved backwards between two {@link #nextId()} calls. */
    public static final class ClockMovedBackwardsException extends RuntimeException {
        public ClockMovedBackwardsException(long last, long now) {
            super("Clock moved backwards: last=" + last + " now=" + now);
        }
    }
}
