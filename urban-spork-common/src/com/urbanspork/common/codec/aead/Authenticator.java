package com.urbanspork.common.codec.aead;

import com.urbanspork.common.codec.BytesGenerator;
import com.urbanspork.common.codec.NonceGenerator;
import org.bouncycastle.crypto.InvalidCipherTextException;

public record Authenticator(CipherMethod method, byte[] key, NonceGenerator nonceGenerator, BytesGenerator associatedTextGenerator) {
    public int overhead() {
        return method.tagSize();
    }

    public byte[] seal(byte[] in) throws InvalidCipherTextException {
        return method.encrypt(key, nonceGenerator.generate(), associatedTextGenerator.generate(), in);
    }

    public byte[] open(byte[] in) throws InvalidCipherTextException {
        return method.decrypt(key, nonceGenerator.generate(), associatedTextGenerator.generate(), in);
    }
}
