package com.urbanspork.common.protocol.shadowsocks.aead2022;

import com.urbanspork.common.codec.aead.CipherMethod;
import org.bouncycastle.crypto.InvalidCipherTextException;

public record UdpCipher(CipherMethod method, byte[] key) {
    public byte[] seal(byte[] in, byte[] nonce) throws InvalidCipherTextException {
        return method.encrypt(key, nonce, in);
    }

    public byte[] open(byte[] in, byte[] nonce) throws InvalidCipherTextException {
        return method.decrypt(key, nonce, in);
    }
}
