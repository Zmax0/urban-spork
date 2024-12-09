package com.urbanspork.jni;

public interface AeadCipher {
    void encrypt(byte[] nonce, byte[] aad, byte[] plaintext);

    void decrypt(byte[] nonce, byte[] aad, byte[] ciphertext);
}
