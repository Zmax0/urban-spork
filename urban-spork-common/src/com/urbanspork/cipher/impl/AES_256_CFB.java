package com.urbanspork.cipher.impl;

import com.urbanspork.cipher.Cipher;

public class AES_256_CFB extends AbstractShadowsocksCipher {

    public AES_256_CFB() {
        super(Cipher.AES_256_CFB(), Cipher.AES_256_CFB());
    }

    @Override
    public String toString() {
        return "aes-256-cfb";
    }

    @Override
    public int getKeyLength() {
        return 32;
    }

}
