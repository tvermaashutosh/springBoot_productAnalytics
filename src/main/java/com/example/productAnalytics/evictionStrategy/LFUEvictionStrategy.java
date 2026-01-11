package com.example.productAnalytics.evictionStrategy;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;

public class LFUEvictionStrategy<K> implements IEvictionStrategy<K> {
    /**
     * Always use SET, not PRIORITY QUEUE in Dijkstra.
     *
     * Always use SET, not PRIORITY QUEUE in Dijkstra.
     *
     * Always use SET, not PRIORITY QUEUE in Dijkstra.
     *
     * Always use SET, not PRIORITY QUEUE in Dijkstra.
     *
     * Always use SET, not PRIORITY QUEUE in Dijkstra.
     */
    SortedSet<List<Object>> sortedSet = new ConcurrentSkipListSet<>( /* NOT Comparator.comparingInt(x -> (Integer) x.get(1)) */
            (x, y) -> {
                if (!((Integer) x.get(1)).equals((Integer) y.get(1)))
                    return Integer.compare((Integer) x.get(1), (Integer) y.get(1));
                return Integer.compare(x.get(0).hashCode(), y.get(0).hashCode());
            }
    ); // K key, Integer freq
    Map<K, List<Object>> ref = new ConcurrentHashMap<>();

    @Override
    public synchronized void keyIsAccessed(K key) {
        List<Object> val = ref.computeIfAbsent(key, __ -> {
            List<Object> def = new CopyOnWriteArrayList<>();
            def.add(key);
            def.add(0);
            return def;
        });
        sortedSet.remove(val);
        val.set(1, (Integer) val.get(1) + 1);
        sortedSet.add(val);
    }

    @Override
    public synchronized K getKeyToEvict() {
        List<Object> evictList = sortedSet.first();
        sortedSet.remove(evictList);
        ref.remove((K) evictList.get(0));
        return (K) evictList.get(0);
    }
}
