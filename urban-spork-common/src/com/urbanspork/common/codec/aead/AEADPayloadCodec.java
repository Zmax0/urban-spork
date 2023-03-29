package com.urbanspork.common.codec.aead;

import com.urbanspork.common.codec.ChunkSizeCodec;

public interface AEADPayloadCodec {

    ChunkSizeCodec chunkSizeCodec();

    AEADAuthenticator authenticator();

}
