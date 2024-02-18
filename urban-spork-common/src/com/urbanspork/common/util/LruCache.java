package com.urbanspork.common.util;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class LruCache<K, V> {
    final long capacity;
    final Duration timeToLive;
    final HashedWheelTimer timer = new HashedWheelTimer(1, TimeUnit.SECONDS);
    final BiConsumer<K, V> afterExpired;
    final LinkedHashMap<K, Pair<V>> inner = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<K, Pair<V>> eldest) {
            boolean flag = size() > capacity;
            if (flag) {
                K key = eldest.getKey();
                Pair<V> pair = eldest.getValue();
                afterExpired.accept(key, pair.value);
                pair.timeout.cancel();
            }
            return flag;
        }
    };

    public LruCache(long capacity, Duration timeToLive, BiConsumer<K, V> afterExpired) {
        this.capacity = capacity;
        this.timeToLive = timeToLive;
        this.afterExpired = afterExpired;
    }

    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        V old = get(key);
        if (old == null) {
            V value = mappingFunction.apply(key);
            insert(key, value);
            return value;
        } else {
            return old;
        }
    }

    public void insert(K key, V value) {
        inner.put(key, new Pair<>(value, timer.newTimeout(timeout -> expire(key, value), timeToLive.toNanos(), TimeUnit.NANOSECONDS)));
    }

    public V get(K key) {
        Pair<V> pair = inner.get(key);
        if (pair != null) {
            pair.timeout.cancel();
            pair.timeout = timer.newTimeout(timeout -> expire(key, pair.value), timeToLive.toNanos(), TimeUnit.NANOSECONDS);
            return pair.value;
        } else {
            return null;
        }
    }

    public V remove(K key) {
        Pair<V> pair = inner.remove(key);
        if (pair != null) {
            pair.timeout.cancel();
            return pair.value;
        } else {
            return null;
        }
    }

    public void release() {
        for (Map.Entry<K, Pair<V>> entry : inner.entrySet()) {
            afterExpired.accept(entry.getKey(), entry.getValue().value);
        }
        timer.stop();
        inner.clear();
    }

    private void expire(K key, V value) {
        afterExpired.accept(key, value);
        inner.remove(key);
    }

    static class Pair<V> {
        V value;
        Timeout timeout;

        public Pair(V value, Timeout timeout) {
            this.value = value;
            this.timeout = timeout;
        }
    }
}
