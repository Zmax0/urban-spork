package com.urbanspork.client.vmess;

import com.urbanspork.common.codec.ChunkSizeCodec;
import com.urbanspork.common.codec.aead.AEADAuthenticator;
import com.urbanspork.common.codec.aead.AEADPayloadDecoder;

class ClientBodyDecoder implements AEADPayloadDecoder {

    private final AEADAuthenticator authenticator;
    private final ChunkSizeCodec chunkSizeDecoder;
    private int payloadLength = INIT_PAYLOAD_LENGTH;

    public ClientBodyDecoder(AEADAuthenticator authenticator, ChunkSizeCodec chunkSizeDecoder) {
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
