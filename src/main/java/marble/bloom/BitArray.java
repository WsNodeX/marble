package marble.bloom;

import java.util.Arrays;

/**
 * A fixed-size bit vector backed by a {@link java.util.BitSet}.
 *
 * <p>Indices are taken modulo the size so callers never have to worry about
 * wrapping. The array can be serialised to a byte array and rebuilt, which is
 * what lets a {@link BloomFilter} be persisted or merged.</p>
 */
public final class BitArray {

    private final java.util.BitSet bits;
    private final int size;

    public BitArray(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive");
        }
        this.size = size;
        this.bits = new java.util.BitSet(size);
    }

    private BitArray(java.util.BitSet bits, int size) {
        this.bits = bits;
        this.size = size;
    }

    public int size() {
        return size;
    }

    public void set(int index) {
        bits.set(Math.floorMod(index, size));
    }

    public boolean get(int index) {
        return bits.get(Math.floorMod(index, size));
    }

    /** Logical OR: copies every set bit from {@code other} into this array. */
    public void or(BitArray other) {
        if (other.size != this.size) {
            throw new IllegalArgumentException("bit array sizes differ");
        }
        bits.or(other.bits);
    }

    public byte[] toBytes() {
        return bits.toByteArray();
    }

    public static BitArray fromBytes(byte[] bytes, int size) {
        java.util.BitSet restored = java.util.BitSet.valueOf(bytes);
        return new BitArray(restored, size);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BitArray)) {
            return false;
        }
        BitArray that = (BitArray) o;
        if (this.size != that.size) {
            return false;
        }
        // Compare only the in-range portion.
        for (int i = 0; i < size; i++) {
            if (this.bits.get(i) != that.bits.get(i)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int h = size;
        for (int i = 0; i < size; i++) {
            if (bits.get(i)) {
                h = 31 * h + i;
            }
        }
        return h;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            sb.append(bits.get(i) ? '1' : '0');
        }
        return sb.toString();
    }
}
