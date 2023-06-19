package com.urbanspork.common.protocol.vmess.header;

import java.util.Arrays;

public enum RequestOption {

    // RequestOptionChunkStream indicates request payload is chunked. Each chunk consists of length, authentication and payload.
    ChunkStream((byte) 1),

    // RequestOptionConnectionReuse indicates client side expects to reuse the connection.
    ConnectionReuse((byte) 2),

    ChunkMasking((byte) 4),

    GlobalPadding((byte) 8),

    AuthenticatedLength((byte) 16),
    ;

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
        RequestOption[] options = values();
        RequestOption[] res = new RequestOption[options.length];
        int i = 0;
        for (RequestOption option : options) {
            if ((option.value & mask) != 0) {
                res[i++] = option;
            }
        }
        return Arrays.copyOf(res, i);
    }

    public static boolean has(RequestOption[] options, RequestOption target) {
        return Arrays.stream(options).anyMatch(option -> option == target);
    }
}
