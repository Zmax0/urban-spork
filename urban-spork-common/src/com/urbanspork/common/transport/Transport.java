package com.urbanspork.common.transport;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Transport {
    UDP, TCP, QUIC;

    @JsonValue
    public String value() {
        return name().toLowerCase();
    }
}
