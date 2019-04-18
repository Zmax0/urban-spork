package com.urbanspork.cipher;

import java.security.SecureRandom;

import org.bouncycastle.crypto.StreamCipher;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.engines.ChaCha7539Engine;
import org.bouncycastle.crypto.modes.CFBBlockCipher;
import org.bouncycastle.crypto.modes.GCMBlockCipher;

import com.urbanspork.cipher.impl.AEADBlockCiphers;
import com.urbanspork.cipher.impl.StreamCiphers;

public interface Cipher {

    byte[] encrypt(byte[] in, byte[] key) throws Exception;

    byte[] decrypt(byte[] in, byte[] key) throws Exception;

    default byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    static StreamCiphers CHACHA20_IETF() {
        StreamCipher chaCha7539Engine = new ChaCha7539Engine();
        return new StreamCiphers(chaCha7539Engine, 12);
    }

    static StreamCiphers AES_256_CFB() {
        CFBBlockCipher cipher = new CFBBlockCipher(new AESEngine(), 128);
        return new StreamCiphers(cipher, 16);
    }

    static AEADBlockCiphers AES_256_GCM() {
        GCMBlockCipher cipher = new GCMBlockCipher(new AESEngine());
        return new AEADBlockCiphers(cipher, 32, 128);
    }
}
