package com.example.productAnalytics.executor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class KeyBasedExecutor {
    private int size;
    private ExecutorService[] executors;

    public KeyBasedExecutor(int size) {
        this.size = size;
        this.executors = new ExecutorService[size];
        for (int i = 0; i < size; i++) executors[i] = Executors.newSingleThreadExecutor();
    }

    public <T> CompletableFuture<T> asyncTaskForKey(Object key, Supplier<T> supplier) {
        int index = getExecutorIndexForKey(key);
        return CompletableFuture.supplyAsync(supplier, executors[index]);
    }

    public int getExecutorIndexForKey(Object key) {
        return Math.abs(key.hashCode() % size);
    }

    public void shutdown() {
        for (ExecutorService executor : executors) executor.shutdown();
    }
}
