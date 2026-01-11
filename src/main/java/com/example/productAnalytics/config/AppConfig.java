package com.example.productAnalytics.config;

import com.example.productAnalytics.evictionStrategy.LRUEvictionStrategy;
import com.example.productAnalytics.writeStrategy.WriteBackStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class AppConfig {
    @Bean
    public Map<String, Object> globalConfig() {
        Map<String, Object> globalConfig = new ConcurrentHashMap<>();
        globalConfig.put("cacheSize", 3);
        globalConfig.put("cacheEvictionStrategy", LRUEvictionStrategy.class);
        globalConfig.put("cacheWriteStrategy", WriteBackStrategy.class);
        globalConfig.put("parallelWriteThrough", false);
        globalConfig.put("asyncDBWriteThroughKafka", true);

        return globalConfig;
    }
}
