package com.urbanspork.common.cipher.impl;

import org.bouncycastle.crypto.modes.ChaCha20Poly1305;

import com.urbanspork.common.cipher.Cipher;
import com.urbanspork.common.cipher.ShadowsocksCipher;

public class ChaCha20_IETF_Poly1305 implements ShadowsocksCipher {

    private Cipher encrypter = new AEADCipherImpl(new ChaCha20Poly1305(), 32, 128);
    private Cipher decrypter = new AEADCipherImpl(new ChaCha20Poly1305(), 32, 128);

    @Override
    public String getName() {
        return "chacha20-ietf-poly1305";
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
        return 32;
    }

}
