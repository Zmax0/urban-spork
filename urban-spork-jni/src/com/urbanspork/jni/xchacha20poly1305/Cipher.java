package com.urbanspork.jni.xchacha20poly1305;

import io.questdb.jar.jni.JarJniLoader;

public class Cipher {
    static {
        JarJniLoader.loadLib(
            Cipher.class,
            "/native" /* match with <rust-maven-plugin.copyTo> in pom.xml */,
            "xchacha20poly1305" /* match with [lib.name] in Cargo.toml */
        );
    }

    // ptr of cipher for rust
    @SuppressWarnings("unused")
    long ptr = 0;

    private Cipher() {}

    public static native Cipher init(byte[] key);

    public native void encrypt(byte[] nonce, byte[] aad, byte[] plaintext);

    public native void decrypt(byte[] nonce, byte[] aad, byte[] ciphertext);
}
