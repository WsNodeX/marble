package marble.test;

import marble.hash.ConsistentHash;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsistentHashTest {

    @Test
    void emptyRingRejectsLookups() {
        ConsistentHash<String> ring = new ConsistentHash<>(10);
        assertThrows(IllegalStateException.class, () -> ring.getNode("anything"));
    }

    @Test
    void lookupAlwaysReturnsAMember() {
        ConsistentHash<String> ring = new ConsistentHash<>(50);
        ring.addNode("n1");
        ring.addNode("n2");
        ring.addNode("n3");
        for (int i = 0; i < 1000; i++) {
            assertTrue(ring.nodes().contains(ring.getNode("key-" + i)));
        }
    }

    @Test
    void loadIsBalanced() {
        ConsistentHash<String> ring = new ConsistentHash<>(200);
        List<String> nodes = List.of("a", "b", "c");
        nodes.forEach(ring::addNode);
        Map<String, Integer> counts = new HashMap<>();
        int total = 6000;
        for (int i = 0; i < total; i++) {
            counts.merge(ring.getNode("item-" + i), 1, Integer::sum);
        }
        for (String n : nodes) {
            int c = counts.getOrDefault(n, 0);
            // Each node should own a meaningful, roughly equal share.
            assertTrue(c > total / nodes.size() / 2,
                    "node " + n + " got unfairly few keys: " + c);
        }
    }

    @Test
    void removingNodeKeepsRingValid() {
        ConsistentHash<String> ring = new ConsistentHash<>(100);
        ring.addNode("a");
        ring.addNode("b");
        ring.addNode("c");
        ring.removeNode("b");
        assertEquals(2, ring.size());
        for (int i = 0; i < 1000; i++) {
            String node = ring.getNode("k-" + i);
            assertTrue(node.equals("a") || node.equals("c"));
        }
    }

    @Test
    void addingNodeMovesFewKeys() {
        ConsistentHash<String> ring = new ConsistentHash<>(50);
        ring.addNode("a");
        ring.addNode("b");
        int total = 5000;
        List<String> keys = new ArrayList<>();
        Map<String, String> before = new HashMap<>();
        for (int i = 0; i < total; i++) {
            String k = "doc-" + i;
            keys.add(k);
            before.put(k, ring.getNode(k));
        }
        ring.addNode("c");
        int changed = 0;
        for (String k : keys) {
            if (!before.get(k).equals(ring.getNode(k))) {
                changed++;
            }
        }
        // A single new node should remap well under half the keys (vs ~all for plain modulo).
        assertTrue(changed < total * 0.5, "too many keys moved: " + changed);
    }
}
