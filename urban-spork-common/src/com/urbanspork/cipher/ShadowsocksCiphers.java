package com.urbanspork.cipher;

import com.urbanspork.cipher.impl.AES_256_CFB;
import com.urbanspork.cipher.impl.AES_256_GCM;
import com.urbanspork.cipher.impl.ChaCha20_IETF;

public enum ShadowsocksCiphers {

    AES_256_CFB, AES_256_GCM, ChaCha20_IETF;

    public ShadowsocksCipher get() {
        switch (this) {
        case AES_256_CFB:
            return new AES_256_CFB();
        case AES_256_GCM:
            return new AES_256_GCM();
        case ChaCha20_IETF:
            return new ChaCha20_IETF();
        default:
            throw new UnsupportedOperationException();
        }
    }

}
