package com.urbanspork.common.protocol.vmess.encoding;

import com.urbanspork.common.crypto.GeneralDigests;

import java.util.Arrays;

public class Auth {

    public static byte[] generateChacha20Poly1305Key(byte[] raw) {
        byte[] key = new byte[32];
        GeneralDigests.md5.get(raw, key, 0);
        GeneralDigests.md5.get(Arrays.copyOf(key, 16), key, 16);
        return key;
    }

}
