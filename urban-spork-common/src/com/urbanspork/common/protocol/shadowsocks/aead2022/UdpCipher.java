package com.urbanspork.common.protocol.shadowsocks.aead2022;

import com.urbanspork.common.codec.aead.CipherInstance;
import com.urbanspork.common.codec.aead.CipherMethod;
import org.jspecify.annotations.Nullable;

public class UdpCipher implements AutoCloseable {
    private final byte[] key;
    private final CipherMethod method;
    private final int overhead;
    @Nullable
    private final CipherInstance cache;

    public UdpCipher(CipherMethod method, byte[] key) {
        this.overhead = method.tagSize();
        if (CipherMethod.XCHACHA8_POLY1305 == method || CipherMethod.XCHACHA20_POLY1305 == method) {
            this.method = null;
            this.key = null;
            this.cache = method.init(key);
        } else {
            this.method = method;
            this.key = key;
            this.cache = null;
        }
    }

    public int overhead() {
        return overhead;
    }

    public void sealInPlace(byte[] buffer, byte[] nonce) throws Exception {
        if (cache == null) {
            try (CipherInstance instance = method.init(key)) {
                instance.encrypt(nonce, null, buffer);
            }
        } else {
            cache.encrypt(nonce, null, buffer);
        }
    }

    public int openInPlace(byte[] buffer, byte[] nonce) throws Exception {
        if (cache == null) {
            try (CipherInstance instance = method.init(key)) {
                return instance.decrypt(nonce, null, buffer);
            }
        } else {
            return cache.decrypt(nonce, null, buffer);
        }
    }

    @Override
    public void close() throws Exception {
        if (cache != null) {
            cache.close();
        }
    }
}
