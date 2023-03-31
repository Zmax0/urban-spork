package com.urbanspork.common.protocol.vmess.cons;

import com.urbanspork.common.codec.SupportedCipher;

public enum SecurityType {

    UNKNOWN(0),
    LEGACY(1),
    AUTO(2),
    AES128_GCM(3),
    CHACHA20_POLY1305(4),
    NONE(5),
    ZERO(6);

    private final int value;

    SecurityType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static SecurityType from(SupportedCipher cipher) {
        return switch (cipher) {
            case aes_128_gcm -> AES128_GCM;
            case chacha20_poly1305 -> CHACHA20_POLY1305;
            default -> AUTO;
        };
    }
}
