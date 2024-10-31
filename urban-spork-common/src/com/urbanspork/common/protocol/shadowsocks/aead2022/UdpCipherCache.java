package com.urbanspork.common.protocol.shadowsocks.aead2022;

import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.codec.aead.CipherMethod;
import com.urbanspork.common.util.LruCache;

import java.util.Arrays;
import java.util.Objects;

public enum UdpCipherCache {
    INSTANCE(new LruCache<>(AEAD2022.UDP.CIPHER_CACHE_LIMIT, AEAD2022.UDP.CIPHER_CACHE_DURATION, (k, v) -> {}));

    private final LruCache<Key, UdpCipher> cache;

    UdpCipherCache(LruCache<Key, UdpCipher> cache) {
        this.cache = cache;
    }

    public UdpCipher get(CipherKind kind, CipherMethod method, byte[] key, long sessionId) {
        return cache.computeIfAbsent(
            new Key(kind, key, sessionId),
            k -> {
                if (kind == CipherKind.aead2022_blake3_chacha20_poly1305) {
                    return new UdpCipher(method, key);
                } else {
                    return new UdpCipher(method, AEAD2022.UDP.sessionSubkey(key, sessionId));
                }
            }
        );
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
