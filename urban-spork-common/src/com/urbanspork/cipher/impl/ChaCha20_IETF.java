package com.urbanspork.cipher.impl;

import com.urbanspork.cipher.Cipher;
import com.urbanspork.cipher.ShadowsocksCipher;

import org.bouncycastle.crypto.engines.ChaCha7539Engine;

public class ChaCha20_IETF implements ShadowsocksCipher {

    private StreamCipherImpl encrypter = new StreamCipherImpl(new ChaCha7539Engine(), 12);
    private StreamCipherImpl decrypter = new StreamCipherImpl(new ChaCha7539Engine(), 12);

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
    public int getKeyLength() {
        return 32;
    }

}
