package com.urbanspork.common.codec.aead;

import org.bouncycastle.crypto.InvalidCipherTextException;

public interface CipherInstance extends AutoCloseable {
    byte[] encrypt(byte[] nonce, byte[] aad, byte[] in) throws InvalidCipherTextException;

    byte[] decrypt(byte[] nonce, byte[] aad, byte[] in) throws InvalidCipherTextException;
}
