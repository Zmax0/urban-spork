package com.urbanspork.cipher;

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
