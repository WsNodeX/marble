package marble.id;

import marble.core.Preconditions;

/**
 * Configuration for a {@link SnowflakeIdGenerator}.
 *
 * <p>A snowflake id is a 64-bit integer split into three fields:</p>
 * <pre>
 *  1 bit sign | 41 bits timestamp | N bits node | M bits sequence
 * </pre>
 * where {@code N + M} must not exceed 22 so the whole value fits in a signed
 * {@code long} together with 41 bits of timestamp and the sign bit.</p>
 *
 * @param epoch     the custom epoch in milliseconds; ids encode time as
 *                  {@code (now - epoch)} so the 41-bit field lasts ~69 years
 * @param nodeBits  number of bits reserved for the node id
 * @param stepBits  number of bits reserved for the in-millisecond sequence
 */
public record Snowflake(long epoch, int nodeBits, int stepBits) {

    /** A sensible default epoch (2023-11-14T22:13:20Z). */
    public static final long DEFAULT_EPOCH = 1_700_000_000_000L;

    /** Classic layout: 10 node bits, 12 sequence bits. */
    public static Snowflake classic() {
        return new Snowflake(DEFAULT_EPOCH, 10, 12);
    }

    public Snowflake {
        Preconditions.checkArgument(epoch > 0, "epoch must be positive");
        Preconditions.checkArgument(nodeBits >= 0, "nodeBits must be >= 0");
        Preconditions.checkArgument(stepBits >= 0, "stepBits must be >= 0");
        Preconditions.checkArgument(nodeBits + stepBits <= 22,
                "nodeBits + stepBits must be <= 22 (got " + (nodeBits + stepBits) + ")");
    }

    /** Maximum representable node id for this layout. */
    public int maxNodeId() {
        return (1 << nodeBits) - 1;
    }

    /** Maximum in-millisecond sequence for this layout. */
    public int maxSequence() {
        return (1 << stepBits) - 1;
    }
}
