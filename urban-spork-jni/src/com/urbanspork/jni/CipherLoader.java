package com.urbanspork.jni;

import io.questdb.jar.jni.JarJniLoader;

import java.lang.ref.Cleaner;

public abstract class CipherLoader implements AeadCipher {
    // ptr of cipher for rust
    @SuppressWarnings("unused")
    protected long ptr = 0;

    static {
        JarJniLoader.loadLib(
            CipherLoader.class,
            "/native" /* match with <rust-maven-plugin.copyTo> in pom.xml */,
            "ciphers" /* match with [lib.name] in Cargo.toml */
        );
    }

    protected static final Cleaner CLEANER = Cleaner.create();
}
