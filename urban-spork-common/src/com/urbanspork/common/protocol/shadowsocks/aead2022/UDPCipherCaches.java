package com.urbanspork.common.protocol.shadowsocks.aead2022;

import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.codec.aead.CipherMethod;
import com.urbanspork.common.codec.shadowsocks.UdpCipher;

public enum UDPCipherCaches {
    INSTANCE(new UDPCipherCache(AEAD2022.UDP.CIPHER_CACHE_DURATION, AEAD2022.UDP.CIPHER_CACHE_LIMIT));

    private final UDPCipherCache cache;

    UDPCipherCaches(UDPCipherCache cache) {
        this.cache = cache;
    }

    public UdpCipher get(CipherKind kind, CipherMethod method, byte[] key, long sessionId) {
        return cache.computeIfAbsent(kind, method, key, sessionId);
    }
}
