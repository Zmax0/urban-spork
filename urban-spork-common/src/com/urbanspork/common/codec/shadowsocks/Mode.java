package com.urbanspork.common.codec.shadowsocks;

public enum Mode {
    Client((byte) 0), Server((byte) 1);

    private final byte value;

    Mode(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}