package marble.core;

/**
 * Small bit-manipulation helpers shared by the id generator.
 */
public final class Bits {

    private Bits() {
    }

    /** Extracts {@code bits} contiguous bits starting at {@code shift}. */
    public static long extract(long value, int bits, long shift) {
        long mask = (bits >= 64) ? ~0L : ((1L << bits) - 1);
        return (value >>> shift) & mask;
    }

    /** Returns a {@code bits}-wide mask of all ones. */
    public static long mask(int bits) {
        return (bits >= 64) ? ~0L : ((1L << bits) - 1);
    }

    /** Returns the number of bits required to represent {@code value} (0 for 0). */
    public static int bitLength(long value) {
        return 64 - Long.numberOfLeadingZeros(value);
    }
}
