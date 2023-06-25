package com.urbanspork.common.codec.shadowsocks;

import com.urbanspork.common.codec.SupportedCipher;
import com.urbanspork.common.codec.aead.CipherCodecs;
import com.urbanspork.common.protocol.network.Network;

public class AEADCipherCodecs {

    private AEADCipherCodecs() {}

    public static AEADCipherCodec get(String password, SupportedCipher cipher, Network network) {
        return switch (cipher) {
            case aes_256_gcm -> new AEADCipherCodec(password, 32, CipherCodecs.AES_GCM.get(), network);
            case chacha20_poly1305 -> new AEADCipherCodec(password, 32, CipherCodecs.CHACHA20_POLY1305.get(), network);
            default -> new AEADCipherCodec(password, 16, CipherCodecs.AES_GCM.get(), network);
        };
    }
}
