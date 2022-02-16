package com.urbanspork.common.cipher.impl;

import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.GCMBlockCipher;

import com.urbanspork.common.cipher.Cipher;
import com.urbanspork.common.cipher.ShadowsocksCipher;
import com.urbanspork.common.cipher.base.BaseAEADCipher;

public class AES_192_GCM implements ShadowsocksCipher {

    private final Cipher encryptCipher = new BaseAEADCipher(new GCMBlockCipher(new AESEngine()), 24, 128);
    private final Cipher decryptCipher = new BaseAEADCipher(new GCMBlockCipher(new AESEngine()), 24, 128);

    @Override
    public Cipher encryptCipher() {
        return encryptCipher;
    }

    @Override
    public Cipher decryptCipher() {
        return decryptCipher;
    }

    @Override
    public int getKeySize() {
        return 24;
    }

}
