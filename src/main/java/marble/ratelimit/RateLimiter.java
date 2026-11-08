package marble.ratelimit;

/**
 * Contract for a rate limiter.
 */
public interface RateLimiter {

    /** Tries to consume a single permit without blocking. */
    boolean tryAcquire();

    /** Tries to consume {@code permits} permits without blocking. */
    boolean tryAcquire(int permits);
}
