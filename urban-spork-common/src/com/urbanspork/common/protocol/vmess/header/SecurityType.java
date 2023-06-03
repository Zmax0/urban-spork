package com.urbanspork.common.protocol.vmess.header;

import com.urbanspork.common.codec.SupportedCipher;

import java.util.Arrays;

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

    public static SecurityType valueOf(int value) {
        return Arrays.stream(values()).filter(type -> type.value == value).findFirst().orElse(UNKNOWN);
    }

    public static SecurityType valueOf(SupportedCipher cipher) {
        if (SupportedCipher.chacha20_poly1305 == cipher) {
            return CHACHA20_POLY1305;
        } else {
            return AES128_GCM;
        }
    }
}
