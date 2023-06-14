package com.urbanspork.common.protocol.vmess.aead;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

@DisplayName("VMess - KDF")
class KDFTestCase {
    @Test
    void testKdf16() {
        byte[] key = KDF.kdf16("Demo Key for Auth ID Test".getBytes(), "Demo Path for Auth ID Test".getBytes());
        Assertions.assertEquals("ZuQa1H+nRfv9HpcyXpPb9A==", Base64.getEncoder().encodeToString(key));
    }
}
