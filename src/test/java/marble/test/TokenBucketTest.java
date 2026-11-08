package marble.test;

import marble.core.Clock;
import marble.ratelimit.TokenBucket;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenBucketTest {

    @Test
    void respectsCapacity() {
        TokenBucket bucket = new TokenBucket(5, 100, new Clock.ManualClock(0));
        for (int i = 0; i < 5; i++) {
            assertTrue(bucket.tryAcquire(), "first 5 should pass");
        }
        assertFalse(bucket.tryAcquire(), "6th should be rejected");
    }

    @Test
    void refillRestoresTokens() {
        Clock.ManualClock clock = new Clock.ManualClock(0);
        TokenBucket bucket = new TokenBucket(5, 10, clock); // 10 tokens / second
        for (int i = 0; i < 5; i++) {
            bucket.tryAcquire();
        }
        assertFalse(bucket.tryAcquire());
        clock.advance(1000); // one second later
        assertTrue(bucket.tryAcquire(5), "should be refilled to capacity");
    }

    @Test
    void largeRequestExceedsCapacity() {
        Clock.ManualClock clock = new Clock.ManualClock(0);
        TokenBucket bucket = new TokenBucket(5, 100, clock);
        assertFalse(bucket.tryAcquire(6), "cannot satisfy more than capacity");
        clock.advance(10_000);
        assertFalse(bucket.tryAcquire(6), "still cannot exceed capacity after refill");
    }
}
