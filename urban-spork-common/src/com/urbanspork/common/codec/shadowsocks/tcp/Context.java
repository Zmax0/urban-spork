package com.urbanspork.common.codec.shadowsocks.tcp;

import com.urbanspork.common.protocol.shadowsocks.aead2022.AEAD2022;
import com.urbanspork.common.util.LruCache;

import java.time.Duration;
import java.util.Arrays;

public record Context(LruCache<Key, Object> saltCache) {
    private static final Object V = new Object();

    public Context() {
        this(null);
    }

    public static Context checkReplay() {
        return new Context(new LruCache<>(Long.MAX_VALUE, Duration.ofSeconds(AEAD2022.SERVER_STREAM_TIMESTAMP_MAX_DIFF * 2), (k, v) -> {}));
    }

    public boolean checkNonceReplay(byte[] nonce) {
        if (saltCache == null) {
            return false;
        }
        Key key = new Key(nonce);
        Object unused = saltCache.get(key);
        if (unused != null) {
            return true;
        } else {
            saltCache.computeIfAbsent(key, k -> V);
            return false;
        }
    }

    record Key(byte[] nonce) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key nonce1 = (Key) o;
            return Arrays.equals(nonce, nonce1.nonce);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(nonce);
        }
    }
}
