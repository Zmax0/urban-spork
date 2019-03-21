package com.urbanspork.cipher;

import javax.crypto.SecretKey;

public interface ShadowsocksCipher {

    byte[] encrypt(byte[] in, SecretKey key) throws Exception;

    byte[] decrypt(byte[] in, SecretKey key) throws Exception;

}
