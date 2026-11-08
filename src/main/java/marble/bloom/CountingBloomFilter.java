package marble.bloom;

import marble.core.Hash;
import marble.core.Preconditions;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * A counting Bloom filter: a Bloom filter whose bits are replaced by small
 * counters so that elements can be removed.
 *
 * <p>Standard Bloom filters are write-once. A counting variant stores an
 * integer per slot; {@link #put} increments the relevant counters and
 * {@link #remove} decrements them, so membership tests stay correct after
 * deletions. Counters saturate rather than overflow.</p>
 */
public final class CountingBloomFilter {

    private final int[] counters;
    private final int bitSize;
    private final int numHashes;

    public CountingBloomFilter(int expectedInsertions, double falsePositiveRate) {
        Preconditions.checkArgument(expectedInsertions > 0, "expectedInsertions must be > 0");
        Preconditions.checkArgument(falsePositiveRate > 0 && falsePositiveRate < 1,
                "falsePositiveRate must be in (0, 1)");
        this.bitSize = BloomFilter.optimalBitSize(expectedInsertions, falsePositiveRate);
        this.numHashes = BloomFilter.optimalNumHashes(expectedInsertions, bitSize);
        this.counters = new int[bitSize];
    }

    private CountingBloomFilter(int[] counters, int numHashes) {
        this.counters = counters;
        this.bitSize = counters.length;
        this.numHashes = numHashes;
    }

    public void put(byte[] data) {
        for (int h : Hash.doubleHashes(data, numHashes)) {
            int idx = Math.floorMod(h, bitSize);
            if (counters[idx] < Integer.MAX_VALUE) {
                counters[idx]++;
            }
        }
    }

    public void put(String value) {
        put(value.getBytes(StandardCharsets.UTF_8));
    }

    public void remove(byte[] data) {
        if (!mightContain(data)) {
            return;
        }
        for (int h : Hash.doubleHashes(data, numHashes)) {
            int idx = Math.floorMod(h, bitSize);
            if (counters[idx] > 0) {
                counters[idx]--;
            }
        }
    }

    public void remove(String value) {
        remove(value.getBytes(StandardCharsets.UTF_8));
    }

    public boolean mightContain(byte[] data) {
        for (int h : Hash.doubleHashes(data, numHashes)) {
            if (counters[Math.floorMod(h, bitSize)] == 0) {
                return false;
            }
        }
        return true;
    }

    public boolean mightContain(String value) {
        return mightContain(value.getBytes(StandardCharsets.UTF_8));
    }

    public int bitSize() {
        return bitSize;
    }

    public int numHashes() {
        return numHashes;
    }

    /** Returns a deep copy so callers can snapshot state. */
    public CountingBloomFilter copy() {
        return new CountingBloomFilter(Arrays.copyOf(counters, counters.length), numHashes);
    }
}
