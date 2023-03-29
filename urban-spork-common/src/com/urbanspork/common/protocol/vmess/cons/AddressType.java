package com.urbanspork.common.protocol.vmess.cons;

public enum AddressType {

    IPV4(1), DOMAIN(2), IPV6(3);

    private final int value;

    AddressType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
