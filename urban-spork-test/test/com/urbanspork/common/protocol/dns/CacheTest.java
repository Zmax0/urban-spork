package com.urbanspork.common.protocol.dns;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

class CacheTest {
    @Test
    void test() {
        Cache cache = new Cache(2);
        cache.put("k1", "v1", 1, Instant.now());
        cache.put("k2", "v2", 1, Instant.now());
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1100));
        Assertions.assertFalse(cache.get("k1", Instant.now()).isPresent());
        cache.put("k2", "v2", 10, Instant.now());
        cache.put("k3", "v3", 10, Instant.now());
        LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(100));
        Assertions.assertFalse(cache.get("k2", Instant.now()).isPresent());
    }
}
