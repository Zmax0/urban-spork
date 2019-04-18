package com.urbanspork.cipher;

public interface ShadowsocksCipher {

    byte[] encrypt(byte[] in, ShadowsocksKey key) throws Exception;

    byte[] decrypt(byte[] in, ShadowsocksKey key) throws Exception;

    int getKeyLength();

}
