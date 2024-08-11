package com.urbanspork.common.crypto;

import org.bouncycastle.crypto.MultiBlockCipher;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.params.KeyParameter;

public class AES {
    private AES() {}

    public static byte[] encrypt(byte[] key, byte[] in, int keyLen) {
        MultiBlockCipher cipher = AESEngine.newInstance();
        byte[] out = new byte[cipher.getBlockSize()];
        cipher.init(true, new KeyParameter(key, 0, keyLen));
        cipher.processBlock(in, 0, out, 0);
        return out;
    }

    public static void encrypt(byte[] key, byte[] in, int keyLen, byte[] out) {
        MultiBlockCipher cipher = AESEngine.newInstance();
        cipher.init(true, new KeyParameter(key, 0, keyLen));
        cipher.processBlock(in, 0, out, 0);
    }

    public static byte[] decrypt(byte[] key, byte[] in, int keyLen) {
        MultiBlockCipher cipher = AESEngine.newInstance();
        byte[] out = new byte[cipher.getBlockSize()];
        cipher.init(false, new KeyParameter(key, 0, keyLen));
        cipher.processBlock(in, 0, out, 0);
        return out;
    }

    public static void decrypt(byte[] key, byte[] in, int keyLen, byte[] out) {
        MultiBlockCipher cipher = AESEngine.newInstance();
        cipher.init(false, new KeyParameter(key, 0, keyLen));
        cipher.processBlock(in, 0, out, 0);
    }
}
