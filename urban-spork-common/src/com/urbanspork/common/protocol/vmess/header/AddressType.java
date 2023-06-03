package com.urbanspork.common.protocol.vmess.header;

import java.util.Arrays;

public enum AddressType {

    IPV4(1), DOMAIN(2), IPV6(3);

    private final int value;

    AddressType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static AddressType valueOf(int value) {
        return Arrays.stream(values()).filter(type -> type.value == value)
            .findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown address type: " + value));
    }
}
