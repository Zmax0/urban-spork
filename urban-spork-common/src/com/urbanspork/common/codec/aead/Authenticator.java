package com.urbanspork.common.codec.aead;

import com.urbanspork.common.codec.BytesGenerator;
import com.urbanspork.common.codec.NonceGenerator;
import org.bouncycastle.crypto.InvalidCipherTextException;

import java.util.Arrays;

public record Authenticator(byte[] key, CipherMethod method, NonceGenerator nonceGenerator, BytesGenerator associatedTextGenerator) {
    public Authenticator(byte[] key, CipherMethod method, NonceGenerator nonceGenerator, BytesGenerator associatedTextGenerator) {
        int keySize = method.keySize();
        this.key = keySize == key.length ? key : Arrays.copyOf(key, keySize);
        this.method = method;
        this.nonceGenerator = nonceGenerator;
        this.associatedTextGenerator = associatedTextGenerator;
    }

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
