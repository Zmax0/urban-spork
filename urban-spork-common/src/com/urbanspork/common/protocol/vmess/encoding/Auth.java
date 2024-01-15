package com.urbanspork.common.protocol.vmess.encoding;

import com.urbanspork.common.crypto.Digests;

import java.util.Arrays;

public class Auth {
    private Auth() {}

    public static byte[] generateChacha20Poly1305Key(byte[] raw) {
        byte[] key = new byte[32];
        Digests.md5.hash(raw, key, 0);
        Digests.md5.hash(Arrays.copyOf(key, 16), key, 16);
        return key;
    }
}
