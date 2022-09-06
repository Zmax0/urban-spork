package com.urbanspork.common.cipher.impl;

import com.urbanspork.common.cipher.Cipher;
import com.urbanspork.common.cipher.ShadowsocksCipher;
import com.urbanspork.common.cipher.base.BaseAEADCipher;
import org.bouncycastle.crypto.modes.ChaCha20Poly1305;

public class ChaCha20IETFPoly1305 implements ShadowsocksCipher {

    private final Cipher encryptCipher = new BaseAEADCipher(new ChaCha20Poly1305(), 32, 128);
    private final Cipher decryptCipher = new BaseAEADCipher(new ChaCha20Poly1305(), 32, 128);

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
