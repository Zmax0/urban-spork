package com.urbanspork.common.cipher.impl;

import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.GCMBlockCipher;

import com.urbanspork.common.cipher.Cipher;
import com.urbanspork.common.cipher.ShadowsocksCipher;

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
