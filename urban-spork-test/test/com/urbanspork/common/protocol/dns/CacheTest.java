package com.urbanspork.common.protocol.dns;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CacheTest {
    @Test
    void testRemoveEldest() {
        Cache cache = new Cache(2);
        cache.put("1", "1");
        cache.put("2", "2");
        cache.put("3", "3");
        Assertions.assertFalse(cache.containsKey("1"));
    }
}
