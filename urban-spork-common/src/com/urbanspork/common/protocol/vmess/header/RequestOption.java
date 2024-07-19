package com.urbanspork.common.protocol.vmess.header;

import java.util.Arrays;

public enum RequestOption {
    Empty((byte) 0),
    // RequestOptionChunkStream indicates request payload is chunked. Each chunk consists of length, authentication and payload.
    ChunkStream((byte) 1),
    // RequestOptionConnectionReuse indicates client side expects to reuse the connection.
    ConnectionReuse((byte) 2),

    ChunkMasking((byte) 4),

    GlobalPadding((byte) 8),

    AuthenticatedLength((byte) 16),
    ;

    private static final RequestOption[] VALUES = values();

    private final byte value;

    RequestOption(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }

    public static byte toMask(RequestOption[] arr) {
        byte b = 0;
        for (RequestOption option : arr) {
            b |= option.value;
        }
        return b;
    }

    public static RequestOption[] fromMask(byte mask) {
        RequestOption[] res = Arrays.copyOf(VALUES, VALUES.length);
        for (int i = 0; i < VALUES.length; i++) {
            if ((VALUES[i].value & mask) == 0) {
                res[i] = Empty;
            }
        }
        return res;
    }

    public static boolean has(RequestOption[] options, RequestOption target) {
        for (RequestOption option : options) {
            if (option == target) {
                return true;
            }
        }
        return false;
    }
}
