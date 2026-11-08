package marble.core;

/**
 * Abstraction over a monotonic time source.
 *
 * <p>All time-dependent primitives (id generation, rate limiting, lock leases)
 * read time through this interface instead of calling {@code System.currentTimeMillis()}
 * directly. That indirection makes the components deterministic and unit-testable:
 * a test can drive a {@link ManualClock} forward in controlled steps.</p>
 */
@FunctionalInterface
public interface Clock {

    /** Returns the current time in milliseconds. */
    long millis();

    /** Default clock backed by the system wall clock. */
    Clock SYSTEM = System::currentTimeMillis;

    /** A clock whose time can be advanced programmatically. */
    final class ManualClock implements Clock {
        private long now;

        public ManualClock(long initialMillis) {
            this.now = initialMillis;
        }

        public ManualClock() {
            this(0L);
        }

        /** Advances the clock by {@code deltaMillis}. */
        public void advance(long deltaMillis) {
            this.now += deltaMillis;
        }

        /** Sets the clock to an absolute value. */
        public void set(long absoluteMillis) {
            this.now = absoluteMillis;
        }

        @Override
        public long millis() {
            return now;
        }
    }
}
