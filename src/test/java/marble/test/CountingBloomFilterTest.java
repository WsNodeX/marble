package marble.test;

import marble.bloom.CountingBloomFilter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CountingBloomFilterTest {

    @Test
    void putThenContain() {
        CountingBloomFilter f = new CountingBloomFilter(100, 0.01);
        f.put("alpha");
        assertTrue(f.mightContain("alpha"));
    }

    @Test
    void removeMakesUniqueElementAbsent() {
        CountingBloomFilter f = new CountingBloomFilter(100, 0.01);
        f.put("beta");
        assertTrue(f.mightContain("beta"));
        f.remove("beta");
        assertFalse(f.mightContain("beta"));
    }

    @Test
    void removingNonMemberIsSafe() {
        CountingBloomFilter f = new CountingBloomFilter(100, 0.01);
        f.put("gamma");
        // Removing something never inserted must not throw and must not hide gamma.
        f.remove("unknown");
        assertTrue(f.mightContain("gamma"));
    }

    @Test
    void copyIsIndependent() {
        CountingBloomFilter f = new CountingBloomFilter(100, 0.01);
        f.put("delta");
        CountingBloomFilter copy = f.copy();
        copy.remove("delta");
        assertTrue(f.mightContain("delta"));
        assertFalse(copy.mightContain("delta"));
    }
}
