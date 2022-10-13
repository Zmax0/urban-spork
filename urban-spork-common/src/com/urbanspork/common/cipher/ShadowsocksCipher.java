package com.urbanspork.common.cipher;

import io.netty.buffer.ByteBuf;
import org.bouncycastle.crypto.InvalidCipherTextException;

import java.util.List;

public interface ShadowsocksCipher {

    int getKeySize();

    Cipher encryptCipher();

    Cipher decryptCipher();

    default void encrypt(ByteBuf in, ShadowsocksKey key, ByteBuf out) throws InvalidCipherTextException {
        encryptCipher().encrypt(in, key.getEncoded(), out);
    }

    default void decrypt(ByteBuf in, ShadowsocksKey key, List<Object> out) throws InvalidCipherTextException {
        decryptCipher().decrypt(in, key.getEncoded(), out);
    }

}
