package com.urbanspork.cipher;

public enum ShadowsocksCiphers {

    AES_256_CBA;

    public ShadowsocksCipher get() {
        switch (this) {
        case AES_256_CBA:
            return new AES_256_CBA();
        default:
            throw new UnsupportedOperationException();
        }
    }

}
