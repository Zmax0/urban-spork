package com.urbanspork.common.codec.vmess;

import com.urbanspork.common.codec.ChunkSizeCodec;
import com.urbanspork.common.codec.PaddingLengthGenerator;
import com.urbanspork.common.codec.aead.Authenticator;
import com.urbanspork.common.codec.aead.PayloadDecoder;

public class AEADBodyDecoder implements PayloadDecoder {

    private final Authenticator auth;
    private final ChunkSizeCodec sizeCodec;
    private final PaddingLengthGenerator padding;
    private int payloadLength = INIT_LENGTH;
    private int paddingLength = INIT_LENGTH;

    public AEADBodyDecoder(Authenticator auth, ChunkSizeCodec sizeCodec, PaddingLengthGenerator padding) {
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
    public int paddingLength() {
        return paddingLength;
    }

    @Override
    public void updatePaddingLength(int paddingLength) {
        this.paddingLength = paddingLength;
    }

    @Override
    public Authenticator auth() {
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
