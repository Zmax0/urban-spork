package com.urbanspork.common.codec.shadowsocks;

import com.urbanspork.common.codec.SupportedCipher;
import com.urbanspork.common.codec.aead.CipherCodecs;

import java.util.Base64;

public class AEADCipherCodecs {

    private AEADCipherCodecs() {}

    static AEADCipherCodec get(String password, SupportedCipher cipher) {
        return switch (cipher) {
            case aes_256_gcm -> new AEADCipherCodec(password, 32, CipherCodecs.AES_GCM.get());
            case chacha20_poly1305 -> new AEADCipherCodec(password, 32, CipherCodecs.CHACHA20_POLY1305.get());
            default -> new AEADCipherCodec(password, 16, CipherCodecs.AES_GCM.get());
        };
    }

    public static AEAD2022CipherCodec get2022(String password, SupportedCipher cipher) {
        byte[] key = Base64.getDecoder().decode(password);
        AEAD2022CipherCodec codec;
        if (SupportedCipher.aead2022_blake3_aes_128_gcm == cipher) {
            codec = new AEAD2022CipherCodec(key, 16, CipherCodecs.AES_GCM.get());
        } else {
            codec = new AEAD2022CipherCodec(key, 32, CipherCodecs.AES_GCM.get());
        }
        if (key.length != codec.saltSize()) {
            String msg = String.format("%s is expecting a %d bytes key, but password: %s (%d bytes after decode)",
                cipher.toString(), codec.saltSize(), password, key.length);
            throw new IllegalArgumentException(msg);
        }
        return codec;
    }
}
