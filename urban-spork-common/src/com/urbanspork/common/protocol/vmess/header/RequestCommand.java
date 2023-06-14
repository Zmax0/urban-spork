package com.urbanspork.common.protocol.vmess.header;

import java.util.Arrays;

public enum RequestCommand {

    TCP((byte) 1), UDP((byte) 2), Mux((byte) 3);

    private final byte value;

    RequestCommand(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }

    public static RequestCommand valueOf(byte value) {
        return Arrays.stream(values()).filter(type -> type.value == value).findFirst().orElseThrow();
    }
}
