package com.urbanspork.common.protocol.vmess.aead;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;

@DisplayName("VMess - KDF")
class KDFTestCase {

    @Test
    void testKdf16() {
        byte[] key = KDF.kdf16("Demo Key for Auth ID Test".getBytes(), "Demo Path for Auth ID Test".getBytes());
        Assertions.assertEquals("ZuQa1H+nRfv9HpcyXpPb9A==", Base64.getEncoder().encodeToString(key));
    }

    @Test
    void testKdf() {
        byte[] key = KDF.kdf("Demo Key for Auth ID Test".getBytes(), ThreadLocalRandom.current().nextInt(32, Integer.MAX_VALUE));
        Assertions.assertEquals("e50sLh+rC0B6LsALqzcblmfKNfZnQIbvOEJRgh9gBfg=", Base64.getEncoder().encodeToString(key));
    }
}
