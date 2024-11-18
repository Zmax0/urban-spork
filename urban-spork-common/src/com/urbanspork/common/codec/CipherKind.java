package com.urbanspork.common.codec;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum CipherKind {
    aes_128_gcm(16),
    aes_256_gcm(32),
    chacha20_poly1305(32),
    aead2022_blake3_aes_128_gcm("2022-blake3-aes-128-gcm", 16, true, true),
    aead2022_blake3_aes_256_gcm("2022-blake3-aes-256-gcm", 32, true, true),
    aead2022_blake3_chacha8_poly1305("2022-blake3-chacha8-poly1305", 32, true, false),
    aead2022_blake3_chacha20_poly1305("2022-blake3-chacha20-poly1305", 32, true, false),
    ;

    private static final Map<String, CipherKind> MAP;
    private final String value;
    private final int keySize;
    private final boolean aead2022;
    private final boolean eih;

    static {
        MAP = new HashMap<>();
        for (CipherKind cipherKind : CipherKind.values()) {
            MAP.put(cipherKind.toString(), cipherKind);
        }
    }

    CipherKind(int keySize) {
        this.value = name().replace('_', '-'); //  aes_128_gcm ->  aes-128-gcm
        this.keySize = keySize;
        this.aead2022 = false;
        this.eih = false;
    }

    CipherKind(String value, int keySize, boolean aead2022, boolean eih) {
        this.value = value;
        this.keySize = keySize;
        this.aead2022 = aead2022;
        this.eih = eih;
    }

    @JsonValue
    @Override
    public String toString() {
        return value;
    }

    public static Optional<CipherKind> from(String method) {
        return Optional.ofNullable(MAP.get(method));
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
