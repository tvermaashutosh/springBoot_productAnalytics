package com.example.productAnalytics.evictionStrategy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LRUEvictionStrategy<K> implements IEvictionStrategy<K> {
    Node<K> head, tail;
    Map<K, Node<K>> ref = new ConcurrentHashMap<>();

    @Override
    public synchronized void keyIsAccessed(K key) {
        Node<K> node = ref.getOrDefault(key, new Node<>());

        // move head to next
        if (head == node && head.next != null) head = head.next;
        // or set head
        else if (head == null) head = node;

        // move tail to prev
        if (tail == node && tail.prev != null) tail = tail.prev;
        // or set tail
        else if (tail == null) tail = node;

        // detach prev
        if (node.prev != null) node.prev.next = node.next;
        // detach next
        if (node.next != null) node.next.prev = node.prev;

        // attach at last
        if (tail != node) {
            tail.next = node;
            node.prev = tail;
            tail = node;
        }

        node.key = key;
        ref.put(key, node);
    }

    @Override
    public synchronized K getKeyToEvict() {
        Node<K> evictNode = head;
        if (head.next != null) head.next.prev = null;
        head = head.next;
        ref.remove(evictNode.key);
        return evictNode.key;
    }

    private static class Node<K> {
        K key;
        Node<K> prev, next;
    }
}
