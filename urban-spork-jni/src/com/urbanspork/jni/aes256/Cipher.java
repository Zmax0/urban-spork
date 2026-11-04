package com.urbanspork.jni.aes256;

import com.urbanspork.jni.CipherLoader;

public class Cipher extends CipherLoader {
    private Cipher() {}

    public static Cipher newInstance(byte[] key) {
        return init(key);
    }

    static native Cipher init(byte[] key);

    @Override
    protected native void dispose(long ptr);

    public void encrypt(byte[] block) {
        encrypt0(ptr, block);
    }

    public void decrypt(byte[] block) {
        decrypt0(ptr, block);
    }

    private native void encrypt0(long ptr, byte[] block);

    private native void decrypt0(long ptr, byte[] block);
}
