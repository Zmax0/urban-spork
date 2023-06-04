package com.urbanspork.common.codec.aead;

import com.urbanspork.common.protocol.vmess.aead.KDF;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

@DisplayName("VMess - AuthID")
class AuthIDTestCase {
    @Test
    void testCreateAuthID() {
        byte[] key = KDF.kdf16("Demo Key for Auth ID Test".getBytes(), "Demo Path for Auth ID Test".getBytes());
        Assertions.assertEquals("ZuQa1H+nRfv9HpcyXpPb9A==", Base64.getEncoder().encodeToString(key));
    }
}
