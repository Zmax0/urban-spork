package com.urbanspork.jni;

import java.nio.ByteBuffer;

public abstract class AeadCipherLoader extends CipherLoader implements AeadCipher {
    @Override
    public void encrypt(byte[] nonce, byte[] aad, byte[] plaintext) {
        encrypt0(ptr, nonce, aad, plaintext);
    }

    @Override
    public void decrypt(byte[] nonce, byte[] aad, byte[] ciphertext) {
        decrypt0(ptr, nonce, aad, ciphertext);
    }

    @Override
    public void encryptDirect(byte[] nonce, byte[] aad, ByteBuffer plaintext) {
        encryptDirect0(ptr, nonce, aad, requireRegion(plaintext, "plaintext"));
    }

    @Override
    public void decryptDirect(byte[] nonce, byte[] aad, ByteBuffer ciphertext) {
        decryptDirect0(ptr, nonce, aad, requireRegion(ciphertext, "ciphertext"));
    }

    private static ByteBuffer requireRegion(ByteBuffer buffer, String name) {
        if (buffer == null || !buffer.isDirect()) {
            throw new IllegalArgumentException(name + " must be a direct ByteBuffer");
        }
        if (buffer.position() != 0 || buffer.limit() != buffer.capacity()) {
            String msg = String.format("%s must be a slice with [0, capacity) as the payload, but found %s", name, buffer);
            throw new IllegalArgumentException(msg);
        }
        return buffer;
    }

    protected abstract void encrypt0(long ptr, byte[] nonce, byte[] aad, byte[] plaintext);

    protected abstract void decrypt0(long ptr, byte[] nonce, byte[] aad, byte[] ciphertext);

    protected abstract void encryptDirect0(long ptr, byte[] nonce, byte[] aad, ByteBuffer plaintext);

    protected abstract void decryptDirect0(long ptr, byte[] nonce, byte[] aad, ByteBuffer ciphertext);
}
