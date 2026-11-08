package marble.id;

/**
 * Minimal contract for a unique-id source.
 */
@FunctionalInterface
public interface IdGenerator {

    /** Returns the next unique identifier. */
    long nextId();
}
