package com.urbanspork.cipher.impl;

import com.urbanspork.cipher.Cipher;

public class ChaCha20_IETF extends AbstractShadowsocksCipher {

    public ChaCha20_IETF() {
        super(Cipher.CHACHA20_IETF(), Cipher.CHACHA20_IETF());
    }

    @Override
    public String toString() {
        return "chacha20-ietf";
    }

    @Override
    public int getKeyLength() {
        return 32;
    }

}
