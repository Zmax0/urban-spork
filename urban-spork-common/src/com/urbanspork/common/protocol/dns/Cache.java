package com.urbanspork.common.protocol.dns;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Cache extends LinkedHashMap<String, String> {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final int limit;

    public Cache(int limit) {
        this.limit = limit;
    }

    @Override
    public String get(Object key) {
        lock.readLock().lock();
        try {
            return super.get(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public String put(String key, String value) {
        lock.writeLock().lock();
        try {
            return super.put(key, value);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry eldest) {
        lock.writeLock().lock();
        try {
            return size() > limit;
        } finally {
            lock.writeLock().unlock();
        }
    }
}