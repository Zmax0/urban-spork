package com.urbanspork.common.cipher;

import com.urbanspork.common.cipher.impl.AES_128_GCM;
import com.urbanspork.common.cipher.impl.AES_192_GCM;
import com.urbanspork.common.cipher.impl.AES_256_GCM;
import com.urbanspork.common.cipher.impl.ChaCha20_IETF_Poly1305;

public enum ShadowsocksCiphers {

    aes_128_gcm, aes_192_gcm, aes_256_gcm,

    chacha20_ietf_poly1305;

    public ShadowsocksCipher newShadowsocksCipher() {
        return switch (this) {
            case aes_128_gcm -> new AES_128_GCM();
            case aes_192_gcm -> new AES_192_GCM();
            case aes_256_gcm -> new AES_256_GCM();
            case chacha20_ietf_poly1305 -> new ChaCha20_IETF_Poly1305();
        };
    }

}
