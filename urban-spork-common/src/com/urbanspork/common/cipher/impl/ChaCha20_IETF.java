package com.urbanspork.common.cipher.impl;

import org.bouncycastle.crypto.engines.ChaCha7539Engine;

import com.urbanspork.common.cipher.Cipher;
import com.urbanspork.common.cipher.ShadowsocksCipher;

public class ChaCha20_IETF implements ShadowsocksCipher {

    private Cipher encrypter = new StreamCipherImpl(new ChaCha7539Engine(), 12);
    private Cipher decrypter = new StreamCipherImpl(new ChaCha7539Engine(), 12);

    @Override
    public String getName() {
        return "chacha20-ietf";
    }

    @Override
    public Cipher encrypter() {
        return encrypter;
    }

    @Override
    public Cipher decrypter() {
        return decrypter;
    }

    @Override
    public int getKeySize() {
        return 32;
    }

}
