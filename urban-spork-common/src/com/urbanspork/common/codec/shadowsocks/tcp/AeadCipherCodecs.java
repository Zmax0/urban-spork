package com.urbanspork.common.codec.shadowsocks.tcp;

import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.codec.aead.CipherMethod;
import com.urbanspork.common.codec.shadowsocks.Keys;
import com.urbanspork.common.config.ServerConfig;

class AeadCipherCodecs {

    private AeadCipherCodecs() {}

    static AeadCipherCodec get(ServerConfig config) {
        CipherKind kind = config.getCipher();
        Keys keys = Keys.from(kind, config.getPassword());
        CipherMethod method;
        switch (kind) {
            case aes_128_gcm, aead2022_blake3_aes_128_gcm -> method = CipherMethod.AES_128_GCM;
            case chacha20_poly1305, aead2022_blake3_chacha20_poly1305 -> method = CipherMethod.CHACHA20_POLY1305;
            case aead2022_blake3_chacha8_poly1305 -> method = CipherMethod.CHACHA8_POLY1305;
            default -> method = CipherMethod.AES_265_GCM;
        }
        return new AeadCipherCodec(kind, method, keys);
    }
}
