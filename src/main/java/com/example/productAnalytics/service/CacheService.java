package com.example.productAnalytics.service;

import com.example.productAnalytics.cache.SimpleCache;
import com.example.productAnalytics.evictionStrategy.LFUEvictionStrategy;
import com.example.productAnalytics.evictionStrategy.LRUEvictionStrategy;
import com.example.productAnalytics.factory.CacheFactory;
import com.example.productAnalytics.writeStrategy.WriteBackStrategy;
import com.example.productAnalytics.writeStrategy.WriteThroughStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CacheService {
    private final Map<String, Object> globalConfig;
    private final CacheFactory cacheFactory;

    public void evictionStrategy(String strategy) {
        globalConfig.remove("cacheEvictionStrategy");
        Set<SimpleCache<String, Object>> set = cacheFactory.getAllExceptRecentFrequent();
        if (strategy.equalsIgnoreCase("LFU")) {
            set.forEach(x -> x.setEvictionStrategy(new LFUEvictionStrategy<>()));
            globalConfig.put("cacheEvictionStrategy", LFUEvictionStrategy.class);
        } else if (strategy.equalsIgnoreCase("LRU")) {
            set.forEach(x -> x.setEvictionStrategy(new LRUEvictionStrategy<>()));
            globalConfig.put("cacheEvictionStrategy", LRUEvictionStrategy.class);
        }
    }

    public void writeStrategy(String strategy) {
        globalConfig.remove("cacheWriteStrategy");
        Set<SimpleCache<String, Object>> set = cacheFactory.getAll();
        if (strategy.equalsIgnoreCase("WriteThrough")) {
            set.forEach(x -> x.setWriteStrategy(new WriteThroughStrategy(globalConfig)));
            globalConfig.put("cacheWriteStrategy", WriteThroughStrategy.class);
        } else if (strategy.equalsIgnoreCase("WriteBack")) {
            set.forEach(x -> x.setWriteStrategy(new WriteBackStrategy()));
            globalConfig.put("cacheWriteStrategy", WriteBackStrategy.class);
        }
    }

    public void parallelWriteThrough(Boolean yes) {
        globalConfig.remove("parallelWriteThrough");
        globalConfig.put("parallelWriteThrough", yes);
    }

    public List<Map<String, Object>> stats() {
        return cacheFactory.getAll().stream()
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", c.getName());
                    m.put("capacity", c.getSize());
                    m.put("currentSize", c.getCache().size());
                    m.put("fillPercent", (c.getSize() == 0) ? 0 : (int) ((c.getCache().size() * 100.0) / c.getSize()));
                    m.put("evictionStrategy", c.getEvictionStrategy().getClass().getSimpleName());
                    m.put("writeStrategy", c.getWriteStrategy().getClass().getSimpleName());
                    m.put("cache", c.getCache());
                    return m;
                })
                .toList();
    }

    public void clear(String cacheName) {
        if (cacheName.equalsIgnoreCase("recent")) {
            cacheFactory.getRecent().getCache().clear();
        } else if (cacheName.equalsIgnoreCase("frequent")) {
            cacheFactory.getFrequent().getCache().clear();
        } else if (cacheName.equalsIgnoreCase("all")) {
            Set<SimpleCache<String, Object>> set = cacheFactory.getAll();
            set.forEach(x -> x.getCache().clear());
        }
    }
}
