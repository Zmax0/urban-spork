package com.urbanspork.common.cipher;

import io.netty.buffer.ByteBuf;
import org.bouncycastle.crypto.InvalidCipherTextException;

public interface ShadowsocksCipher {

    int getKeySize();

    Cipher encryptCipher();

    Cipher decryptCipher();

    default ByteBuf encrypt(ByteBuf in, ShadowsocksKey key) throws InvalidCipherTextException {
        return encryptCipher().encrypt(in, key.getEncoded());
    }

    default ByteBuf decrypt(ByteBuf in, ShadowsocksKey key) throws InvalidCipherTextException {
        return decryptCipher().decrypt(in, key.getEncoded());
    }

    default void releaseBuffer() {
        encryptCipher().releaseBuffer();
        decryptCipher().releaseBuffer();
    }

}
