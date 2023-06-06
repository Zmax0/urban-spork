package com.urbanspork.common.codec.vmess;

import com.urbanspork.common.codec.ChunkSizeCodec;
import com.urbanspork.common.codec.PaddingLengthGenerator;
import com.urbanspork.common.codec.aead.AEADAuthenticator;
import com.urbanspork.common.codec.aead.AEADPayloadEncoder;

public record AEADBodyEncoder(AEADAuthenticator auth, ChunkSizeCodec sizeCodec, PaddingLengthGenerator padding) implements AEADPayloadEncoder {
    @Override
    public int payloadLimit() {
        return 2048;
    }
}
