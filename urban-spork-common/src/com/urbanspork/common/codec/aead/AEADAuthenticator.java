package com.urbanspork.common.codec.aead;

import com.urbanspork.common.codec.BytesGenerator;
import com.urbanspork.common.codec.NonceGenerator;
import org.bouncycastle.crypto.InvalidCipherTextException;

public record AEADAuthenticator(AEADCipherCodec codec, byte[] key,
                                NonceGenerator nonceGenerator,
                                BytesGenerator associatedTextGenerator) {

    public AEADAuthenticator(AEADCipherCodec codec, byte[] key, byte[] nonce) {
        this(codec, key, NonceGenerator.generateCountingNonce(nonce, codec.nonceSize()), BytesGenerator.generateEmptyBytes());
    }

    public byte[] seal(byte[] in) throws InvalidCipherTextException {
        return codec.encrypt(key, nonceGenerator.generate(), associatedTextGenerator.generate(), in);
    }

    public byte[] open(byte[] in) throws InvalidCipherTextException {
        return codec.decrypt(key, nonceGenerator.generate(), associatedTextGenerator.generate(), in);
    }

}
