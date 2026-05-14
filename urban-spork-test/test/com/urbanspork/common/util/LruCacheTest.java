package com.urbanspork.common.util;

import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;

class LruCacheTest {
    @Test
    void test() {
        AtomicInteger expired = new AtomicInteger();
        Duration timeToLive = Duration.ofSeconds(3);
        LruCache<InetSocketAddress, Channel> cache = new LruCache<>(
            2, timeToLive, (_, v) -> {
            expired.incrementAndGet();
            v.close();
        }
        );
        InetSocketAddress a1 = new InetSocketAddress(InetAddress.getLoopbackAddress(), 16801);
        InetSocketAddress a2 = new InetSocketAddress(InetAddress.getLoopbackAddress(), 16802);
        InetSocketAddress a3 = new InetSocketAddress(InetAddress.getLoopbackAddress(), 16803);
        EmbeddedChannel c1 = new EmbeddedChannel();
        EmbeddedChannel c2 = new EmbeddedChannel();
        EmbeddedChannel c3 = new EmbeddedChannel();
        Assertions.assertNull(cache.get(a1));
        cache.computeIfAbsent(a1, _ -> c1);
        Assertions.assertSame(c1, cache.remove(a1));
        Assertions.assertNull(cache.get(a1));
        Channel c1t1 = cache.computeIfAbsent(a1, _ -> c1);
        Channel c1t2 = cache.computeIfAbsent(a1, _ -> c1);
        Assertions.assertSame(c1t1, c1t2);
        cache.computeIfAbsent(a2, _ -> c2);
        cache.computeIfAbsent(a3, _ -> c3);
        waitUntil(() -> presentCount(cache, List.of(a1, a2, a3)) <= 2, Duration.ofSeconds(3));
        Assertions.assertTrue(presentCount(cache, List.of(a1, a2, a3)) <= 2);
        Assertions.assertTrue(expired.get() >= 1);
        cache.insert(a1, new EmbeddedChannel());
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
        Assertions.assertNotNull(cache.get(a1));
        LockSupport.parkNanos(timeToLive.plusSeconds(2).toNanos());
        Assertions.assertNull(cache.get(a1));
        cache.insert(a1, new EmbeddedChannel());
        cache.release();
        Assertions.assertNull(cache.get(a1));
    }

    private static int presentCount(LruCache<InetSocketAddress, Channel> cache, List<InetSocketAddress> keys) {
        int count = 0;
        for (InetSocketAddress key : keys) {
            if (cache.get(key) != null) {
                count++;
            }
        }
        return count;
    }

    private static void waitUntil(BooleanSupplier check, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (check.getAsBoolean()) {
                return;
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(50));
        }
        Assertions.assertTrue(check.getAsBoolean());
    }

}
