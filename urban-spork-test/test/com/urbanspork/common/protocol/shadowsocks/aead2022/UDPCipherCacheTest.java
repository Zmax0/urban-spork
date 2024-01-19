package com.urbanspork.common.protocol.shadowsocks.aead2022;

import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.test.TestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Shadowsocks - UDP Cipher Cache")
class UDPCipherCacheTest {
    @Test
    void testKey() {
        CipherKind kind = CipherKind.aead2022_blake3_aes_128_gcm;
        byte[] key = new byte[]{1};
        UdpCipherCache.Key k1 = new UdpCipherCache.Key(kind, key, 1);
        UdpCipherCache.Key k2 = new UdpCipherCache.Key(kind, key, 1);
        TestUtil.testEqualsAndHashcode(k1, k2);
        UdpCipherCache.Key k3 = new UdpCipherCache.Key(CipherKind.aead2022_blake3_aes_256_gcm, key, 1);
        UdpCipherCache.Key k4 = new UdpCipherCache.Key(kind, new byte[]{2}, 1);
        UdpCipherCache.Key k5 = new UdpCipherCache.Key(kind, key, 2);
        Assertions.assertNotEquals(k1, k3);
        Assertions.assertNotEquals(k1, k4);
        Assertions.assertNotEquals(k1, k5);
    }
}