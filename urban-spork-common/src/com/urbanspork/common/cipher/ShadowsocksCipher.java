package com.urbanspork.common.cipher;

public interface ShadowsocksCipher {

    String getName();

    int getKeyLength();

    Cipher encrypter();

    Cipher decrypter();

    default byte[] encrypt(byte[] in, ShadowsocksKey key) throws Exception {
        return encrypter().encrypt(in, key.getEncoded());
    }

    default byte[] decrypt(byte[] in, ShadowsocksKey key) throws Exception {
        return decrypter().decrypt(in, key.getEncoded());
    }

}
