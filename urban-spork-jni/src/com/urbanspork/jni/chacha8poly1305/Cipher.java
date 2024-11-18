package com.urbanspork.jni.chacha8poly1305;

import com.urbanspork.jni.CipherLoader;

public class Cipher extends CipherLoader {
    private Cipher() {}

    public static native Cipher init(byte[] key);

    public native void encrypt(byte[] nonce, byte[] aad, byte[] plaintext);

    public native void decrypt(byte[] nonce, byte[] aad, byte[] ciphertext);
}
