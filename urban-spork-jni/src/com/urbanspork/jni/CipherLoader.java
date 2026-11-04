package com.urbanspork.jni;

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
        NativeLibrary.load();
    }
}
