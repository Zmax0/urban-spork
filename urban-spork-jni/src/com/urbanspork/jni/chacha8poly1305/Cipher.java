package com.urbanspork.jni.chacha8poly1305;

import com.urbanspork.jni.CipherLoader;

public class Cipher extends CipherLoader {
    private Cipher() {}

    public static Cipher newInstance(byte[] key) {
        return init(key);
    }

    static native Cipher init(byte[] key);

    static native void dispose(long ptr);

    @Override
    public native void encrypt(byte[] nonce, byte[] aad, byte[] plaintext);

    @Override
    public native void decrypt(byte[] nonce, byte[] aad, byte[] ciphertext);

    @Override
    public void close() {
        if (closed) {
            return;
        }
        dispose(ptr);
        closed = true;
    }
}
