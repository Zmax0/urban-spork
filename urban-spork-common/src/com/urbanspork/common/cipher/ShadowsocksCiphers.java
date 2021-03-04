package com.urbanspork.common.cipher;

import com.urbanspork.common.cipher.impl.AES_128_GCM;
import com.urbanspork.common.cipher.impl.AES_192_GCM;
import com.urbanspork.common.cipher.impl.AES_256_GCM;
import com.urbanspork.common.cipher.impl.ChaCha20_IETF_Poly1305;

public enum ShadowsocksCiphers {

    AES_128_GCM, AES_192_GCM, AES_256_GCM,

    ChaCha20_IETF_Poly1305;

    public ShadowsocksCipher newShadowsocksCipher() {
        return switch (this) {
            case AES_128_GCM -> new AES_128_GCM();
            case AES_192_GCM -> new AES_192_GCM();
            case AES_256_GCM -> new AES_256_GCM();
            case ChaCha20_IETF_Poly1305 -> new ChaCha20_IETF_Poly1305();
        };
    }

}
