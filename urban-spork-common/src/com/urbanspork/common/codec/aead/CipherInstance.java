package com.urbanspork.common.codec.aead;

import io.netty.buffer.ByteBuf;

import java.util.Arrays;

public interface CipherInstance extends AutoCloseable {
    int tagSize();

    void encrypt(byte[] nonce, byte[] aad, byte[] buffer);

    int decrypt(byte[] nonce, byte[] aad, byte[] buffer);

    void encrypt(byte[] nonce, byte[] aad, ByteBuf dst, int index, int length);

    ByteBuf decrypt(byte[] nonce, byte[] aad, ByteBuf src, int index, int length);

    default byte[] encryptTo(byte[] nonce, byte[] aad, byte[] in) {
        byte[] buffer = Arrays.copyOf(in, in.length + tagSize());
        encrypt(nonce, aad, buffer);
        return buffer;
    }

    default byte[] decryptTo(byte[] nonce, byte[] aad, byte[] in) {
        return Arrays.copyOf(in, decrypt(nonce, aad, in));
    }
}
