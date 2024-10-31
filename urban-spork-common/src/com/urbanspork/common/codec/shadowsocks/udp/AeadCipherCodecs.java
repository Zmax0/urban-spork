package com.urbanspork.common.codec.shadowsocks.udp;

import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.codec.aead.CipherMethods;
import com.urbanspork.common.codec.shadowsocks.Keys;
import com.urbanspork.common.config.ServerConfig;

class AeadCipherCodecs {

    private AeadCipherCodecs() {}

    static AeadCipherCodec get(ServerConfig config) {
        CipherKind kind = config.getCipher();
        Keys keys = Keys.from(kind, config.getPassword());
        CipherMethods methods;
        switch (kind) {
            case aes_128_gcm, aead2022_blake3_aes_128_gcm -> methods = CipherMethods.AES_128_GCM;
            case chacha20_poly1305 -> methods = CipherMethods.CHACHA20_POLY1305;
            case aead2022_blake3_chacha20_poly1305 -> methods = CipherMethods.XCHACHA20_POLY1305;
            default -> methods = CipherMethods.AES_265_GCM;
        }
        if (kind.isAead2022()) {
            return new Aead2022CipherCodecImpl(kind, methods.get(), keys);
        } else {
            return new AeadCipherCodecImpl(methods.get(), keys);
        }
    }
}
