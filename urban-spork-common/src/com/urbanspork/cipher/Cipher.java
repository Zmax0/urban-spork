package com.urbanspork.cipher;

import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CFBBlockCipher;

public interface Cipher {

    byte[] encrypt(byte[] in, byte[] key) throws Exception;

    byte[] decrypt(byte[] in, byte[] key) throws Exception;

    static class StreamBlockCiphers extends AbstractStreamBlockCipher {

        private StreamBlockCiphers() {

        }

        public static StreamBlockCiphers AES_256_CFB() {
            StreamBlockCiphers ciphers = new StreamBlockCiphers();
            ciphers.ivl = 16;
            ciphers.cipher = new CFBBlockCipher(new AESEngine(), 128);
            return ciphers;
        }

    }

}
