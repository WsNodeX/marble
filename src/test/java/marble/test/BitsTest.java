package marble.test;

import marble.core.Bits;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BitsTest {

    @Test
    void extractLowBits() {
        // value 0b1011, extract 3 bits at shift 0 -> 0b011 = 3
        assertEquals(3, Bits.extract(0b1011, 3, 0));
    }

    @Test
    void extractHighBits() {
        // value 0b101101, extract 3 bits at shift 3 -> 0b101 = 5
        assertEquals(5, Bits.extract(0b101101, 3, 3));
    }

    @Test
    void maskWidth() {
        assertEquals(0b1111, Bits.mask(4));
        assertEquals(0b1, Bits.mask(1));
    }

    @Test
    void bitLength() {
        assertEquals(0, Bits.bitLength(0));
        assertEquals(1, Bits.bitLength(1));
        assertEquals(4, Bits.bitLength(0b1010));
        assertEquals(64, Bits.bitLength(-1L));
    }
}
