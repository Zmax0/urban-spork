package com.urbanspork.cipher;

import java.util.Arrays;

import com.urbanspork.cipher.impl.AES_256_CFB;
import com.urbanspork.cipher.impl.AES_256_CTR;
import com.urbanspork.cipher.impl.AES_256_GCM;
import com.urbanspork.cipher.impl.Camellia_256_CFB;
import com.urbanspork.cipher.impl.ChaCha20_IETF;

public enum ShadowsocksCiphers {

    AES_256_CFB(new AES_256_CFB()),

    AES_256_CTR(new AES_256_CTR()),

    AES_256_GCM(new AES_256_GCM()),

    ChaCha20_IETF(new ChaCha20_IETF()),

    Camellia_256_CFB(new Camellia_256_CFB());

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
