package com.urbanspork.common.codec.shadowsocks.tcp;

import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.codec.aead.CipherMethods;
import com.urbanspork.common.codec.shadowsocks.Keys;
import com.urbanspork.common.config.ServerConfig;

class AeadCipherCodecs {

    private AeadCipherCodecs() {}

    static AeadCipherCodec get(ServerConfig config) {
        CipherKind kind = config.getCipher();
        Keys keys = Keys.from(kind, config.getPassword());
        if (CipherKind.chacha20_poly1305 == kind) {
            return new AeadCipherCodec(kind, CipherMethods.CHACHA20_POLY1305.get(), keys);
        } else {
            return new AeadCipherCodec(kind, CipherMethods.AES_GCM.get(), keys);
        }
    }
}
