package com.example.productAnalytics.cache;

import com.example.productAnalytics.evictionStrategy.IEvictionStrategy;
import com.example.productAnalytics.executor.KeyBasedExecutor;
import com.example.productAnalytics.writeStrategy.IWriteStrategy;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

public class SimpleCache<K, V> {
    @Getter
    private int size;
    @Getter
    private Map<K, V> cache;
    private KeyBasedExecutor keyBasedExecutor;
    @Getter
    @Setter
    private IEvictionStrategy<K> evictionStrategy;
    @Getter
    @Setter
    private IWriteStrategy writeStrategy;
    @Getter
    private String name;

    public SimpleCache(String name, int size, IEvictionStrategy<K> evictionStrategy, IWriteStrategy writeStrategy) {
        this.name = name;
        this.size = size;
        cache = new ConcurrentHashMap<>();
        this.keyBasedExecutor = new KeyBasedExecutor(size);
        this.evictionStrategy = evictionStrategy;
        this.writeStrategy = writeStrategy;
    }

    public CompletableFuture<V> get(K key) {
        return keyBasedExecutor.asyncTaskForKey(key, () -> {
            evictionStrategy.keyIsAccessed(key);
            return cache.get(key);
        });
    }

    public CompletableFuture<Void> put(K key, V value, Supplier<CompletableFuture<Void>> dbWrite) {
        return keyBasedExecutor.asyncTaskForKey(key, () -> {

                    if (!cache.containsKey(key) && cache.size() == size) {
                        K keyToEvict = evictionStrategy.getKeyToEvict();
                        if (keyToEvict != null) {
                            int indexToEvict = keyBasedExecutor.getExecutorIndexForKey(keyToEvict);
                            int curIndex = keyBasedExecutor.getExecutorIndexForKey(key);

                            if (curIndex == indexToEvict) cache.remove(keyToEvict);
                            else keyBasedExecutor.asyncTaskForKey(keyToEvict, () -> cache.remove(keyToEvict));
                        }
                    }

                    evictionStrategy.keyIsAccessed(key);
                    return writeStrategy.write(() -> {
                                cache.put(key, value);
                                return CompletableFuture.completedFuture(null);
                            },
                            dbWrite);
                })
                .thenCompose(f -> f);
    }

    public CompletableFuture<Void> put(K key, V value) {
        return put(key, value, () -> CompletableFuture.completedFuture(null));
    }

    public CompletableFuture<Void> put(K key, Function<V, V> cacheUpdate, Supplier<CompletableFuture<Void>> dbWrite) {
        return keyBasedExecutor.asyncTaskForKey(key, () -> {
                    V cur = cache.get(key);
                    V upd = cacheUpdate.apply(cur);

                    if (cur == null && cache.size() == size) {
                        K keyToEvict = evictionStrategy.getKeyToEvict();
                        if (keyToEvict != null) {
                            int indexToEvict = keyBasedExecutor.getExecutorIndexForKey(keyToEvict);
                            int curIndex = keyBasedExecutor.getExecutorIndexForKey(key);

                            if (curIndex == indexToEvict) cache.remove(keyToEvict);
                            else keyBasedExecutor.asyncTaskForKey(keyToEvict, () -> cache.remove(keyToEvict));
                        }
                    }

                    evictionStrategy.keyIsAccessed(key);
                    return writeStrategy.write(() -> {
                                cache.put(key, upd);
                                return CompletableFuture.completedFuture(null);
                            },
                            dbWrite);
                })
                .thenCompose(f -> f);
    }
}
