package com.urbanspork.jni;

import java.nio.ByteBuffer;

public interface AeadCipher extends AutoCloseable {
    void encrypt(byte[] nonce, byte[] aad, byte[] plaintext);

    void decrypt(byte[] nonce, byte[] aad, byte[] ciphertext);

    void encryptDirect(byte[] nonce, byte[] aad, ByteBuffer plaintext);

    void decryptDirect(byte[] nonce, byte[] aad, ByteBuffer ciphertext);
}
