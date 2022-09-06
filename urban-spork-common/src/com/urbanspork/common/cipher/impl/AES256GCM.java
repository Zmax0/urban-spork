package com.urbanspork.common.cipher.impl;

import com.urbanspork.common.cipher.Cipher;
import com.urbanspork.common.cipher.ShadowsocksCipher;
import com.urbanspork.common.cipher.base.BaseAEADCipher;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.GCMBlockCipher;

public class AES256GCM implements ShadowsocksCipher {

    private final Cipher encryptCipher = new BaseAEADCipher(new GCMBlockCipher(new AESEngine()), 32, 128);
    private final Cipher decryptCipher = new BaseAEADCipher(new GCMBlockCipher(new AESEngine()), 32, 128);

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
        return 32;
    }

}
