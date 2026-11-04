package com.urbanspork.common.codec.aead;

import com.urbanspork.common.codec.BytesGenerator;
import com.urbanspork.common.codec.NonceGenerator;
import io.netty.buffer.ByteBuf;

public record Authenticator(CipherMethod method, CipherInstance instance, NonceGenerator nonceGenerator, BytesGenerator associatedTextGenerator) implements AutoCloseable {
    public Authenticator(byte[] key, CipherMethod method, NonceGenerator nonceGenerator, BytesGenerator associatedTextGenerator) {
        this(method, method.init(key), nonceGenerator, associatedTextGenerator);
    }

    public int overhead() {
        return method.tagSize();
    }

    public byte[] seal(byte[] in) {
        return instance.encryptTo(nonceGenerator.generate(), associatedTextGenerator.generate(), in);
    }

    public byte[] open(byte[] in) {
        return instance.decryptTo(nonceGenerator.generate(), associatedTextGenerator.generate(), in);
    }

    public void seal(ByteBuf dst, int index, int length) {
        instance.encrypt(nonceGenerator.generate(), associatedTextGenerator.generate(), dst, index, length);
    }

    public ByteBuf open(ByteBuf src, int index, int length) {
        return instance.decrypt(nonceGenerator.generate(), associatedTextGenerator.generate(), src, index, length);
    }

    @Override
    public void close() throws Exception {
        instance.close();
    }
}
