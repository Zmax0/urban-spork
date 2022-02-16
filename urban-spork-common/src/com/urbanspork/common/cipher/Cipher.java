package com.urbanspork.common.cipher;

import io.netty.buffer.ByteBuf;

import java.security.SecureRandom;

public interface Cipher {

    ByteBuf encrypt(ByteBuf in, byte[] key) throws Exception;

    ByteBuf decrypt(ByteBuf in, byte[] key) throws Exception;

    void releaseBuffer();

    default byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

}
