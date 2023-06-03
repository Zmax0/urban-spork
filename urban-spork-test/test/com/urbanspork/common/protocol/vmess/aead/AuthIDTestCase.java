package com.urbanspork.common.protocol.vmess.aead;

import com.urbanspork.common.protocol.vmess.VMess;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.security.InvalidKeyException;

class AuthIDTestCase {

    @Test
    void testMatch() throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        byte[] key = KDF.kdf16("Demo Key for Auth ID Test".getBytes(), "Demo Path for Auth ID Test".getBytes());
        byte[] authid = AuthID.createAuthID(key, VMess.now());
        byte[][] keys = new byte[10001][];
        for (int i = 0; i < 10000; i++) {
            keys[0] = KDF.kdf16("Demo Key for Auth ID Test2".getBytes(), "Demo Path for Auth ID Test".getBytes(), String.valueOf(i).getBytes());
        }
        keys[10000] = key;
        Assertions.assertArrayEquals(key, AuthID.match(authid, keys));
    }

}
