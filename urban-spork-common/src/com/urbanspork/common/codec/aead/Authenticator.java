package com.urbanspork.common.codec.aead;

import com.urbanspork.common.codec.BytesGenerator;
import com.urbanspork.common.codec.NonceGenerator;
import org.bouncycastle.crypto.InvalidCipherTextException;

import java.util.Arrays;

public record Authenticator(CipherMethod method, CipherInstance instance, NonceGenerator nonceGenerator, BytesGenerator associatedTextGenerator) {
    public Authenticator(byte[] key, CipherMethod method, NonceGenerator nonceGenerator, BytesGenerator associatedTextGenerator) {
        this(method, method.init(method.keySize() == key.length ? key : Arrays.copyOf(key, method.keySize())), nonceGenerator, associatedTextGenerator);
    }

    public int overhead() {
        return method.tagSize();
    }

    public byte[] seal(byte[] in) throws InvalidCipherTextException {
        return instance.encrypt(nonceGenerator.generate(), associatedTextGenerator.generate(), in);
    }

    public byte[] open(byte[] in) throws InvalidCipherTextException {
        return instance.decrypt(nonceGenerator.generate(), associatedTextGenerator.generate(), in);
    }
}
