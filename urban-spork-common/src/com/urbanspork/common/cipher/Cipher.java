package com.urbanspork.common.cipher;

import io.netty.buffer.ByteBuf;
import org.bouncycastle.crypto.InvalidCipherTextException;

import java.security.SecureRandom;
import java.util.List;

public interface Cipher {

    void encrypt(ByteBuf in, byte[] key, ByteBuf out) throws InvalidCipherTextException;

    void decrypt(ByteBuf in, byte[] key, List<Object> out) throws InvalidCipherTextException;

    default byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

}
