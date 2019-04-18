package com.urbanspork.cipher.impl;

import com.urbanspork.cipher.Cipher;
import com.urbanspork.cipher.ShadowsocksCipher;
import com.urbanspork.cipher.ShadowsocksKey;

public abstract class AbstractShadowsocksCipher implements ShadowsocksCipher {

    private Cipher encrypt;
    private Cipher decrypt;

    // @formatter:off
    protected AbstractShadowsocksCipher() {}
    // @formatter:on

    protected AbstractShadowsocksCipher(Cipher encrypt, Cipher decrypt) {
        this.encrypt = encrypt;
        this.decrypt = decrypt;
    }

    @Override
    public byte[] encrypt(byte[] in, ShadowsocksKey key) throws Exception {
        return encrypt.encrypt(in, key.getEncoded());
    }

    @Override
    public byte[] decrypt(byte[] in, ShadowsocksKey key) throws Exception {
        return decrypt.decrypt(in, key.getEncoded());
    }

}
