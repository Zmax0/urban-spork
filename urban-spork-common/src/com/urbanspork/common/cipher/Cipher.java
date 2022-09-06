package com.urbanspork.common.cipher;

import io.netty.buffer.ByteBuf;
import org.bouncycastle.crypto.InvalidCipherTextException;

import java.security.SecureRandom;

public interface Cipher {

    ByteBuf encrypt(ByteBuf in, byte[] key) throws InvalidCipherTextException;

    ByteBuf decrypt(ByteBuf in, byte[] key) throws InvalidCipherTextException;

    void releaseBuffer();

    default byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

}
