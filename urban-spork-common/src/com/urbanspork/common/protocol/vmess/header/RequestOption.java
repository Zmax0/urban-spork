package com.urbanspork.common.protocol.vmess.header;

public enum RequestOption {

    // RequestOptionChunkStream indicates request payload is chunked. Each chunk consists of length, authentication and payload.
    ChunkStream(1),

    // RequestOptionConnectionReuse indicates client side expects to reuse the connection.
    ConnectionReuse(2),

    ChunkMasking(4),

    GlobalPadding(8),

    AuthenticatedLength(16),
    ;

    private final int value;

    RequestOption(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
