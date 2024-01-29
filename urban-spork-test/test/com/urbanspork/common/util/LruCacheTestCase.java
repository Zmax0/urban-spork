package com.urbanspork.common.util;

import com.urbanspork.test.template.TraceLevelLoggerTestTemplate;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

class LruCacheTestCase extends TraceLevelLoggerTestTemplate {
    @Test
    void test() {
        Duration timeToLive = Duration.ofSeconds(3);
        LruCache<InetSocketAddress, Channel> cache = new LruCache<>(2, timeToLive, (k, v) -> v.close());
        InetSocketAddress a1 = new InetSocketAddress(InetAddress.getLoopbackAddress(), 16801);
        InetSocketAddress a2 = new InetSocketAddress(InetAddress.getLoopbackAddress(), 16802);
        InetSocketAddress a3 = new InetSocketAddress(InetAddress.getLoopbackAddress(), 16803);
        Assertions.assertNull(cache.get(a1));
        cache.insert(a1, new EmbeddedChannel());
        cache.remove(a1);
        Assertions.assertNull(cache.get(a1));
        cache.insert(a3, new EmbeddedChannel());
        cache.insert(a2, new EmbeddedChannel());
        cache.insert(a1, new EmbeddedChannel());
        Assertions.assertNull(cache.get(a3));
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
        Assertions.assertNotNull(cache.get(a1));
        LockSupport.parkNanos(timeToLive.plusSeconds(2).toNanos());
        Assertions.assertNull(cache.get(a1));
        cache.insert(a1, new EmbeddedChannel());
        cache.clear();
        Assertions.assertNull(cache.get(a1));
    }

    @Override
    protected Class<?> loggerClass() {
        return LruCache.class;
    }
}
