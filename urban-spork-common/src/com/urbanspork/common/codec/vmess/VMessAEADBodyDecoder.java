package com.urbanspork.common.codec.vmess;

import com.urbanspork.common.codec.ChunkSizeCodec;
import com.urbanspork.common.codec.aead.AEADAuthenticator;
import com.urbanspork.common.codec.aead.AEADPayloadDecoder;

public class VMessAEADBodyDecoder implements AEADPayloadDecoder {

    private final AEADAuthenticator authenticator;
    private final ChunkSizeCodec chunkSizeDecoder;
    private int payloadLength = INIT_PAYLOAD_LENGTH;

    public VMessAEADBodyDecoder(AEADAuthenticator authenticator, ChunkSizeCodec chunkSizeDecoder) {
        this.authenticator = authenticator;
        this.chunkSizeDecoder = chunkSizeDecoder;
    }

    @Override
    public ChunkSizeCodec chunkSizeDecoder() {
        return chunkSizeDecoder;
    }

    @Override
    public AEADAuthenticator payloadDecoder() {
        return authenticator;
    }

    @Override
    public int payloadLength() {
        return payloadLength;
    }

    @Override
    public void updatePayloadLength(int payloadLength) {
        this.payloadLength = payloadLength;
    }

}
