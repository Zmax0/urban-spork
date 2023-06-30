package com.urbanspork.common.protocol.vmess.header;

public record RequestCommand(byte value) {

    public static final RequestCommand TCP = new RequestCommand((byte) 1);
    public static final RequestCommand UDP = new RequestCommand((byte) 2);
    // not support Mux now

    @Override
    public boolean equals(Object obj) {
        return obj instanceof RequestCommand command && value == command.value;
    }

    @Override
    public int hashCode() {
        return Byte.hashCode(value);
    }
}
