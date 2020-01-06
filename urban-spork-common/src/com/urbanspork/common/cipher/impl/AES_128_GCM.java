package com.urbanspork.common.cipher.impl;

import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.GCMBlockCipher;

import com.urbanspork.common.cipher.Cipher;
import com.urbanspork.common.cipher.ShadowsocksCipher;
import com.urbanspork.common.cipher.base.BaseAEADCipher;

public class AES_128_GCM implements ShadowsocksCipher {

    private Cipher encrypter = new BaseAEADCipher(new GCMBlockCipher(new AESEngine()), 16, 128);
    private Cipher decrypter = new BaseAEADCipher(new GCMBlockCipher(new AESEngine()), 16, 128);

    @Override
    public String getName() {
        return "aes-128-gcm";
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
        return 16;
    }

}
