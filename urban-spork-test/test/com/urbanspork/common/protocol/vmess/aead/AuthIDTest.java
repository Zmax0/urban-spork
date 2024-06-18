package com.urbanspork.common.protocol.vmess.aead;

import com.urbanspork.common.protocol.vmess.VMess;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

class AuthIDTest {
    @Test
    void testMatch() {
        byte[] key = KDF.kdf16("Demo Key for Auth ID Test".getBytes(), "Demo Path for Auth ID Test".getBytes());
        byte[] authid = AuthID.createAuthID(key, VMess.now());
        int size = 10000;
        byte[][] keys = new byte[size + 1][];
        for (int i = 0; i < size; i++) {
            keys[i] = KDF.kdf16("Demo Key for Auth ID Test2".getBytes(), "Demo Path for Auth ID Test".getBytes(), String.valueOf(i).getBytes());
        }
        byte[] empty = new byte[0];
        Assertions.assertArrayEquals(empty, AuthID.match(authid, keys));
        keys[size] = key;
        Assertions.assertArrayEquals(key, AuthID.match(authid, keys));
        authid = AuthID.createAuthID(key, VMess.now() + ThreadLocalRandom.current().nextInt() + 120);
        Assertions.assertArrayEquals(empty, AuthID.match(authid, keys));
    }
}
