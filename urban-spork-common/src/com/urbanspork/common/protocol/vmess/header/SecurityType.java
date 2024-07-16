package com.urbanspork.common.protocol.vmess.header;

import com.urbanspork.common.codec.CipherKind;

public enum SecurityType {
    UNKNOWN((byte) 0),
    LEGACY((byte) 1),
    AUTO((byte) 2),
    AES128_GCM((byte) 3),
    CHACHA20_POLY1305((byte) 4),
    NONE((byte) 5),
    ZERO((byte) 6);

    private static final SecurityType[] VALUES = values();
    private final byte value;

    SecurityType(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }

    public static SecurityType valueOf(byte value) {
        return VALUES[value];
    }

    public static SecurityType valueOf(CipherKind cipher) {
        if (CipherKind.chacha20_poly1305 == cipher) {
            return CHACHA20_POLY1305;
        } else {
            return AES128_GCM;
        }
    }
}
