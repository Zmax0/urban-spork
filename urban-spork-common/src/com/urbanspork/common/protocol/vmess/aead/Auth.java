package com.urbanspork.common.protocol.vmess.aead;

import com.urbanspork.common.protocol.vmess.VMess;

public class Auth {

    public static byte[] generateChacha20Poly1305Key(byte[] raw) {
        byte[] key = new byte[32];
        byte[] temp = VMess.md5(raw);
        System.arraycopy(temp, 0, key, 0, temp.length);
        temp = VMess.md5(temp);
        System.arraycopy(temp, 0, key, 16, temp.length);
        return key;
    }

}
