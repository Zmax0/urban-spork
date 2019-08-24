package com.urbanspork.cipher.impl;

import com.urbanspork.cipher.Cipher;
import com.urbanspork.cipher.ShadowsocksCipher;

import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.SICBlockCipher;

public class AES_256_CTR implements ShadowsocksCipher {

    private StreamCiphers encrypter = new StreamCiphers(new SICBlockCipher(new AESEngine()), 16);
    private StreamCiphers decrypter = new StreamCiphers(new SICBlockCipher(new AESEngine()), 16);

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

    @Override
    public String toString() {
        return "aes-256-ctr";
    }

}