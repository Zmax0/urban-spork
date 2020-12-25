package com.urbanspork.common.cipher.impl;

import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CFBBlockCipher;

import com.urbanspork.common.cipher.Cipher;
import com.urbanspork.common.cipher.ShadowsocksCipher;
import com.urbanspork.common.cipher.base.BaseStreamCipher;

public class AES_256_CFB implements ShadowsocksCipher {

    private final Cipher encrypter = new BaseStreamCipher(new CFBBlockCipher(new AESEngine(), 128), 16);
    private final Cipher decrypter = new BaseStreamCipher(new CFBBlockCipher(new AESEngine(), 128), 16);

    @Override
    public String getName() {
        return "aes-256-cfb";
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
