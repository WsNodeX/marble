package marble.test;

import marble.bloom.BitArray;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BitArrayTest {

    @Test
    void setAndGetWrapAround() {
        BitArray a = new BitArray(8);
        a.set(3);
        a.set(11); // 11 % 8 == 3
        assertTrue(a.get(3));
        assertTrue(a.get(11));
        assertFalse(a.get(4));
    }

    @Test
    void orCombines() {
        BitArray a = new BitArray(8);
        BitArray b = new BitArray(8);
        a.set(1);
        b.set(2);
        a.or(b);
        assertTrue(a.get(1));
        assertTrue(a.get(2));
    }

    @Test
    void serializationRoundTrip() {
        BitArray a = new BitArray(8);
        a.set(0);
        a.set(7);
        BitArray b = BitArray.fromBytes(a.toBytes(), 8);
        assertEquals(a, b);
    }

    @Test
    void equalityIsContentBased() {
        BitArray a = new BitArray(8);
        BitArray b = new BitArray(8);
        a.set(5);
        b.set(5);
        assertEquals(a, b);
        assertNotSame(a, b);
    }
}
