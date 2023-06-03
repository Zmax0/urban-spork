package com.urbanspork.common.codec.vmess;

import com.urbanspork.common.codec.ChunkSizeCodec;
import com.urbanspork.common.codec.aead.AEADAuthenticator;
import com.urbanspork.common.codec.aead.AEADPayloadEncoder;

public record VMessAEADBodyEncoder(AEADAuthenticator payloadEncoder, ChunkSizeCodec chunkSizeEncoder) implements AEADPayloadEncoder {
    @Override
    public int payloadLimit() {
        return 0xffff;
    }
}
