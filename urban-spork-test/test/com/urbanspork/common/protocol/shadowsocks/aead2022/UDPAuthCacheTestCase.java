package com.urbanspork.common.protocol.shadowsocks.aead2022;

import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.codec.aead.CipherMethod;
import com.urbanspork.common.codec.aead.CipherMethods;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

@DisplayName("Shadowsocks - UDP Auth Cache")
class UDPAuthCacheTestCase {
    @Test
    void testDuration() {
        CipherKind kind = CipherKind.aead2022_blake3_aes_128_gcm;
        CipherMethod method = CipherMethods.AES_GCM.get();
        byte[] key = new byte[]{1};
        long sessionId = 1;
        UdpCipherCache cache = new UdpCipherCache(Duration.ofSeconds(1), 2);
        UdpCipher auth1 = cache.computeIfAbsent(kind, method, key, sessionId);
        UdpCipher auth2 = cache.computeIfAbsent(kind, method, key, sessionId);
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(3));
        UdpCipher auth3 = cache.computeIfAbsent(kind, method, key, sessionId);
        Assertions.assertEquals(auth1, auth2);
        Assertions.assertNotEquals(auth2, auth3);
    }

    @Test
    void testLimit() {
        CipherKind kind = CipherKind.aead2022_blake3_aes_128_gcm;
        CipherMethod method = CipherMethods.AES_GCM.get();
        byte[] key = new byte[]{1};
        UdpCipherCache cache = new UdpCipherCache(Duration.ofHours(1), 2);
        cache.computeIfAbsent(kind, method, key, 1);
        cache.computeIfAbsent(kind, method, key, 2);
        cache.computeIfAbsent(kind, method, key, 3);
        cache.computeIfAbsent(kind, method, key, 4);
        Assertions.assertFalse(cache.contains(kind, key, 1));
        Assertions.assertFalse(cache.contains(kind, key, 2));
        Assertions.assertTrue(cache.contains(kind, key, 3));
        Assertions.assertTrue(cache.contains(kind, key, 4));
    }
}
