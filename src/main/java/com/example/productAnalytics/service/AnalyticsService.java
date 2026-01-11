package com.example.productAnalytics.service;

import com.example.productAnalytics.cache.SimpleCache;
import com.example.productAnalytics.dto.ViewEvent;
import com.example.productAnalytics.factory.CacheFactory;
import com.example.productAnalytics.model.Product;
import com.example.productAnalytics.model.ProductView;
import com.example.productAnalytics.producer.KafkaProducer;
import com.example.productAnalytics.repository.ProductRepository;
import com.example.productAnalytics.repository.ProductViewRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Service
public class AnalyticsService {
    private Map<String, Object> globalConfig;
    private KafkaProducer kafkaProducer;
    private ProductRepository productRepository;
    private ProductViewRepository productViewRepository;
    private SimpleCache<String, Object> cache;
    private SimpleCache<String, Object> recentCache;
    private SimpleCache<String, Object> frequentCache;

    public AnalyticsService(Map<String, Object> globalConfig, KafkaProducer kafkaProducer, ProductRepository productRepository, ProductViewRepository productViewRepository, CacheFactory cacheFactory) {
        this.globalConfig = globalConfig;
        this.kafkaProducer = kafkaProducer;
        this.productRepository = productRepository;
        this.productViewRepository = productViewRepository;
        this.cache = cacheFactory.get("cache" + getClass().getSimpleName());
        this.recentCache = cacheFactory.getRecent();
        this.frequentCache = cacheFactory.getFrequent();
    }

    public CompletableFuture<List<Map<String, Object>>> all() {
        return cache.get("all")
                .thenCompose(existing -> {
                    if (existing != null)
                        return CompletableFuture.completedFuture((List<Map<String, Object>>) existing);

                    List<Map<String, Object>> list = productRepository.findAllProductIdAndName().stream()
                            .map(product -> {
                                Map<String, Object> productMap = new HashMap<>();
                                productMap.put("productId", product[0]);
                                productMap.put("name", product[1]);
                                return productMap;
                            })
                            .toList();

                    return cache.put("all", list)
                            .thenApply(v -> list);
                });
    }

    public CompletableFuture<Product> one(String productId) {
        return cache.get(productId)
                .thenCompose(existing -> {
                    if (existing != null)
                        return CompletableFuture.completedFuture((Product) existing);

                    Product product = productRepository.findByProductId(productId).orElseThrow();

                    return cache.put(productId, product)
                            .thenApply(v -> product);
                });
    }

    public CompletableFuture<Void> view(String productId, String userIp) {
        Supplier<CompletableFuture<Void>> dbWrite = () -> CompletableFuture.runAsync(() -> {
            if (globalConfig.get("asyncDBWriteThroughKafka").equals(true)) {
                ViewEvent viewEvent = new ViewEvent(productId, userIp);
                kafkaProducer.produce(viewEvent);
            } else {
                ProductView pv = productViewRepository.findByProductIdAndUserIp(productId, userIp)
                        .orElseGet(() -> {
                            ProductView productView = new ProductView();
                            productView.setProductId(productId);
                            productView.setUserIp(userIp);
                            productView.setViewCount(0);
                            return productView;
                        });

                pv.setViewCount(pv.getViewCount() + 1);
                productViewRepository.save(pv);
            }
        });

        // 1) Recent cache update + trigger DB update
        CompletableFuture<Void> recentF = recentCache.put(
                productId,
                cur -> {
                    int c = (cur == null) ? 0 : (Integer) cur;
                    return c + 1;
                },
                dbWrite
        );

        // 2) Frequent cache update + NO DB update
        CompletableFuture<Void> frequentF = frequentCache.put(
                productId,
                cur -> {
                    int c = (cur == null) ? 0 : (Integer) cur;
                    return c + 1;
                },
                () -> CompletableFuture.completedFuture(null)
        );

        return CompletableFuture.allOf(recentF, frequentF);
    }

    public Set<String> recent() {
        return recentCache.getCache().keySet();
    }

    public Set<String> frequent() {
        return frequentCache.getCache().keySet();
    }
}
