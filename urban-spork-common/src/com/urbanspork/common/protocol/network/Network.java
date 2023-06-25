package com.urbanspork.common.protocol.network;

public enum Network {

    TCP, UDP;

    private final String value;

    Network() {
        value = name().toLowerCase();
    }

    public String value() {
        return value;
    }
}
