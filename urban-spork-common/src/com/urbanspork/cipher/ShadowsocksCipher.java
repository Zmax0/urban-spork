package com.urbanspork.cipher;

public interface ShadowsocksCipher {

    Cipher encrypter();

    Cipher decrypter();

    int getKeyLength();

    default byte[] encrypt(byte[] in, ShadowsocksKey key) throws Exception {
        return encrypter().encrypt(in, key.getEncoded());
    }

    default byte[] decrypt(byte[] in, ShadowsocksKey key) throws Exception {
        return decrypter().decrypt(in, key.getEncoded());
    }

}
