package com.urbanspork.cipher.impl;

import com.urbanspork.cipher.AbstractShadowsocksCipher;
import com.urbanspork.cipher.Cipher;

public class AES_256_GCM extends AbstractShadowsocksCipher {

    public AES_256_GCM() {
        super(Cipher.AES_256_GCM(), Cipher.AES_256_GCM());
    }

    @Override
    public String toString() {
        return "aes-256-gcm";
    }

    @Override
    public int getKeyLength() {
        return 32;
    }

}
