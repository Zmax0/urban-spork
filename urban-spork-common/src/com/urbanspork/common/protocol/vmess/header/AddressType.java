package com.urbanspork.common.protocol.vmess.header;

public enum AddressType {

    IPV4((byte) 1), DOMAIN((byte) 2), IPV6((byte) 3);

    private static final AddressType[] VALUES = values();
    private final byte value;

    AddressType(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }

    public static AddressType valueOf(byte value) {
        if (1 <= value && value <= 3) {
            return VALUES[value - 1];
        } else {
            throw new IllegalArgumentException("Unknown address type: " + value);
        }
    }
}
