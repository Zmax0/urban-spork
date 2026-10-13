package com.urbanspork.jni;

import io.questdb.jar.jni.JarJniLoader;

public abstract class CipherLoader implements AutoCloseable {
    // ptr of cipher for rust
    protected long ptr = 0;
    protected volatile boolean closed;

    protected abstract void dispose(long ptr);

    @Override
    public void close() {
        if (closed) {
            return;
        }
        dispose(ptr);
        closed = true;
    }

    static {
        JarJniLoader.loadLib(
            CipherLoader.class,
            "/native" /* match with <rust-maven-plugin.copyTo> in pom.xml */,
            "ciphers" /* match with [lib.name] in Cargo.toml */
        );
    }
}
