package com.urbanspork.common.codec.vmess;

import com.urbanspork.common.codec.ChunkSizeCodec;
import com.urbanspork.common.codec.PaddingLengthGenerator;
import com.urbanspork.common.codec.aead.AEADAuthenticator;
import com.urbanspork.common.codec.aead.AEADPayloadDecoder;

public class AEADBodyDecoder implements AEADPayloadDecoder {

    private final AEADAuthenticator auth;
    private final ChunkSizeCodec sizeCodec;
    private final PaddingLengthGenerator padding;
    private int payloadLength = INIT_PAYLOAD_LENGTH;

    public AEADBodyDecoder(AEADAuthenticator auth, ChunkSizeCodec sizeCodec, PaddingLengthGenerator padding) {
        this.auth = auth;
        this.sizeCodec = sizeCodec;
        this.padding = padding;
    }

    @Override
    public int payloadLength() {
        return payloadLength;
    }

    @Override
    public void updatePayloadLength(int payloadLength) {
        this.payloadLength = payloadLength;
    }

    @Override
    public AEADAuthenticator auth() {
        return auth;
    }

    @Override
    public ChunkSizeCodec sizeCodec() {
        return sizeCodec;
    }

    @Override
    public PaddingLengthGenerator padding() {
        return padding;
    }

}
