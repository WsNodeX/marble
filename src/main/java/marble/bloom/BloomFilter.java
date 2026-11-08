package marble.bloom;

import marble.core.Hash;
import marble.core.Preconditions;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * A space-efficient probabilistic set membership structure.
 *
 * <p>A Bloom filter can answer "is X a member?" with no false negatives and a
 * tunable, bounded false-positive rate. It never stores the elements
 * themselves, which is why it is so compact &mdash; a classic building block for
 * cache guards, deduplication and "have I seen this before" checks.</p>
 *
 * <p>This implementation chooses its bit-array size and hash count from the
 * requested capacity and false-positive probability using the standard closed
 * forms, derives the hashes with {@link Hash#doubleHashes(byte[], int)}, and can
 * be merged with another filter of identical shape or serialised to a string.</p>
 */
public final class BloomFilter {

    private final BitArray bits;
    private final int bitSize;
    private final int numHashes;
    private long insertions;

    public BloomFilter(int expectedInsertions, double falsePositiveRate) {
        Preconditions.checkArgument(expectedInsertions > 0, "expectedInsertions must be > 0");
        Preconditions.checkArgument(falsePositiveRate > 0 && falsePositiveRate < 1,
                "falsePositiveRate must be in (0, 1)");
        this.bitSize = optimalBitSize(expectedInsertions, falsePositiveRate);
        this.numHashes = optimalNumHashes(expectedInsertions, bitSize);
        this.bits = new BitArray(bitSize);
        this.insertions = 0;
    }

    private BloomFilter(BitArray bits, int numHashes, long insertions) {
        this.bits = bits;
        this.bitSize = bits.size();
        this.numHashes = numHashes;
        this.insertions = insertions;
    }

    /** Optimal bit-array size for n insertions at false-positive rate p. */
    public static int optimalBitSize(int n, double p) {
        return (int) Math.ceil(-(n * Math.log(p)) / (Math.log(2) * Math.log(2)));
    }

    /** Optimal number of hash functions for n insertions and m bits. */
    public static int optimalNumHashes(int n, int m) {
        return Math.max(1, (int) Math.round((double) m / n * Math.log(2)));
    }

    public void put(byte[] data) {
        for (int h : Hash.doubleHashes(data, numHashes)) {
            bits.set(h % bitSize);
        }
        insertions++;
    }

    public void put(String value) {
        put(value.getBytes(StandardCharsets.UTF_8));
    }

    public boolean mightContain(byte[] data) {
        for (int h : Hash.doubleHashes(data, numHashes)) {
            if (!bits.get(h % bitSize)) {
                return false;
            }
        }
        return true;
    }

    public boolean mightContain(String value) {
        return mightContain(value.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Returns the empirical false-positive probability for the current load,
     * using the standard approximation k * (1 - e^(-kn/m))^k.
     */
    public double falsePositiveProbability() {
        double exponent = -(double) numHashes * insertions / bitSize;
        return Math.pow(1 - Math.exp(exponent), numHashes);
    }

    public int bitSize() {
        return bitSize;
    }

    public int numHashes() {
        return numHashes;
    }

    public long insertions() {
        return insertions;
    }

    /**
     * Returns a new filter containing the logical union of this filter and
     * {@code other}. Both must share the same size and hash count.
     */
    public BloomFilter merge(BloomFilter other) {
        Preconditions.requireNonNull(other, "other");
        if (other.bitSize != this.bitSize || other.numHashes != this.numHashes) {
            throw new IllegalArgumentException("filters must share size and hash count");
        }
        BitArray merged = new BitArray(this.bitSize);
        merged.or(this.bits);
        merged.or(other.bits);
        return new BloomFilter(merged, numHashes, this.insertions + other.insertions);
    }

    /** Serialises the filter to a self-describing Base64 string. */
    public String toBase64() {
        byte[] header = new byte[16];
        writeInt(header, 0, bitSize);
        writeInt(header, 4, numHashes);
        writeLong(header, 8, insertions);
        String headerB64 = Base64.getEncoder().encodeToString(header);
        String bodyB64 = Base64.getEncoder().encodeToString(bits.toBytes());
        return headerB64 + ":" + bodyB64;
    }

    public static BloomFilter fromBase64(String encoded) {
        Preconditions.requireNonNull(encoded, "encoded");
        int colon = encoded.indexOf(':');
        if (colon < 0) {
            throw new IllegalArgumentException("malformed bloom filter string");
        }
        byte[] header = Base64.getDecoder().decode(encoded.substring(0, colon));
        byte[] body = Base64.getDecoder().decode(encoded.substring(colon + 1));
        int bitSize = readInt(header, 0);
        int numHashes = readInt(header, 4);
        long insertions = readLong(header, 8);
        BitArray bits = BitArray.fromBytes(body, bitSize);
        return new BloomFilter(bits, numHashes, insertions);
    }

    private static void writeInt(byte[] b, int off, int v) {
        b[off] = (byte) (v >>> 24);
        b[off + 1] = (byte) (v >>> 16);
        b[off + 2] = (byte) (v >>> 8);
        b[off + 3] = (byte) v;
    }

    private static int readInt(byte[] b, int off) {
        return ((b[off] & 0xff) << 24)
                | ((b[off + 1] & 0xff) << 16)
                | ((b[off + 2] & 0xff) << 8)
                | (b[off + 3] & 0xff);
    }

    private static void writeLong(byte[] b, int off, long v) {
        for (int i = 0; i < 8; i++) {
            b[off + i] = (byte) (v >>> (56 - 8 * i));
        }
    }

    private static long readLong(byte[] b, int off) {
        long v = 0;
        for (int i = 0; i < 8; i++) {
            v = (v << 8) | (b[off + i] & 0xff);
        }
        return v;
    }
}
