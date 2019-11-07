package com.urbanspork.common.cipher;

import java.util.Arrays;

import com.urbanspork.common.cipher.impl.AES_256_CFB;
import com.urbanspork.common.cipher.impl.AES_256_CTR;
import com.urbanspork.common.cipher.impl.AES_256_GCM;
import com.urbanspork.common.cipher.impl.Camellia_256_CFB;
import com.urbanspork.common.cipher.impl.ChaCha20_IETF;
import com.urbanspork.common.cipher.impl.ChaCha20_IETF_Poly1305;

public enum ShadowsocksCiphers {

    AES_256_CFB(new AES_256_CFB()),

    AES_256_CTR(new AES_256_CTR()),

    AES_256_GCM(new AES_256_GCM()),

    Camellia_256_CFB(new Camellia_256_CFB()),

    ChaCha20_IETF(new ChaCha20_IETF()),

    ChaCha20_IETF_Poly1305(new ChaCha20_IETF_Poly1305());

    private ShadowsocksCipher shadowsocksCipher;

    private ShadowsocksCiphers(ShadowsocksCipher shadowsocksCipher) {
        this.shadowsocksCipher = shadowsocksCipher;
    }

    private ShadowsocksCipher getShadowsocksCipher() {
        return shadowsocksCipher;
    }

    public ShadowsocksCipher newShadowsocksCipher() {
        switch (this) {
        case AES_256_CFB:
            return new AES_256_CFB();
        case AES_256_CTR:
            return new AES_256_CTR();
        case AES_256_GCM:
            return new AES_256_GCM();
        case ChaCha20_IETF:
            return new ChaCha20_IETF();
        case Camellia_256_CFB:
            return new Camellia_256_CFB();
        case ChaCha20_IETF_Poly1305:
            return new ChaCha20_IETF_Poly1305();
        default:
            throw new UnsupportedOperationException();
        }
    }

    public static ShadowsocksCiphers nameOf(String name) {
        return Arrays.asList(values()).stream()
            .filter(cipher -> cipher.getShadowsocksCipher().getName().equals(name))
            .findAny().orElseThrow(UnsupportedOperationException::new);
    }

}
