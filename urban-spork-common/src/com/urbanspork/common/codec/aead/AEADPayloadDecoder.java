package com.urbanspork.common.codec.aead;

import com.urbanspork.common.codec.ChunkSizeCodec;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;

public class AEADPayloadDecoder extends ReplayingDecoder<AEADPayloadDecoder.State> {

    private final AEADAuthenticator authenticator;
    private final int tagSize;
    private final ChunkSizeCodec chunkSizeDecoder;
    private final int sizeBytes;

    private int length;

    public AEADPayloadDecoder(AEADAuthenticator authenticator, ChunkSizeCodec chunkSizeDecoder) {
        super(State.READ_LENGTH);
        this.authenticator = authenticator;
        this.tagSize = authenticator.codec().TAG_SIZE;
        this.chunkSizeDecoder = chunkSizeDecoder;
        this.sizeBytes = chunkSizeDecoder.sizeBytes();
    }

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        switch (state()) {
            case READ_LENGTH -> {
                if (in.readableBytes() >= sizeBytes + tagSize) {
                    byte[] payloadSizeBytes = new byte[sizeBytes + tagSize];
                    in.readBytes(payloadSizeBytes);
                    checkpoint(State.READ_PAYLOAD);
                    length = chunkSizeDecoder.decode(payloadSizeBytes);
                }
            }
            case READ_PAYLOAD -> {
                if (in.readableBytes() >= length + tagSize) {
                    byte[] payloadBytes = new byte[length + tagSize];
                    in.readBytes(payloadBytes);
                    checkpoint(State.READ_LENGTH);
                    out.add(in.alloc().buffer().writeBytes(authenticator.open(payloadBytes)));
                }
            }
        }
    }

    public AEADAuthenticator authenticator() {
        return authenticator;
    }

    enum State {
        READ_LENGTH, READ_PAYLOAD
    }
}
