package com.urbanspork.common.protocol.shadowsocks;

public enum StreamType {
    Request((byte) 0), Response((byte) 1);

    private final byte value;

    StreamType(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }
}