package com.urbanspork.cipher;

public enum ShadowsocksCiphers {

    AES_256_CFB;

    public ShadowsocksCipher get() {
        switch (this) {
        case AES_256_CFB:
            return new AES_256_CFB();
        default:
            throw new UnsupportedOperationException();
        }
    }

}
