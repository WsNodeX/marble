package marble.core;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Deterministic, dependency-free hash functions.
 *
 * <p>These are intentionally small, well-known algorithms (FNV-1a and Murmur3)
 * rather than cryptographic hashes. They are fast, have good avalanche
 * behaviour, and &mdash; crucially &mdash; produce identical results on every
 * machine and JVM, which is what distributed primitives need.</p>
 *
 * <p>The {@link #doubleHashes(byte[], int)} helper implements the
 * Kirsch&ndash;Mitzenmacher trick: two independent base hashes are combined to
 * derive an arbitrary number of extra hashes without extra passes over the
 * data. This is the standard way to keep a Bloom filter cheap.</p>
 */
public final class Hash {

    private Hash() {
    }

    /** FNV-1a 32-bit hash. */
    public static int fnv1a32(byte[] data) {
        int hash = 0x811c9dc5;
        for (byte b : data) {
            hash ^= (b & 0xff);
            hash *= 0x01000193;
        }
        return hash;
    }

    /** FNV-1a 64-bit hash. */
    public static long fnv1a64(byte[] data) {
        long hash = 0xcbf29ce484222325L;
        for (byte b : data) {
            hash ^= (b & 0xff);
            hash *= 0x100000001b3L;
        }
        return hash;
    }

    /** FNV-1a 64-bit hash over a string's UTF-8 bytes. */
    public static long fnv1a64(String text) {
        return fnv1a64(text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Murmur3 32-bit hash with a seed.
     *
     * @see <a href="https://github.com/aappleby/smhasher">SMHasher</a>
     */
    public static int murmur3_32(byte[] data, int seed) {
        final int c1 = 0xcc9e2d51;
        final int c2 = 0x1b873593;
        int h1 = seed;
        int len = data.length;
        int i = 0;
        int nblocks = len >> 2;

        for (int block = 0; block < nblocks; block++) {
            int k1 = (data[i] & 0xff)
                    | ((data[i + 1] & 0xff) << 8)
                    | ((data[i + 2] & 0xff) << 16)
                    | ((data[i + 3] & 0xff) << 24);
            i += 4;

            k1 *= c1;
            k1 = Integer.rotateLeft(k1, 15);
            k1 *= c2;
            h1 ^= k1;
            h1 = Integer.rotateLeft(h1, 13);
            h1 = h1 * 5 + 0xe6546b64;
        }

        int tail = len & 3;
        int k1 = 0;
        switch (tail) {
            case 3:
                k1 ^= (data[i + 2] & 0xff) << 16;
                // fall through
            case 2:
                k1 ^= (data[i + 1] & 0xff) << 8;
                // fall through
            case 1:
                k1 ^= (data[i] & 0xff);
                k1 *= c1;
                k1 = Integer.rotateLeft(k1, 15);
                k1 *= c2;
                h1 ^= k1;
            default:
                break;
        }

        h1 ^= len;
        h1 ^= h1 >>> 16;
        h1 *= 0x85ebca6b;
        h1 ^= h1 >>> 13;
        h1 *= 0xc2b2ae35;
        h1 ^= h1 >>> 16;
        return h1;
    }

    /**
     * Derives {@code k} independent-looking hashes from a single input using
     * two base hashes: {@code h_i = h1 + i * h2}. Each result is mapped to a
     * non-negative {@code int} so callers can take {@code hash % m}.
     */
    public static int[] doubleHashes(byte[] data, int k) {
        int h1 = murmur3_32(data, 0);
        int h2 = murmur3_32(data, 0x9747b28c);
        int[] out = new int[k];
        for (int i = 0; i < k; i++) {
            long combined = (long) h1 + (long) i * h2;
            out[i] = Math.floorMod(combined, Integer.MAX_VALUE);
        }
        return out;
    }

    /** Convenience overload that hashes a UTF-8 string. */
    public static int[] doubleHashes(String text, int k) {
        return doubleHashes(text.getBytes(StandardCharsets.UTF_8), k);
    }

    /**
     * A 64-bit hash built by mixing two Murmur3 32-bit hashes with different
     * seeds. The wider output and stronger avalanche make it well suited to
     * placing points on a consistent-hash ring, where a 32-bit hash would leave
     * visible clustering for short keys.
     */
    public static long hash64(String text) {
        byte[] b = text.getBytes(StandardCharsets.UTF_8);
        int h1 = murmur3_32(b, 0);
        int h2 = murmur3_32(b, 0x9e3779b9);
        return ((long) h1 << 32) | (h2 & 0xffffffffL);
    }

    /** Returns true when two byte arrays are element-wise equal. */
    public static boolean bytesEqual(byte[] a, byte[] b) {
        return Arrays.equals(a, b);
    }
}
