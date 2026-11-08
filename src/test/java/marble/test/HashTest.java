package marble.test;

import marble.core.Hash;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HashTest {

    @Test
    void fnv1a32IsDeterministic() {
        byte[] d = "marble".getBytes(StandardCharsets.UTF_8);
        assertEquals(Hash.fnv1a32(d), Hash.fnv1a32(d));
    }

    @Test
    void fnv1a64VariesAcrossInputs() {
        long a = Hash.fnv1a64("alpha");
        long b = Hash.fnv1a64("beta");
        assertNotEquals(a, b);
    }

    @Test
    void murmur3EmptySeedZeroIsZero() {
        assertEquals(0, Hash.murmur3_32(new byte[0], 0));
    }

    @Test
    void murmur3IsDeterministicAndSensitive() {
        byte[] d = "distributed-systems".getBytes(StandardCharsets.UTF_8);
        assertEquals(Hash.murmur3_32(d, 7), Hash.murmur3_32(d, 7));
        assertNotEquals(Hash.murmur3_32(d, 7), Hash.murmur3_32(d, 8));
    }

    @Test
    void doubleHashesHasCorrectLengthAndIsNonNegative() {
        int[] hashes = Hash.doubleHashes("key", 5);
        assertEquals(5, hashes.length);
        for (int h : hashes) {
            assertTrue(h >= 0, "hash must be non-negative");
        }
    }

    @Test
    void doubleHashesDiffersAcrossInputs() {
        int[] a = Hash.doubleHashes("one", 4);
        int[] b = Hash.doubleHashes("two", 4);
        boolean anyDifferent = false;
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                anyDifferent = true;
            }
        }
        assertTrue(anyDifferent);
    }
}
