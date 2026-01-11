package com.example.productAnalytics.factory;

import com.example.productAnalytics.cache.SimpleCache;
import com.example.productAnalytics.evictionStrategy.IEvictionStrategy;
import com.example.productAnalytics.evictionStrategy.LFUEvictionStrategy;
import com.example.productAnalytics.evictionStrategy.LRUEvictionStrategy;
import com.example.productAnalytics.model.ProductView;
import com.example.productAnalytics.repository.ProductViewRepository;
import com.example.productAnalytics.writeStrategy.IWriteStrategy;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class CacheFactory {
    private final Map<String, Object> globalConfig;
    private final ProductViewRepository productViewRepository;
    private volatile SimpleCache<String, Object> recentCache;
    private volatile SimpleCache<String, Object> frequentCache;
    private final Set<SimpleCache<String, Object>> cacheSet = ConcurrentHashMap.newKeySet();

    public SimpleCache<String, Object> getRecent() {
        if (recentCache == null) {
            synchronized (this) {
                if (recentCache == null) {
                    try {
                        recentCache = new SimpleCache<>("recent" + ProductView.class.getSimpleName(),
                                (Integer) globalConfig.get("cacheSize"),
                                new LRUEvictionStrategy<>(),
                                ((Class<? extends IWriteStrategy>) globalConfig.get("cacheWriteStrategy")).getDeclaredConstructor().newInstance());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        cacheSet.add(recentCache);
        return recentCache;
    }

    public SimpleCache<String, Object> getFrequent() {
        if (frequentCache == null) {
            synchronized (this) {
                if (frequentCache == null) {
                    try {
                        frequentCache = new SimpleCache<>("frequent" + ProductView.class.getSimpleName(),
                                (Integer) globalConfig.get("cacheSize"),
                                new LFUEvictionStrategy<>(),
                                ((Class<? extends IWriteStrategy>) globalConfig.get("cacheWriteStrategy")).getDeclaredConstructor().newInstance());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        cacheSet.add(frequentCache);
        return frequentCache;
    }

    public SimpleCache<String, Object> get(String name) {
        SimpleCache<String, Object> newCache = null;
        try {
            newCache = new SimpleCache<>(name,
                    (Integer) globalConfig.get("cacheSize"),
                    ((Class<? extends IEvictionStrategy<String>>) globalConfig.get("cacheEvictionStrategy")).getDeclaredConstructor().newInstance(),
                    ((Class<? extends IWriteStrategy>) globalConfig.get("cacheWriteStrategy")).getDeclaredConstructor().newInstance());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        cacheSet.add(newCache);
        return newCache;
    }

    public Set<SimpleCache<String, Object>> getAll() {
        return cacheSet;
    }

    public Set<SimpleCache<String, Object>> getAllExceptRecentFrequent() {
        Set<SimpleCache<String, Object>> result = ConcurrentHashMap.newKeySet();
        result.addAll(cacheSet);
        result.remove(recentCache);
        result.remove(frequentCache);
        return result;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        List<Object[]> recents = productViewRepository.findRecentProducts(PageRequest.of(0, 3));
        List<Object[]> frequents = productViewRepository.findFrequentProducts(PageRequest.of(0, 3));

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        recents.forEach(x ->
                futures.add(recentCache.put((String) x[0], x[1])));
        frequents.forEach(x ->
                futures.add(frequentCache.put((String) x[0], x[1])));

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }
}
