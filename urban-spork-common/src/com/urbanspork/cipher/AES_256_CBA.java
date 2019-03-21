package com.urbanspork.cipher;

import javax.crypto.SecretKey;

public class AES_256_CBA implements ShadowsocksCipher {

    private final SymmetricEncryptUtils encrypt = SymmetricEncryptUtils.AES.AES_256_CFB();
    private final SymmetricEncryptUtils decrypt = SymmetricEncryptUtils.AES.AES_256_CFB();

    @Override
    public byte[] encrypt(byte[] in, SecretKey key) throws Exception {
        return encrypt.encrypt(in, key.getEncoded());
    }

    @Override
    public byte[] decrypt(byte[] in, SecretKey key) throws Exception {
        return decrypt.decrypt(in, key.getEncoded());
    }

}
