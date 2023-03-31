package com.urbanspork.common.protocol.vmess.encoding;

import com.urbanspork.common.crypto.GeneralDigests;

public class Auth {

    public static byte[] generateChacha20Poly1305Key(byte[] raw) {
        byte[] key = new byte[32];
        byte[] temp = GeneralDigests.md5.get(raw);
        System.arraycopy(temp, 0, key, 0, temp.length);
        temp = GeneralDigests.md5.get(temp);
        System.arraycopy(temp, 0, key, 16, temp.length);
        return key;
    }

}
