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
        CipherMethods methods = CipherKind.chacha20_poly1305 == kind ? CipherMethods.CHACHA20_POLY1305 : CipherMethods.AES_GCM;
        if (kind.isAead2022()) {
            return new Aead2022CipherCodecImpl(kind, methods.get(), keys);
        } else {
            return new AeadCipherCodecImpl(methods.get(), keys);
        }
    }
}
