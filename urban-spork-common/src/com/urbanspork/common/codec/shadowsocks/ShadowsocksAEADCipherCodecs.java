package com.urbanspork.common.codec.shadowsocks;

import com.urbanspork.common.codec.SupportedCipher;
import com.urbanspork.common.codec.aead.AEADCipherCodecs;
import com.urbanspork.common.protocol.shadowsocks.network.Network;

public class ShadowsocksAEADCipherCodecs {

    private ShadowsocksAEADCipherCodecs() {

    }

    public static ShadowsocksAEADCipherCodec get(String password, SupportedCipher cipher, Network network) {
        return switch (cipher) {
            case aes_128_gcm -> new ShadowsocksAEADCipherCodec(password, 16, AEADCipherCodecs.AES_GCM.get(), network);
            case aes_256_gcm -> new ShadowsocksAEADCipherCodec(password, 32, AEADCipherCodecs.AES_GCM.get(), network);
            case chacha20_poly1305 ->
                    new ShadowsocksAEADCipherCodec(password, 32, AEADCipherCodecs.CHACHA20_POLY1305.get(), network);
        };
    }

}
