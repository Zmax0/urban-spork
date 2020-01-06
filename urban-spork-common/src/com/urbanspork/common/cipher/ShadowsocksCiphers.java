package com.urbanspork.common.cipher;

import com.urbanspork.common.cipher.impl.AES_128_CFB;
import com.urbanspork.common.cipher.impl.AES_128_CTR;
import com.urbanspork.common.cipher.impl.AES_128_GCM;
import com.urbanspork.common.cipher.impl.AES_192_CFB;
import com.urbanspork.common.cipher.impl.AES_192_CTR;
import com.urbanspork.common.cipher.impl.AES_192_GCM;
import com.urbanspork.common.cipher.impl.AES_256_CFB;
import com.urbanspork.common.cipher.impl.AES_256_CTR;
import com.urbanspork.common.cipher.impl.AES_256_GCM;
import com.urbanspork.common.cipher.impl.Camellia_128_CFB;
import com.urbanspork.common.cipher.impl.Camellia_192_CFB;
import com.urbanspork.common.cipher.impl.Camellia_256_CFB;
import com.urbanspork.common.cipher.impl.ChaCha20_IETF;
import com.urbanspork.common.cipher.impl.ChaCha20_IETF_Poly1305;

public enum ShadowsocksCiphers {

    AES_128_CFB, AES_192_CFB, AES_256_CFB,

    AES_128_CTR, AES_192_CTR, AES_256_CTR,

    Camellia_128_CFB, Camellia_192_CFB, Camellia_256_CFB,

    ChaCha20_IETF,

    AES_128_GCM, AES_192_GCM, AES_256_GCM,

    ChaCha20_IETF_Poly1305;

    public ShadowsocksCipher newShadowsocksCipher() {
        switch (this) {
        case AES_128_CFB:
            return new AES_128_CFB();
        case AES_192_CFB:
            return new AES_192_CFB();
        case AES_256_CFB:
            return new AES_256_CFB();
        case AES_128_CTR:
            return new AES_128_CTR();
        case AES_192_CTR:
            return new AES_192_CTR();
        case AES_256_CTR:
            return new AES_256_CTR();
        case ChaCha20_IETF:
            return new ChaCha20_IETF();
        case Camellia_128_CFB:
            return new Camellia_128_CFB();
        case Camellia_192_CFB:
            return new Camellia_192_CFB();
        case Camellia_256_CFB:
            return new Camellia_256_CFB();
        case AES_128_GCM:
            return new AES_128_GCM();
        case AES_192_GCM:
            return new AES_192_GCM();
        case AES_256_GCM:
            return new AES_256_GCM();
        case ChaCha20_IETF_Poly1305:
            return new ChaCha20_IETF_Poly1305();
        default:
            throw new UnsupportedOperationException();
        }
    }

}
