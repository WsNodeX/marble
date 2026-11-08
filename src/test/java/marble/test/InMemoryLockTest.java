package marble.test;

import marble.lock.InMemoryLock;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryLockTest {

    @Test
    void acquireAndRelease() throws InterruptedException {
        InMemoryLock lock = new InMemoryLock();
        String token = lock.tryLock(0, TimeUnit.SECONDS);
        assertNotNull(token);
        assertTrue(lock.isLocked());
        assertTrue(lock.unlock(token));
        assertFalse(lock.isLocked());
    }

    @Test
    void secondHolderBlocksWhileLocked() throws InterruptedException {
        InMemoryLock lock = new InMemoryLock();
        String token = lock.tryLock(0, TimeUnit.SECONDS);
        AtomicReference<String> result = new AtomicReference<>();
        Thread t = new Thread(() -> {
            try {
                result.set(lock.tryLock(100, TimeUnit.MILLISECONDS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        t.start();
        t.join(2000);
        assertNull(result.get(), "second acquire should time out");
        assertTrue(lock.unlock(token));
    }

    @Test
    void wrongTokenCannotUnlock() throws InterruptedException {
        InMemoryLock lock = new InMemoryLock();
        String token = lock.tryLock(0, TimeUnit.SECONDS);
        assertFalse(lock.unlock("not-the-owner"));
        assertTrue(lock.unlock(token));
    }

    @Test
    void expiredLeaseFreesTheLock() throws InterruptedException {
        InMemoryLock lock = new InMemoryLock();
        String token = lock.tryLock(0, TimeUnit.SECONDS, 50, TimeUnit.MILLISECONDS);
        assertNotNull(token);
        Thread.sleep(120); // longer than the 50ms lease
        AtomicReference<String> result = new AtomicReference<>();
        Thread t = new Thread(() -> {
            try {
                result.set(lock.tryLock(0, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        t.start();
        t.join(2000);
        assertNotNull(result.get(), "expired lease should let a new holder in");
    }
}
