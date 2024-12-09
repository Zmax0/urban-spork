package com.urbanspork.common.protocol.shadowsocks.aead2022;

import com.urbanspork.common.codec.aead.CipherInstance;
import com.urbanspork.common.codec.aead.CipherMethod;
import org.bouncycastle.crypto.InvalidCipherTextException;

public class UdpCipher {
    private final byte[] key;
    private final CipherMethod method;
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

    public byte[] seal(byte[] in, byte[] nonce) throws InvalidCipherTextException {
        return (cache == null ? method.init(key) : cache).encrypt(nonce, null, in);
    }

    public byte[] open(byte[] in, byte[] nonce) throws InvalidCipherTextException {
        return (cache == null ? method.init(key) : cache).decrypt(nonce, null, in);
    }
}
