package marble.hash;

import marble.core.Hash;
import marble.core.Preconditions;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

/**
 * A consistent hash ring with virtual nodes.
 *
 * <p>Consistent hashing maps keys onto a circle. Each physical node is placed
 * many times &mdash; at {@code virtualNodes} evenly-spread points &mdash; so that
 * when a node is added or removed only a small, predictable fraction of keys
 * move. That property is what makes it the backbone of sharded caches
 * (Memcached, Redis Cluster) and content-delivery routing.</p>
 *
 * <p>Membership changes are O(virtualNodes &times; log n); lookups are
 * O(log n) thanks to the {@link TreeMap} backing the ring.</p>
 *
 * @param <T> the physical node type
 */
public final class ConsistentHash<T> {

    private final int virtualNodes;
    private final NavigableMap<Long, T> ring = new TreeMap<>();
    private final Map<T, Integer> nodeCounts = new HashMap<>();

    public ConsistentHash(int virtualNodes) {
        Preconditions.checkArgument(virtualNodes > 0, "virtualNodes must be > 0");
        this.virtualNodes = virtualNodes;
    }

    /** Number of virtual replicas per physical node. */
    public int virtualNodes() {
        return virtualNodes;
    }

    /** Adds a node and its virtual replicas to the ring. */
    public void addNode(T node) {
        Objects.requireNonNull(node, "node");
        for (int i = 0; i < virtualNodes; i++) {
            long position = Hash.hash64(node + "#" + i);
            ring.put(position, node);
        }
        nodeCounts.merge(node, 1, Integer::sum);
    }

    /** Removes a node (and all its replicas) from the ring. */
    public void removeNode(T node) {
        Objects.requireNonNull(node, "node");
        for (int i = 0; i < virtualNodes; i++) {
            long position = Hash.hash64(node + "#" + i);
            ring.remove(position, node);
        }
        nodeCounts.remove(node);
    }

    /** Returns the node responsible for {@code key}, or throws if the ring is empty. */
    public T getNode(String key) {
        Preconditions.checkArgument(key != null, "key must not be null");
        if (ring.isEmpty()) {
            throw new IllegalStateException("ring has no nodes");
        }
        long position = Hash.hash64(key);
        Map.Entry<Long, T> entry = ring.ceilingEntry(position);
        if (entry == null) {
            entry = ring.firstEntry();
        }
        return entry.getValue();
    }

    /** Number of distinct physical nodes currently on the ring. */
    public int size() {
        return nodeCounts.size();
    }

    /** The set of distinct physical nodes. */
    public Collection<T> nodes() {
        return nodeCounts.keySet();
    }
}
