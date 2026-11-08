package marble.lock;

import java.util.concurrent.TimeUnit;

/**
 * A minimal distributed-lock contract modelled on the Redis {@code SET NX}
 * pattern.
 *
 * <p>The shape mirrors what a real distributed lock needs: an acquire that may
 * wait up to a timeout, a lease that auto-expires so a crashed holder cannot
 * deadlock the system, and an unlock that only succeeds for the holder that
 * owns the lock (identified by an opaque token). The bundled
 * {@link InMemoryLock} is a single-process reference implementation; a networked
 * one would talk to Redis/etcd while honouring the very same interface.</p>
 */
public interface DistributedLock {

    /**
     * Tries to acquire the lock, waiting at most {@code waitTime} for it to
     * become free. Returns an ownership token on success, or {@code null} if the
     * timeout elapses first.
     */
    String tryLock(long waitTime, TimeUnit unit) throws InterruptedException;

    /**
     * Releases the lock. Succeeds only when {@code token} matches the current
     * holder; returns {@code false} otherwise.
     */
    boolean unlock(String token);

    /** True if the lock is currently held (and not expired). */
    boolean isLocked();
}
