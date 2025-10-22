package com.urbanspork.jni.xchacha20poly1305;

import com.urbanspork.jni.CipherLoader;

public class Cipher extends CipherLoader {
    private Cipher() {}

    public static Cipher newInstance(byte[] key) {
        Cipher cipher = init(key);
        long p = cipher.ptr;
        CLEANER.register(cipher, () -> dispose(p));
        return cipher;
    }

    static native Cipher init(byte[] key);

    static native void dispose(long ptr);

    @Override
    public native void encrypt(byte[] nonce, byte[] aad, byte[] plaintext);

    @Override
    public native void decrypt(byte[] nonce, byte[] aad, byte[] ciphertext);
}
