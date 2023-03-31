package com.urbanspork.common.codec.aead;

import com.urbanspork.common.protocol.vmess.aead.AuthID;
import com.urbanspork.common.protocol.vmess.aead.KDF;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Base64;

public class AuthIDTestCase {

    @Test
    public void testCreateAuthID() {
        byte[] key = createKey();
        Assertions.assertEquals("ZuQa1H+nRfv9HpcyXpPb9A==", Base64.getEncoder().encodeToString(key));
    }

    @Test
    public void testCreateAuthIDAndMatch() throws Exception {
        byte[] key = createKey();
        byte[] authID = AuthID.createAuthID(key, Instant.now().getEpochSecond());
        Assertions.assertTrue(AuthID.match(authID, key));
    }

    public static byte[] createKey() {
        return KDF.kdf16("Demo Key for Auth ID Test".getBytes(), "Demo Path for Auth ID Test".getBytes());
    }
}
