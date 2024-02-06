package com.urbanspork.common.transport;

public enum Transport {

    TCP, UDP;

    private final String value;

    Transport() {
        value = name().toLowerCase();
    }

    public String value() {
        return value;
    }
}
