package com.urbanspork.cipher.impl;

import com.urbanspork.cipher.Cipher;
import com.urbanspork.cipher.ShadowsocksCipher;

import org.bouncycastle.crypto.engines.CamelliaEngine;
import org.bouncycastle.crypto.modes.CFBBlockCipher;

public class Camellia_256_CFB implements ShadowsocksCipher {

    private StreamCipherImpl encrypter = new StreamCipherImpl(new CFBBlockCipher(new CamelliaEngine(), 128), 16);
    private StreamCipherImpl decrypter = new StreamCipherImpl(new CFBBlockCipher(new CamelliaEngine(), 128), 16);

    @Override
    public String getName() {
        return "camellia-256-cfb";
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