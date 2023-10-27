package com.urbanspork.common.codec.shadowsocks;

import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.codec.aead.CipherMethods;

public class AEADCipherCodecs {

    private AEADCipherCodecs() {}

    static AEADCipherCodec get(CipherKind kind, String password) {
        return switch (kind) {
            case aes_256_gcm, aead2022_blake3_aes_256_gcm -> new AEADCipherCodec(kind, CipherMethods.AES_GCM.get(), password, 32);
            case chacha20_poly1305 -> new AEADCipherCodec(kind, CipherMethods.CHACHA20_POLY1305.get(), password, 32);
            default -> new AEADCipherCodec(kind, CipherMethods.AES_GCM.get(), password, 16);
        };
    }
}
