package com.urbanspork.common.protocol.network;

public enum Direction {

    Inbound("←"),
    Outbound("→"),
    ;

    private final String value;

    Direction(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
