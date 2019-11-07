package com.urbanspork.common.cipher;

import java.security.SecureRandom;

public interface Cipher {

    byte[] encrypt(byte[] in, byte[] key) throws Exception;

    byte[] decrypt(byte[] in, byte[] key) throws Exception;

    default byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

}
