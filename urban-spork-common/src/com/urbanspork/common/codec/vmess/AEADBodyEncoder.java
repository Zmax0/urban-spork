package com.urbanspork.common.codec.vmess;

import com.urbanspork.common.codec.ChunkSizeCodec;
import com.urbanspork.common.codec.PaddingLengthGenerator;
import com.urbanspork.common.codec.aead.Authenticator;
import com.urbanspork.common.codec.aead.PayloadEncoder;

public record AEADBodyEncoder(Authenticator auth, ChunkSizeCodec sizeCodec, PaddingLengthGenerator padding) implements PayloadEncoder {
    @Override
    public int payloadLimit() {
        return 2048;
    }
}
