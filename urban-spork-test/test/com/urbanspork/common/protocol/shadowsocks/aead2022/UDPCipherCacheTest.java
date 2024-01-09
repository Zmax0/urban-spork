package com.urbanspork.common.protocol.shadowsocks.aead2022;

import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.test.TestUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Shadowsocks - UDP Cipher Cache")
class UDPCipherCacheTest {
    @Test
    void testKey() {
        CipherKind kind = CipherKind.aead2022_blake3_aes_128_gcm;
        byte[] key = new byte[]{1};
        UDPCipherCache.Key k1 = new UDPCipherCache.Key(kind, key, 1);
        UDPCipherCache.Key k2 = new UDPCipherCache.Key(kind, key, 1);
        TestUtil.testEqualsAndHashcode(k1, k2);
    }
}