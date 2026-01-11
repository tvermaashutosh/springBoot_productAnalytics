package com.example.productAnalytics.evictionStrategy;

public interface IEvictionStrategy<K> {
    void keyIsAccessed(K key);
    K getKeyToEvict();
}
