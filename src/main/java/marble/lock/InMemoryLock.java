package marble.lock;

import marble.core.Clock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A single-process reference implementation of {@link DistributedLock}.
 *
 * <p>It demonstrates the full distributed-lock lifecycle &mdash; token-based
 * ownership, bounded wait, lease expiry and safe release &mdash; without any
 * network. Swap the backing store for Redis and the algorithm is unchanged:
 * {@code SET key token NX PX lease}, release via a guarded {@code DEL},
 * and a watchdog that renews the lease while the owner is still active.</p>
 */
public final class InMemoryLock implements DistributedLock {

    private final ReentrantLock gate = new ReentrantLock();
    private final Condition free = gate.newCondition();
    private final Clock clock;

    private String ownerToken;
    private long expiryMillis;

    public InMemoryLock() {
        this(Clock.SYSTEM);
    }

    public InMemoryLock(Clock clock) {
        this.clock = clock;
    }

    @Override
    public String tryLock(long waitTime, TimeUnit unit) throws InterruptedException {
        long deadline = clock.millis() + unit.toMillis(waitTime);
        gate.lock();
        try {
            while (isHeld()) {
                long remaining = deadline - clock.millis();
                if (remaining <= 0) {
                    return null;
                }
                free.await(remaining, TimeUnit.MILLISECONDS);
            }
            String token = newToken();
            ownerToken = token;
            // Default acquire holds until explicitly released (no auto-expiry).
            // Use the explicit-lease overload for crash-safe expiry semantics.
            expiryMillis = Long.MAX_VALUE;
            return token;
        } finally {
            gate.unlock();
        }
    }

    /**
     * Acquires the lock with an explicit lease duration (how long the lock
     * survives if the holder crashes). This is the form a real client uses.
     */
    public String tryLock(long waitTime, TimeUnit waitUnit, long leaseTime, TimeUnit leaseUnit)
            throws InterruptedException {
        long deadline = clock.millis() + waitUnit.toMillis(waitTime);
        gate.lock();
        try {
            while (isHeld()) {
                long remaining = deadline - clock.millis();
                if (remaining <= 0) {
                    return null;
                }
                free.await(remaining, TimeUnit.MILLISECONDS);
            }
            String token = newToken();
            ownerToken = token;
            expiryMillis = clock.millis() + leaseUnit.toMillis(leaseTime);
            return token;
        } finally {
            gate.unlock();
        }
    }

    @Override
    public boolean unlock(String token) {
        gate.lock();
        try {
            if (token != null && token.equals(ownerToken) && !isExpired()) {
                ownerToken = null;
                free.signalAll();
                return true;
            }
            return false;
        } finally {
            gate.unlock();
        }
    }

    @Override
    public boolean isLocked() {
        gate.lock();
        try {
            return isHeld();
        } finally {
            gate.unlock();
        }
    }

    private boolean isHeld() {
        return ownerToken != null && !isExpired();
    }

    private boolean isExpired() {
        if (ownerToken == null) {
            return true;
        }
        if (clock.millis() >= expiryMillis) {
            ownerToken = null;
            return true;
        }
        return false;
    }

    private static String newToken() {
        return Long.toHexString(ThreadLocalRandom.current().nextLong())
                + Long.toHexString(System.nanoTime());
    }
}
