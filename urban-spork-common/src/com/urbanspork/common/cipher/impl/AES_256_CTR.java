package com.urbanspork.common.cipher.impl;

import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.SICBlockCipher;

import com.urbanspork.common.cipher.Cipher;
import com.urbanspork.common.cipher.ShadowsocksCipher;

public class AES_256_CTR implements ShadowsocksCipher {

    private StreamCipherImpl encrypter = new StreamCipherImpl(new SICBlockCipher(new AESEngine()), 16);
    private StreamCipherImpl decrypter = new StreamCipherImpl(new SICBlockCipher(new AESEngine()), 16);

    @Override
    public String getName() {
        return "aes-256-ctr";
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