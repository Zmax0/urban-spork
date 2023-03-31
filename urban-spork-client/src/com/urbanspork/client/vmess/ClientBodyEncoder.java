package com.urbanspork.client.vmess;

import com.urbanspork.common.codec.ChunkSizeCodec;
import com.urbanspork.common.codec.aead.AEADAuthenticator;
import com.urbanspork.common.codec.aead.AEADPayloadEncoder;

record ClientBodyEncoder(AEADAuthenticator payloadEncoder,
                         ChunkSizeCodec chunkSizeEncoder) implements AEADPayloadEncoder {
    @Override
    public int maxPayloadLength() {
        return 0xffff;
    }
}
