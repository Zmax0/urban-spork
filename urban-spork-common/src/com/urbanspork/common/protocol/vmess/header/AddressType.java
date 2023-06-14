package com.urbanspork.common.protocol.vmess.header;

import java.util.Arrays;

public enum AddressType {

    IPV4((byte) 1), DOMAIN((byte) 2), IPV6((byte) 3);

    private final byte value;

    AddressType(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }

    public static AddressType valueOf(byte value) {
        return Arrays.stream(values()).filter(type -> type.value == value)
            .findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown address type: " + value));
    }
}
