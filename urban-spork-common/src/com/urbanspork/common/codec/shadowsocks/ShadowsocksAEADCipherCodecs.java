package com.urbanspork.common.codec.shadowsocks;

import com.urbanspork.common.codec.SupportedCipher;
import com.urbanspork.common.codec.aead.AEADCipherCodecs;

public class ShadowsocksAEADCipherCodecs {

    public static ShadowsocksAEADCipherCodec get(SupportedCipher cipher, String password) {
        return switch (cipher) {
            case aes_128_gcm -> new ShadowsocksAEADCipherCodec(password, 16, AEADCipherCodecs.AES_GCM.get());
            case aes_192_gcm -> new ShadowsocksAEADCipherCodec(password, 24, AEADCipherCodecs.AES_GCM.get());
            case aes_256_gcm -> new ShadowsocksAEADCipherCodec(password, 32, AEADCipherCodecs.AES_GCM.get());
            case chacha20_poly1305 ->
                    new ShadowsocksAEADCipherCodec(password, 32, AEADCipherCodecs.CHACHA20_POLY1305.get());
        };
    }

}
