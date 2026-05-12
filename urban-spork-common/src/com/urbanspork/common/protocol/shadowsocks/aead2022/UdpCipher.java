package com.urbanspork.common.protocol.shadowsocks.aead2022;

import com.urbanspork.common.codec.aead.CipherInstance;
import com.urbanspork.common.codec.aead.CipherMethod;
import org.jspecify.annotations.Nullable;

public class UdpCipher implements AutoCloseable {
    private final byte[] key;
    private final CipherMethod method;
    @Nullable
    private final CipherInstance cache;

    public UdpCipher(CipherMethod method, byte[] key) {
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

    public byte[] seal(byte[] in, byte[] nonce) throws Exception {
        if (cache == null) {
            try (CipherInstance instance = method.init(key)) {
                return instance.encrypt(nonce, null, in);
            }
        } else {
            return cache.encrypt(nonce, null, in);
        }
    }

    public byte[] open(byte[] in, byte[] nonce) throws Exception {
        if (cache == null) {
            try (CipherInstance instance = method.init(key)) {
                return instance.decrypt(nonce, null, in);
            }
        } else {
            return cache.decrypt(nonce, null, in);
        }
    }

    @Override
    public void close() throws Exception {
        if (cache != null) {
            cache.close();
        }
    }
}
