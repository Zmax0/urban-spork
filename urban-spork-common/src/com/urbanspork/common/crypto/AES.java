package com.urbanspork.common.crypto;

import org.bouncycastle.crypto.MultiBlockCipher;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.params.KeyParameter;

public enum AES {

    INSTANCE(AESEngine.newInstance());

    private final MultiBlockCipher cipher;

    AES(MultiBlockCipher cipher) {
        this.cipher = cipher;
    }

    public byte[] encrypt(byte[] key, byte[] in) {
        byte[] out = new byte[cipher.getBlockSize()];
        cipher.init(true, new KeyParameter(key));
        cipher.processBlock(in, 0, out, 0);
        return out;
    }

    public void encrypt(byte[] key, byte[] in, byte[] out) {
        cipher.init(true, new KeyParameter(key));
        cipher.processBlock(in, 0, out, 0);
    }

    public byte[] decrypt(byte[] key, byte[] in) {
        byte[] out = new byte[cipher.getBlockSize()];
        cipher.init(false, new KeyParameter(key));
        cipher.processBlock(in, 0, out, 0);
        return out;
    }

    public void decrypt(byte[] key, byte[] in, byte[] out) {
        cipher.init(false, new KeyParameter(key));
        cipher.processBlock(in, 0, out, 0);
    }
}
