package com.urbanspork.jni;

public interface AeadCipher extends AutoCloseable {
    void encrypt(byte[] nonce, byte[] aad, byte[] plaintext);

    void decrypt(byte[] nonce, byte[] aad, byte[] ciphertext);
}
