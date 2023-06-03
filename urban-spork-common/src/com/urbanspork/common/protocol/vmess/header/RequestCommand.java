package com.urbanspork.common.protocol.vmess.header;

public enum RequestCommand {

    TCP(1), UDP(2), Mux(3);

    private final int value;

    RequestCommand(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
