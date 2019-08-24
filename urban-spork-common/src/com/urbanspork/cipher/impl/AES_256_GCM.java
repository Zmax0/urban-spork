package com.urbanspork.cipher.impl;

import com.urbanspork.cipher.Cipher;
import com.urbanspork.cipher.ShadowsocksCipher;

import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.GCMBlockCipher;

public class AES_256_GCM implements ShadowsocksCipher {

    private AEADBlockCiphers encrypter = new AEADBlockCiphers(new GCMBlockCipher(new AESEngine()), 32, 128);
    private AEADBlockCiphers decrypter = new AEADBlockCiphers(new GCMBlockCipher(new AESEngine()), 32, 128);

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
        return "aes-256-gcm";
    }

}
