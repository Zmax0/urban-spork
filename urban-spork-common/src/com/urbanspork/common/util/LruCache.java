package com.urbanspork.common.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.Scheduler;

import java.time.Duration;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class LruCache<K, V> {
    final BiConsumer<K, V> afterExpired;
    final Cache<K, V> inner;

    public LruCache(long capacity, Duration timeToLive, BiConsumer<K, V> afterExpired) {
        this.afterExpired = afterExpired;
        this.inner = Caffeine.newBuilder()
            .maximumSize(capacity)
            .expireAfterAccess(timeToLive)
            .scheduler(Scheduler.systemScheduler())
            .evictionListener((K key, V value, RemovalCause _) -> afterExpired.accept(key, value))
            .build();
    }

    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        return inner.get(key, mappingFunction);
    }

    public void insert(K key, V value) {
        inner.put(key, value);
    }

    public V get(K key) {
        return inner.asMap().get(key);
    }

    public V remove(K key) {
        return inner.asMap().remove(key);
    }

    public void release() {
        for (Map.Entry<K, V> entry : Map.copyOf(inner.asMap()).entrySet()) {
            afterExpired.accept(entry.getKey(), entry.getValue());
        }
        inner.invalidateAll();
        inner.cleanUp();
    }
}
