package com.urbanspork.common.codec.shadowsocks.impl;

import com.urbanspork.common.codec.SupportedCipher;

public class ShadowsocksCipherCodecs {

    public static ShadowsocksCipherCodec get(SupportedCipher cipher, byte[] password) {
        return switch (cipher) {
            case aes_128_gcm -> new AES128GCM(password);
            case aes_192_gcm -> new AES192GCM(password);
            case aes_256_gcm -> new AES256GCM(password);
            case chacha20_ietf_poly1305 -> new ChaCha20IETFPoly1305(password);
        };
    }

}
