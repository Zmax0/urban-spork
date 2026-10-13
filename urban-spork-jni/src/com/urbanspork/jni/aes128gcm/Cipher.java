package com.urbanspork.jni.aes128gcm;

import com.urbanspork.jni.AeadCipherLoader;

import java.nio.ByteBuffer;

public class Cipher extends AeadCipherLoader {
    private Cipher() {}

    public static Cipher newInstance(byte[] key) {
        return init(key);
    }

    static native Cipher init(byte[] key);

    @Override
    protected native void dispose(long ptr);

    @Override
    protected native void encrypt0(long ptr, byte[] nonce, byte[] aad, byte[] plaintext);

    @Override
    protected native void decrypt0(long ptr, byte[] nonce, byte[] aad, byte[] ciphertext);

    @Override
    protected native void encryptDirect0(long ptr, byte[] nonce, byte[] aad, ByteBuffer plaintext);

    @Override
    protected native void decryptDirect0(long ptr, byte[] nonce, byte[] aad, ByteBuffer ciphertext);
}
