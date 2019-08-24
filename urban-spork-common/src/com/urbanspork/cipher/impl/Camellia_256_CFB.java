package com.urbanspork.cipher.impl;

import com.urbanspork.cipher.Cipher;
import com.urbanspork.cipher.ShadowsocksCipher;

import org.bouncycastle.crypto.engines.CamelliaEngine;
import org.bouncycastle.crypto.modes.CFBBlockCipher;

public class Camellia_256_CFB implements ShadowsocksCipher {

    private StreamCiphers encrypter = new StreamCiphers(new CFBBlockCipher(new CamelliaEngine(), 128), 16);
    private StreamCiphers decrypter = new StreamCiphers(new CFBBlockCipher(new CamelliaEngine(), 128), 16);

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
        return "camellia-256-cfb";
    }

}