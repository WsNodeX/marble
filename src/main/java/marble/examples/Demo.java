package marble.examples;

import marble.bloom.BloomFilter;
import marble.hash.ConsistentHash;
import marble.id.Snowflake;
import marble.id.SnowflakeIdGenerator;
import marble.lock.InMemoryLock;
import marble.ratelimit.TokenBucket;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Self-contained tour of every primitive in the toolkit.
 *
 * <p>Run with {@code java marble.examples.Demo} after compiling. The output is
 * stable enough to paste into the project README as a "quick start" example.</p>
 */
public final class Demo {

    private Demo() {
    }

    public static void main(String[] args) throws InterruptedException {
        snowflake();
        bloom();
        consistentHash();
        rateLimiter();
        distributedLock();
    }

    private static void snowflake() {
        System.out.println("== Snowflake distributed id ==");
        SnowflakeIdGenerator gen = new SnowflakeIdGenerator(Snowflake.classic(), 42);
        for (int i = 0; i < 3; i++) {
            long id = gen.nextId();
            SnowflakeIdGenerator.Components c = gen.decode(id);
            System.out.printf("id=%d  node=%d  seq=%d%n", id, c.nodeId(), c.sequence());
        }
    }

    private static void bloom() {
        System.out.println("\n== Bloom filter ==");
        BloomFilter filter = new BloomFilter(1_000, 0.01);
        List<String> words = List.of("apple", "banana", "cherry", "dragonfruit");
        words.forEach(filter::put);
        System.out.println("contains banana? " + filter.mightContain("banana"));
        System.out.println("contains zucchini? " + filter.mightContain("zucchini"));
        System.out.println("bit size=" + filter.bitSize() + " hashes=" + filter.numHashes());
    }

    private static void consistentHash() {
        System.out.println("\n== Consistent hashing ==");
        ConsistentHash<String> ring = new ConsistentHash<>(100);
        List<String> nodes = List.of("cache-a", "cache-b", "cache-c");
        nodes.forEach(ring::addNode);
        System.out.println("key 'user:1001' -> " + ring.getNode("user:1001"));
        System.out.println("key 'order:77'   -> " + ring.getNode("order:77"));
        ring.removeNode("cache-b");
        System.out.println("after removing cache-b, key 'order:77' -> " + ring.getNode("order:77"));
    }

    private static void rateLimiter() {
        System.out.println("\n== Token-bucket rate limiter ==");
        TokenBucket bucket = new TokenBucket(5, 5, marble.core.Clock.SYSTEM);
        int allowed = 0;
        for (int i = 0; i < 8; i++) {
            if (bucket.tryAcquire()) {
                allowed++;
            }
        }
        System.out.println("allowed " + allowed + " of 8 immediate requests (capacity=5)");
    }

    private static void distributedLock() throws InterruptedException {
        System.out.println("\n== Distributed lock ==");
        InMemoryLock lock = new InMemoryLock();
        String token = lock.tryLock(0, TimeUnit.SECONDS);
        System.out.println("acquired? " + (token != null) + "  locked? " + lock.isLocked());
        boolean released = lock.unlock(token);
        System.out.println("released? " + released + "  locked? " + lock.isLocked());
    }
}
