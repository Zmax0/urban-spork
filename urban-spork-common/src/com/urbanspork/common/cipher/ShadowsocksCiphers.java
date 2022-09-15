package com.urbanspork.common.cipher;

import com.urbanspork.common.cipher.impl.AES128GCM;
import com.urbanspork.common.cipher.impl.AES192GCM;
import com.urbanspork.common.cipher.impl.AES256GCM;
import com.urbanspork.common.cipher.impl.ChaCha20IETFPoly1305;

public enum ShadowsocksCiphers {

    aes_128_gcm, aes_192_gcm, aes_256_gcm,

    chacha20_ietf_poly1305;

    public ShadowsocksCipher newCipher() {
        return switch (this) {
            case aes_128_gcm -> new AES128GCM();
            case aes_192_gcm -> new AES192GCM();
            case aes_256_gcm -> new AES256GCM();
            case chacha20_ietf_poly1305 -> new ChaCha20IETFPoly1305();
        };
    }

    public static ShadowsocksCiphers defaultCipher() {
        return aes_256_gcm;
    }

}
