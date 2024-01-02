package com.urbanspork.common.codec;

import com.fasterxml.jackson.annotation.JsonValue;

public enum CipherKind {

    aes_128_gcm(16),
    aes_256_gcm(32),
    chacha20_poly1305(32),
    aead2022_blake3_aes_128_gcm("2022-blake3-aes-128-gcm", 16),
    aead2022_blake3_aes_256_gcm("2022-blake3-aes-256-gcm", 32),
//    aead2022_blake3_chacha20_poly1305("2022-blake3-chacha20-poly1305", true),
    ;

    private final String value;
    private final int keySize;
    private final boolean aead2022;
    private final boolean eih;

    CipherKind(int keySize) {
        this.value = name().replace('_', '-'); //  aes_128_gcm ->  aes-128-gcm
        this.keySize = keySize;
        this.aead2022 = false;
        this.eih = false;
    }

    CipherKind(String value, int keySize) {
        this.value = value;
        this.keySize = keySize;
        this.aead2022 = true;
        this.eih = true;
    }

    @JsonValue
    @Override
    public String toString() {
        return value;
    }

    public int keySize() {
        return keySize;
    }

    public boolean isAead2022() {
        return this.aead2022;
    }

    public boolean supportEih() {
        return this.eih;
    }
}
