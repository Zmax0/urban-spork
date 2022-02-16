package com.urbanspork.common.cipher;

import io.netty.buffer.ByteBuf;

public interface ShadowsocksCipher {

    int getKeySize();

    Cipher encryptCipher();

    Cipher decryptCipher();

    default ByteBuf encrypt(ByteBuf in, ShadowsocksKey key) throws Exception {
        return encryptCipher().encrypt(in, key.getEncoded());
    }

    default ByteBuf decrypt(ByteBuf in, ShadowsocksKey key) throws Exception {
        return decryptCipher().decrypt(in, key.getEncoded());
    }

    default void releaseBuffer() {
        encryptCipher().releaseBuffer();
        decryptCipher().releaseBuffer();
    }

}
