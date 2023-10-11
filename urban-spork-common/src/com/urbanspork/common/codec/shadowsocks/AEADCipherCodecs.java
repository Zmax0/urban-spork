package com.urbanspork.common.codec.shadowsocks;

import com.urbanspork.common.codec.SupportedCipher;
import com.urbanspork.common.codec.aead.CipherCodecs;

class AEADCipherCodecs {

    private AEADCipherCodecs() {}

    public static AEADCipherCodec get(String password, SupportedCipher cipher) {
        return switch (cipher) {
            case aes_256_gcm -> new AEADCipherCodec(password, 32, CipherCodecs.AES_GCM.get());
            case chacha20_poly1305 -> new AEADCipherCodec(password, 32, CipherCodecs.CHACHA20_POLY1305.get());
            default -> new AEADCipherCodec(password, 16, CipherCodecs.AES_GCM.get());
        };
    }
}
