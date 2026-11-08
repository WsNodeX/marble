package marble.ratelimit;

import marble.core.Clock;
import marble.core.Preconditions;

/**
 * A token-bucket rate limiter.
 *
 * <p>The bucket holds at most {@code capacity} tokens and is refilled at a fixed
 * rate. Each {@link #tryAcquire()} consumes one token if available, otherwise it
 * returns {@code false} immediately &mdash; no blocking, no fairness guarantees,
 * just a cheap, accurate throttle for "at most N per second" style limits.</p>
 *
 * <p>Refill is lazy: the bucket only recomputes its token count when asked,
 * using the supplied {@link Clock}, so it costs nothing while idle and stays
 * deterministic under tests.</p>
 */
public final class TokenBucket implements RateLimiter {

    private final double capacity;
    private final double refillPerMillis;
    private final Clock clock;
    private double tokens;
    private long lastRefill;

    public TokenBucket(double capacity, double refillPerSecond, Clock clock) {
        Preconditions.checkArgument(capacity > 0, "capacity must be > 0");
        Preconditions.checkArgument(refillPerSecond > 0, "refillPerSecond must be > 0");
        Preconditions.requireNonNull(clock, "clock");
        this.capacity = capacity;
        this.refillPerMillis = refillPerSecond / 1_000.0;
        this.clock = clock;
        this.tokens = capacity;
        this.lastRefill = clock.millis();
    }

    public TokenBucket(double capacity, double refillPerSecond) {
        this(capacity, refillPerSecond, Clock.SYSTEM);
    }

    @Override
    public boolean tryAcquire() {
        return tryAcquire(1);
    }

    @Override
    public synchronized boolean tryAcquire(int permits) {
        Preconditions.checkArgument(permits >= 0, "permits must be >= 0");
        refill();
        if (tokens >= permits) {
            tokens -= permits;
            return true;
        }
        return false;
    }

    /** Current (possibly fractional) token count, after a refill. */
    public synchronized double available() {
        refill();
        return tokens;
    }

    private void refill() {
        long now = clock.millis();
        double elapsedMillis = (now - lastRefill);
        if (elapsedMillis <= 0) {
            return;
        }
        tokens = Math.min(capacity, tokens + elapsedMillis * refillPerMillis);
        lastRefill = now;
    }
}
