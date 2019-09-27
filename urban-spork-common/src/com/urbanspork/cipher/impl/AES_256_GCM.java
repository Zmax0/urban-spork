package com.urbanspork.cipher.impl;

import com.urbanspork.cipher.Cipher;
import com.urbanspork.cipher.ShadowsocksCipher;

import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.GCMBlockCipher;

public class AES_256_GCM implements ShadowsocksCipher {

    private AEADCipherImpl encrypter = new AEADCipherImpl(new GCMBlockCipher(new AESEngine()), 32, 128);
    private AEADCipherImpl decrypter = new AEADCipherImpl(new GCMBlockCipher(new AESEngine()), 32, 128);

    @Override
    public String getName() {
        return "aes-256-gcm";
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
