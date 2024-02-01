package com.urbanspork.common.protocol.shadowsocks.aead2022;

import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.codec.aead.CipherMethod;
import io.netty.util.HashedWheelTimer;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class UdpCipherCache {
    private final HashedWheelTimer timer = new HashedWheelTimer(1, TimeUnit.SECONDS);
    private final LinkedHashMap<Key, UdpCipher> map = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return map.size() > limit;
        }
    };
    private final Duration duration;
    private final int limit;

    public UdpCipherCache(Duration duration, int limit) {
        this.duration = duration;
        this.limit = limit;
    }

    public UdpCipher computeIfAbsent(CipherKind kind, CipherMethod method, byte[] key, long sessionId) {
        Key cacheKey = new Key(kind, key, sessionId);
        return map.computeIfAbsent(cacheKey, k -> {
            timer.newTimeout(timeout -> map.remove(cacheKey), duration.toSeconds(), TimeUnit.SECONDS);
            return new UdpCipher(method, AEAD2022.UDP.sessionSubkey(key, sessionId));
        });
    }

    public boolean contains(CipherKind kind, byte[] key, long sessionId) {
        return map.containsKey(new Key(kind, key, sessionId));
    }

    record Key(CipherKind kind, byte[] key, long sessionId) {
        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || getClass() != other.getClass()) {
                return false;
            }
            Key o = (Key) other;
            return sessionId == o.sessionId && kind == o.kind && Arrays.equals(key, o.key);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(kind, sessionId);
            result = 31 * result + System.identityHashCode(key);
            return result;
        }
    }
}
