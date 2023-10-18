package com.urbanspork.common.codec;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SupportedCipher {

    aes_128_gcm,
    aes_256_gcm,
    chacha20_poly1305,
    aead2022_blake3_aes_128_gcm("2022-blake3-aes-128-gcm"),
    aead2022_blake3_aes_256_gcm("2022-blake3-aes-256-gcm"),
    ;

    private final String value;

    SupportedCipher() {
        this.value = name().replace('_', '-'); //  aes_128_gcm ->  aes-128-gcm
    }

    SupportedCipher(String value) {
        this.value = value;
    }

    @JsonValue
    @Override
    public String toString() {
        return value;
    }

    public boolean isAEAD2022() {
        return this == aead2022_blake3_aes_128_gcm || this == aead2022_blake3_aes_256_gcm;
    }
}
