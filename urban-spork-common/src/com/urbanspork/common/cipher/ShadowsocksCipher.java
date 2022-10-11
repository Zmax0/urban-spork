package com.urbanspork.common.cipher;

import io.netty.buffer.ByteBuf;
import org.bouncycastle.crypto.InvalidCipherTextException;

import java.util.List;

public interface ShadowsocksCipher {

    int getKeySize();

    Cipher encryptCipher();

    Cipher decryptCipher();

    default ByteBuf encrypt(ByteBuf in, ShadowsocksKey key) throws InvalidCipherTextException {
        return encryptCipher().encrypt(in, key.getEncoded());
    }

    default List<ByteBuf> decrypt(ByteBuf in, ShadowsocksKey key) throws InvalidCipherTextException {
        return decryptCipher().decrypt(in, key.getEncoded());
    }

}
