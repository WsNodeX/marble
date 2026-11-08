package marble.test;

import marble.bloom.BloomFilter;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BloomFilterTest {

    @Test
    void sizingIsSane() {
        assertTrue(BloomFilter.optimalBitSize(1000, 0.01) > 1000);
        assertTrue(BloomFilter.optimalNumHashes(1000, 10_000) >= 1);
    }

    @Test
    void insertedElementsArePresent() {
        BloomFilter f = new BloomFilter(100, 0.01);
        f.put("hello");
        f.put("world");
        assertTrue(f.mightContain("hello"));
        assertTrue(f.mightContain("world"));
    }

    @Test
    void noFalseNegatives() {
        BloomFilter f = new BloomFilter(500, 0.001);
        Set<String> inserted = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            String s = "item-" + i;
            inserted.add(s);
            f.put(s);
        }
        for (String s : inserted) {
            assertTrue(f.mightContain(s), "must never report a false negative: " + s);
        }
    }

    @Test
    void falsePositiveRateStaysBounded() {
        BloomFilter f = new BloomFilter(2000, 0.01);
        Random rnd = new Random(42);
        Set<String> inserted = new HashSet<>();
        while (inserted.size() < 2000) {
            inserted.add("k" + rnd.nextInt(10_000_000));
        }
        inserted.forEach(f::put);

        Set<String> probe = new HashSet<>();
        while (probe.size() < 2000) {
            probe.add("p" + rnd.nextInt(10_000_000));
        }
        int falsePositives = 0;
        for (String p : probe) {
            if (f.mightContain(p)) {
                falsePositives++;
            }
        }
        double rate = (double) falsePositives / probe.size();
        assertTrue(rate < 0.06, "observed fpp too high: " + rate);
        assertTrue(f.falsePositiveProbability() > 0 && f.falsePositiveProbability() < 0.1);
    }

    @Test
    void mergeContainsUnion() {
        BloomFilter a = new BloomFilter(100, 0.01);
        BloomFilter b = new BloomFilter(100, 0.01);
        a.put("x");
        b.put("y");
        BloomFilter merged = a.merge(b);
        assertTrue(merged.mightContain("x"));
        assertTrue(merged.mightContain("y"));
    }

    @Test
    void serializationRoundTrip() {
        BloomFilter f = new BloomFilter(200, 0.01);
        f.put("persist-me");
        f.put("and-me");
        String encoded = f.toBase64();
        assertNotNull(encoded);
        BloomFilter restored = BloomFilter.fromBase64(encoded);
        assertEquals(f.bitSize(), restored.bitSize());
        assertTrue(restored.mightContain("persist-me"));
        assertTrue(restored.mightContain("and-me"));
    }
}
