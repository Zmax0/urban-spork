package com.urbanspork.common.codec;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SupportedCipher {

    aes_128_gcm,
    aes_256_gcm,
    chacha20_poly1305;

    private final String value;

    SupportedCipher() {
        value = name().replace('_', '-'); //  aes_128_gcm ->  aes-128-gcm
    }

    @JsonValue
    @Override
    public String toString() {
        return value;
    }
}
