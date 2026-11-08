package marble.core;

/**
 * Lightweight argument and state guards used across the toolkit.
 *
 * <p>Keeping these in one place means every primitive validates its inputs the
 * same way and surfaces a consistent, readable exception message.</p>
 */
public final class Preconditions {

    private Preconditions() {
    }

    public static <T> T requireNonNull(T value, String message) {
        if (value == null) {
            throw new NullPointerException(message);
        }
        return value;
    }

    public static void checkArgument(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void checkState(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
